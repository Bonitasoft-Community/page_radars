package org.bonitasoft.radar.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.radar.Radar;
import org.bonitasoft.radar.RadarPhoto;
import org.bonitasoft.radar.RadarPhoto.IndicatorPhoto;

public class RadarProcess extends Radar {

    final static Logger logger = Logger.getLogger(RadarProcess.class.getName());
    private final static String LOGGER_LABEL = "RadarProcess ##";

    public final static String CLASS_RADAR_NAME = "RadarProcess";

    private final static BEvent eventErrorExecution = new BEvent(RadarProcess.class.getName(), 1,
            Level.ERROR,
            "Error during access information", "The calculation failed", "Result is not available",
            "Check exception");

    // please use the RadarFactory method    
    public RadarProcess(String name, long tenantId, APIAccessor apiAccessor) {
        super(name, CLASS_RADAR_NAME, tenantId, apiAccessor);
    }

    @Override
    public String getLabel() {
        return "Number of processes (actif, disable)";
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
        return RadarResult.getInstance(true, true);
    }

    @Override
    public RadarResult deactivate() {
        // nothing to do to deactived
        return RadarResult.getInstance(true, false);
    }

    public RadarResult isActivated() {
        return RadarResult.getInstance(true, true);
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
    public static class ParameterProcess implements RadarPhotoParameter {

        public Long dateDeploymentAfter = null;
    }

    @Override
    public RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter) {

        RadarPhotoResult radarPhotoResult = new RadarPhotoResult();
        RadarPhoto photoWorkers = new RadarPhoto(this, "Items statistic", "Statistics on Bonita object (Process)");
        photoWorkers.startShooting();
        radarPhotoResult.listPhotos.add(photoWorkers);

        try {
            ProcessAPI processAPI = apiAccessor.getProcessAPI();

            

            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 1);
            sob.filter(ProcessDeploymentInfoSearchDescriptor.ACTIVATION_STATE, ActivationState.ENABLED.toString());
            SearchResult<ProcessDeploymentInfo> result = processAPI.searchProcessDeploymentInfos(sob.done());

            IndicatorPhoto indicatorProcessEnabled = new IndicatorPhoto("ProcessEnabled");
            indicatorProcessEnabled.label = "Number of Processes Enabled";
            indicatorProcessEnabled.setValue(result.getCount());
            photoWorkers.addIndicator(indicatorProcessEnabled);

            sob = new SearchOptionsBuilder(0, 1);
            sob.filter(ProcessDeploymentInfoSearchDescriptor.ACTIVATION_STATE, ActivationState.DISABLED.toString());
            result = processAPI.searchProcessDeploymentInfos(sob.done());

            IndicatorPhoto indicatorProcessDisabled = new IndicatorPhoto("ProcessDisabled");
            indicatorProcessDisabled.label = "Number of Processes Disabled";
            indicatorProcessDisabled.setValue(result.getCount());
            photoWorkers.addIndicator(indicatorProcessDisabled);

            if (radarPhotoParameter != null && ((ParameterProcess) radarPhotoParameter).dateDeploymentAfter != null) {
                sob = new SearchOptionsBuilder(0, 1);
                sob.greaterOrEquals(ProcessDeploymentInfoSearchDescriptor.DEPLOYMENT_DATE, ((ParameterProcess) radarPhotoParameter).dateDeploymentAfter);
                result = processAPI.searchProcessDeploymentInfos(sob.done());

                IndicatorPhoto lastProcessDeployed = new IndicatorPhoto("LastProcessDeployed");
                lastProcessDeployed.label = "Number of Last process deployed";
                lastProcessDeployed.setValue(result.getCount());
                photoWorkers.addIndicator(lastProcessDeployed);
            }

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGER_LABEL+"During getAllProcess : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
            radarPhotoResult.listEvents.add(new BEvent(eventErrorExecution, e, ""));
        }
        photoWorkers.stopShooting();
        return radarPhotoResult;
    }

    @Override
    public boolean hasHtmlDasboard() {
        return true;
    }
}
