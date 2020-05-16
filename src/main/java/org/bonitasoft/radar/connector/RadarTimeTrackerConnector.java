package org.bonitasoft.deepmonitoring.radar.connector;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.impl.SpringTenantServiceAccessor;
import org.bonitasoft.engine.tracking.FlushEventListener;
import org.bonitasoft.engine.tracking.Record;
import org.bonitasoft.engine.tracking.TimeTracker;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;


public class RadarTimeTrackerConnector extends Radar {

    final static Logger logger = Logger.getLogger(RadarTimeTrackerConnector.class.getName());
    public final static String LOGGER_LABEL = "RadarTimeTrackerConnector ##";

    public final static String CLASS_RADAR_NAME = "RadarTimeTrackerConnector";

    public final static long CSTFRAMEMONITOR_MIN = 1000*60*10; // 10 mn
    public final static long CSTFRAMEMONITOR_MAX = 1000*60*60*24; // 1 day
    
    private static BEvent eventCantUpdateListener = new BEvent(RadarTimeTrackerConnector.class.getName(), 1, Level.ERROR,
            "Can't update listener",
            "The Listener can't be updated",
            "No collect can be done", "Check the Bonita version");
    
    private static BEvent eventTrackingIsNotActivated = new BEvent(RadarTimeTrackerConnector.class.getName(), 2, Level.INFO,
            "Tracking is not activated",
            "The tracking is not activated, no photo can be delivered",
            "No collect can be done", "Activate the tracking");
    
    private static BEvent eventCollectConnectorInformation = new BEvent(RadarTimeTrackerConnector.class.getName(), 3, Level.ERROR,
            "Collect connector information",
            "A request failed, the information to collect connector failed",
            "No information on connect", "Check Error");
    
    
    public RadarTimeTrackerConnector(String name, long tenantId, APIAccessor apiAccessor) {
        super(name, CLASS_RADAR_NAME, tenantId, apiAccessor);

    }

    @Override
    public String getLabel() {
        return "Time Tracker connector";
    }

