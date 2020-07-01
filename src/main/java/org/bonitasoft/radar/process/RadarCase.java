package org.bonitasoft.radar.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.custompage.workers.EngineMonitoringAPI;
import org.bonitasoft.properties.BonitaEngineConnection;
import org.bonitasoft.radar.Radar;
import org.bonitasoft.radar.RadarPhoto;
import org.bonitasoft.radar.Radar.RadarPhotoParameter;
import org.bonitasoft.radar.Radar.RadarPhotoResult;
import org.bonitasoft.radar.Radar.RadarResult;
import org.bonitasoft.radar.Radar.TypeRadar;
import org.bonitasoft.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class RadarCase extends Radar {

    final static Logger logger = Logger.getLogger(RadarCase.class.getName());
    private final static String LOGGER_LABEL = "RadarCase ##";

    public final static String CLASS_RADAR_NAME = "RadarCases";

    private final static BEvent eventErrorExecution = new BEvent(RadarCase.class.getName(), 1,
            Level.ERROR,
            "Error during access information", "The calculation failed", "Result is not available",
            "Check exception");

    // please use the RadarFactory method    
    public RadarCase(String name, long tenantId, APIAccessor apiAccessor) {
        super(name, CLASS_RADAR_NAME, tenantId, apiAccessor);
    }

    @Override
    public String getLabel() {
        return "Number of active case";
    }

    @Override
    public TypeRadar getType() {
        return TypeRadar.LIGHT;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Radar may want to register / start internal mechanism on start / stop */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    @Override
    public RadarResult activate() {
        // nothing to do to actived
        return RadarResult.getInstance( true, true);
    }

    @Override
    public RadarResult deactivate() {
        // nothing to do to deactived
        return RadarResult.getInstance( true, false);
    }
    public RadarResult isActivated() {
        return RadarResult.getInstance( true, true);
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Additionnal configuration */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    @Override
    public RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter) {

        RadarPhotoResult radarPhotoResult = new RadarPhotoResult();
        RadarPhoto photoWorkers = new RadarPhoto(this, "Items statistic", "Statistics on Bonita object (Cases)");
        photoWorkers.startShooting();
        radarPhotoResult.listPhotos.add(photoWorkers);

        try {
            ProcessAPI processAPI = apiAccessor.getProcessAPI();
            SearchOptionsBuilder sob;
            SearchResult result;
            
            long processInstance = processAPI.getNumberOfProcessInstances();
            addIndicator(photoWorkers, "NbActivesCases", "Nb Active cases", processInstance);
            
            
            long archivedProcessInstance = processAPI.getNumberOfArchivedProcessInstances();
            addIndicator(photoWorkers, "NbAchivedCases", "Nb Archived cases", archivedProcessInstance);
            
            
            result = processAPI.searchFailedProcessInstances( new SearchOptionsBuilder(0,1).done());
            addIndicator(photoWorkers, "NbFailedCases", "Nb Failed cases", result.getCount());

            result = processAPI.searchHumanTaskInstances( new SearchOptionsBuilder(0,1).done() );
            addIndicator( photoWorkers, "HumanTask", "Human task in progress", result.getCount());

            sob = new SearchOptionsBuilder(0, 1);
            sob.greaterOrEquals(HumanTaskInstanceSearchDescriptor.DUE_DATE, new Date().getTime());
            result = processAPI.searchHumanTaskInstances(sob.done());
            addIndicator(photoWorkers, "TaskDueDate", "Task with an overduedate", result.getCount());
            

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGER_LABEL+"During getAllProcessInstance : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
            radarPhotoResult.listEvents.add(new BEvent(eventErrorExecution, e, ""));
            
        }
        photoWorkers.stopShooting();
        return radarPhotoResult;
    }

    @Override
    public boolean hasHtmlDasboard() {
        return true;
    }
    
    public void addIndicator( RadarPhoto photo, String name, String label, long value  ) {
        IndicatorPhoto indicator = new IndicatorPhoto( name );
        indicator.label = label;
        indicator.setValue( value );
        photo.addIndicator( indicator );
    
    }
    
}
