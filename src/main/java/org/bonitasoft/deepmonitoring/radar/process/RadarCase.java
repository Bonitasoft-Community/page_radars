package org.bonitasoft.deepmonitoring.radar.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.custompage.workers.EngineMonitoringAPI;
import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarPhotoParameter;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarPhotoResult;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarResult;
import org.bonitasoft.deepmonitoring.radar.Radar.TypeRadar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.deepmonitoring.tool.BonitaEngineConnection;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;

public class RadarCase extends Radar {

    final static Logger logger = Logger.getLogger(RadarCase.class.getName());
    private final static String LOGGER_LABEL = "DeepMonitoring ##";

    public static String CLASS_RADAR_NAME = "RadarCases";

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
        RadarPhoto photoWorkers = new RadarPhoto(this, "Workers Thread", "Workers thread");
        radarPhotoResult.listPhotos.add(photoWorkers);

        try {
            ProcessAPI processAPI = apiAccessor.getProcessAPI();

            long processInstance = processAPI.getNumberOfProcessInstances();

            IndicatorPhoto indicatorCases = new IndicatorPhoto( "NbCases");
            indicatorCases.label = "Nb cases";
            indicatorCases.setValue( processInstance );
            photoWorkers.addIndicator(indicatorCases);
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
        }
        return radarPhotoResult;
    }

}