    @Override
    public TypeRadar getType() {
        return TypeRadar.HEAVY;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Radar may want to register / start internal mechanism on start / stop */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    @Override
    public RadarResult activate() {
        RadarResult radarResult = new RadarResult();
        radarResult.operationSuccess=true;
        radarResult.isActivated=true;

        // check if the timetracker is set and on
        if (getCurrentListener() == null) {
            RadarConnectorListener listener = RadarConnectorListener.getInstance();
            listener.setThresholdDuration(thresholdDuration);
            if (frameMonitorInMs < CSTFRAMEMONITOR_MIN)
                frameMonitorInMs=CSTFRAMEMONITOR_MIN;
            if (frameMonitorInMs > CSTFRAMEMONITOR_MAX)
                frameMonitorInMs=CSTFRAMEMONITOR_MAX;

            listener.setFrameMonitorInMs(frameMonitorInMs);
            // if the frameMonitorInMs is less than the flush interval, we miss a lot of record by definition.
            if (timeTraker.getFlushIntervalInMS() > frameMonitorInMs) {
                timeTraker.setFlushIntervalInMS( (long) ( frameMonitorInMs * 0.9) );
            }
            radarResult.listEvents.addAll( updateListener(timeTraker, listener, true));
            if (BEventFactory.isError(radarResult.listEvents)) {
                radarResult.operationSuccess = false;
                radarResult.isActivated = false;
            }
        }
        if (radarResult.operationSuccess) {
            timeTraker.start();
            timeTraker.startTracking();
        }
        return radarResult;

    }

    @Override
    public RadarResult deactivate() {
        RadarResult radarResult = new RadarResult();
        radarResult.operationSuccess=true;
        radarResult.isActivated=false;
        // check if the timetracker is set and on
        RadarConnectorListener radarTimeTrackerConnector = getCurrentListener();
        if (radarTimeTrackerConnector != null) {
            radarResult.listEvents.addAll( updateListener(timeTraker, radarTimeTrackerConnector, false));
            if (BEventFactory.isError(radarResult.listEvents)) {
                radarResult.operationSuccess=false;
                radarResult.isActivated=true;
            }
        }
        return radarResult;
    }
    public RadarResult isActivated() {
        RadarResult radarResult = new RadarResult();
        RadarConnectorListener radarTimeTrackerConnector = getCurrentListener();
        if (radarTimeTrackerConnector != null) {
            radarResult.isActivated = true;
        } else {
            radarResult.isActivated =  false;
        }
        return radarResult;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Additionnal configuration */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    
    private TimeTracker timeTraker=null; 
    private long thresholdDuration;
    private long frameMonitorInMs;

    public void setSpringAccessor(TenantServiceAccessor tenantServiceAccessor) {
        if (!(tenantServiceAccessor instanceof SpringTenantServiceAccessor))
            return;
        timeTraker = ((SpringTenantServiceAccessor) tenantServiceAccessor).getTimeTracker();
    
    }

    public void setThresholdDuration(long thresholdDuration) {
        this.thresholdDuration = thresholdDuration;
        RadarConnectorListener listener = getCurrentListener();
        if (listener != null)
            listener.setThresholdDuration(thresholdDuration);

    }

    public void setFrameMonitorInMs(long frameMonitorInMs) {
        this.frameMonitorInMs = frameMonitorInMs;
        RadarConnectorListener listener = getCurrentListener();
        if (listener != null)
            listener.setFrameMonitorInMs(frameMonitorInMs);
    }

    
    public String getTrackerStatus() {
        if (timeTraker==null)
            return "No Time Tracker found";
        StringBuilder result = new StringBuilder();
        result.append("TimeTracker flushIntervalle: "+timeTraker.getFlushIntervalInMS()+" ms,");
        result.append("Tracking: "+timeTraker.isTracking()+",");
        result.append("Status: ["+timeTraker.getStatus()+"],");
        result.append("ActivatedRecord: "+timeTraker.getActivatedRecords()==null ? "null":timeTraker.getActivatedRecords().size()+",");
        RadarConnectorListener listener = getCurrentListener();
        if (listener == null)
            result.append("No RadarListener detected");
        else {
            result.append("RadarListener : ["+listener.getStatus()+"]");
        }
        return result.toString();                
    }
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public static final String CSTPHOTO_CONNECTORSOVERLOADED = "NumberOfConnectorsOverloaded";
    public static final String CSTPHOTO_CONNECTORSCALL = "NumberOfConnectorsCall";
    
    public static class TimeTrackerParameter implements RadarPhotoParameter {
        public long maxConnectorsInDetail=20;
    }
    
    
    @Override
    public RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter) {
        TimeTrackerParameter timeTrackerParameter = (TimeTrackerParameter) radarPhotoParameter;
        if (timeTrackerParameter == null)
            timeTrackerParameter = new TimeTrackerParameter();
        RadarPhotoResult radarPhotoResult =new RadarPhotoResult();

        // 
        RadarConnectorListener radarTimeTrackerConnector = getCurrentListener();
        if (radarTimeTrackerConnector == null) {
            radarPhotoResult.listEvents.add( eventTrackingIsNotActivated);
            return radarPhotoResult;
        }

        RadarPhoto photoConnector = new RadarPhoto(this, "Connector Execution", "Connector Execution");
        radarPhotoResult.listPhotos.add(photoConnector);

        try {
            List<Record> listRecords = radarTimeTrackerConnector.getRecordsOverloaded();

            IndicatorPhoto indicatorConnector = new IndicatorPhoto(CSTPHOTO_CONNECTORSOVERLOADED);
            indicatorConnector.label = "Number of connector execution using more time than the threshold in the period";
            
            // prepare the data
            List<ConnectorDecorator> listConnectorDecorator = new ArrayList<>();
            for (Record record : listRecords) {
                ConnectorDecorator connectorDecorator = new ConnectorDecorator( record );
                if (connectorDecorator.getConnectorInstanceId() !=null)
                    listConnectorDecorator.add(connectorDecorator);
            }
            indicatorConnector.setValue( listConnectorDecorator.size() );
            // detail : manage per page of 100 to build the request
            try
            {
                ConnectorDecorator.completeConnectorInformation( listConnectorDecorator);
                ConnectorDecorator.sortByDuration( listConnectorDecorator, false);
            }
            catch(Exception e) {
                radarPhotoResult.listEvents.add( new BEvent(eventCollectConnectorInformation, e, "" ));
            }
            for (int i=0; i< Math.min(timeTrackerParameter.maxConnectorsInDetail, listConnectorDecorator.size()); i++) {
                ConnectorDecorator connectorDecorator = listConnectorDecorator.get( i );
                indicatorConnector.detailsList.add( connectorDecorator.getInformation() );
                
            }
         
            photoConnector.addIndicator(indicatorConnector);
            
            IndicatorPhoto indicatorNbConnectors = new IndicatorPhoto(CSTPHOTO_CONNECTORSCALL);
            indicatorNbConnectors.label = "Number Of Connectors Call in the period";
            indicatorNbConnectors.setValue( radarTimeTrackerConnector.getNbConnectorCall() );
            photoConnector.addIndicator(indicatorNbConnectors);
            
            
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
        }
        return radarPhotoResult;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Private method */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    private RadarConnectorListener getCurrentListener() {
        // check if the timetracker is set and on
        for (FlushEventListener connectorListener : getActiveFlushEventListener(timeTraker)) {
            // if the command is redeployed, then the class is considered as different
            // BUT in that circunstance, we have to redeploy the class, else we will face a classCastException
            if (connectorListener instanceof RadarConnectorListener)
                return (RadarConnectorListener) connectorListener;
        }
        return null;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Methods are not public, so set it                                    */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * the getActiveFlushEvent listener is not public ( !!!! )
     */
    @SuppressWarnings("unchecked")
    private Collection<FlushEventListener> getActiveFlushEventListener(TimeTracker timeTracker) {
        Method[] methods = TimeTracker.class.getMethods();
        boolean foundMethod=false;
        for (Method method : methods) {
            if (method.getName().equals("getActiveFlushEventListeners")) {
                try {
                    foundMethod=true;
                    Object result = method.invoke(timeTracker);
                    if (result != null && result instanceof List)
                        return (List<FlushEventListener>) result;
                } catch (IllegalAccessException e) {
                    logger.severe("Can't call TimeTracker.getActiveFlushEventListener " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    logger.severe("Can't call TimeTracker.getActiveFlushEventListener " + e.getMessage());
                } catch (InvocationTargetException e) {
                    logger.severe("Can't call TimeTracker.getActiveFlushEventListener " + e.getMessage());
                }
            }
        }
        if (!foundMethod) {
            try {
                Field f1 = timeTracker.getClass().getDeclaredField("flushEventListeners");
                f1.setAccessible(true);
                Map<String, FlushEventListener> listListeners = (Map<String, FlushEventListener>) f1.get(timeTracker);
                return listListeners.values();
            } catch (Exception e)
            {
                logger.severe("Can't call TimeTracker.getActiveFlushEventListener " + e.getMessage());
            }
        }
        return new ArrayList<>();

    }
    
    // the addListener is public only after 7.9
    private List<BEvent> updateListener(TimeTracker timeTracker, FlushEventListener listener, boolean addIt) {
        
        // timeTraker.addFlushEventListener(listener);
        List<BEvent> listEvents = new ArrayList<>();
        Field f1;
        try {
            f1 = TimeTracker.class.getDeclaredField("flushEventListeners");
            f1.setAccessible(true);
            Map<String, FlushEventListener> listListeners = (Map<String, FlushEventListener>) f1.get(timeTracker);
            if (listListeners == null)
            {
                listListeners = new HashMap<>();
            }
            if (addIt)
                listListeners.put( listener.getName(), listener );
            else
                listListeners.remove( listener.getName() );
            f1.set(timeTracker, listListeners);
        } catch (Exception  e) {
            listEvents.add( new BEvent( eventCantUpdateListener, e, ""));            
        }
        return listEvents;
    }

}
