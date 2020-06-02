package org.bonitasoft.radar.connector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.engine.tracking.FlushEvent;
import org.bonitasoft.engine.tracking.FlushEventListener;
import org.bonitasoft.engine.tracking.FlushEventListenerResult;
import org.bonitasoft.engine.tracking.Record;
import org.bonitasoft.engine.tracking.TimeTrackerRecords;

public class RadarConnectorListener implements FlushEventListener {

    static Logger logger = Logger.getLogger(RadarConnectorListener.class.getName());

    
    public static RadarConnectorListener getInstance() {
        return new RadarConnectorListener();

    }

    List<Record> keepRecordOverloaded = new ArrayList<>();

    List<Record> keepAllRecords = new ArrayList<>();

    private boolean isActive = true;

    private boolean isTracking = true;
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Parameters */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    private long thresholdDurationInMs = 1000;
    private long frameMonitorInMs = 10 * 60 * 1000;

    public void setThresholdDuration(long thresholdDurationInMs) {
        this.thresholdDurationInMs = thresholdDurationInMs;
    }

    public void setFrameMonitorInMs(long frameMonitorInMs) {
        this.frameMonitorInMs = frameMonitorInMs;
    }

    public long getThresholdDuration() {
        return thresholdDurationInMs;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * Attention, flushEvent send again and again the same record... So, we don't want to save them twice
     */
    public FlushEventListenerResult flush(final FlushEvent flushEvent) throws Exception {
        if (flushEvent.getRecords().isEmpty()) {
            return new FlushEventListenerResult(flushEvent);
        }
        long currentTime = System.currentTimeMillis();
        long limitTime = currentTime - frameMonitorInMs;

        // now, remove all record too old
        int i = 0;
        while (i < keepRecordOverloaded.size()) {
            if (keepRecordOverloaded.get(i).getTimestamp() < limitTime)
                keepRecordOverloaded.remove(i);
            else
                i++;
        }
        i = 0;
        while (i < keepAllRecords.size()) {
            if (keepAllRecords.get(i).getTimestamp() < limitTime)
                keepAllRecords.remove(i);
            else
                i++;
        }
        // final long flushTime = flushEvent.getFlushTime();
        
        for (Record record : flushEvent.getRecords()) {
            logger.fine("RadarConnectionListener : Name="+record.getName()+" Already? "+keepAllRecords.contains(record)+" "+record.getDescription());
            if (! record.getName().equals(TimeTrackerRecords.EXECUTE_CONNECTOR_WORK) )
                continue;
            
            if (record.getDuration() > thresholdDurationInMs && record.getTimestamp() > limitTime) {
                if (keepRecordOverloaded.contains(record))
                    logger.fine("Record ["+record.getTimestamp()+"] already recorded");
                else
                    keepRecordOverloaded.add(record);
            }
            if (keepAllRecords.contains(record))
                logger.fine("Record ["+record.getTimestamp()+"] already recorded");
            else
                keepAllRecords.add(record);
        }

        return new FlushEventListenerResult(flushEvent);

    }

    // EXECUTE_CONNECTOR_INCLUDING_POOL_SUBMIT
    // EXECUTE_CONNECTOR_WORK : processDefinitionId: 7717119015881338597 - connectorDefinitionName: LongConnector 30 s - connectorInstanceId: 20001
    // EVALUATE_EXPRESSION_INCLUDING_CONTEXT : Expression: SExpressionImpl [name=Sleep 45, content=Thread.sleep(1000*45), returnType=java.lang.String, dependencies=[], expressionKind=ExpressionKind [interpreter=NONE, type=TYPE_CONSTANT]] - evaluationContext: context [containerId=40003, containerType=ACTIVITY_INSTANCE, processDefinitionId=7717119015881338597]
    // EXECUTE_CONNECTOR_INPUT_EXPRESSIONS: 0ms / Connector ID: scripting-groovy-script - input parameters: {variables=[], engineExecutionContext=org.bonitasoft.engine.connector.EngineExecutionContext@4162, script=Thread.sleep(1000*45), connectorApiAccessor=com.bonitasoft.engine.expression.ConnectorAPIAccessorExt@57f67509}
    // EXECUTE_CONNECTOR_CALLABLE : 45123 ms / Connector: org.bonitasoft.engine.core.connector.impl.SConnectorAdapter@734417e1 - inputParameters: {variables=[], script=Thread.sleep(1000*45)}
    // EXECUTE_CONNECTOR_INCLUDING_POOL_SUBMIT 45124 ms / Connector: org.bonitasoft.engine.core.connector.impl.SConnectorAdapter@734417e1 - inputParameters: {variables=[], script=Thread.sleep(1000*45)}
    
    // EXECUTE_CONNECTOR_WORK/ 45126 ms processDefinitionId: 7717119015881338597 - connectorDefinitionName: Connector 45 s - connectorInstanceId: 20002
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Interface */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public String getStatus() {
        return "keepRecordOverloaded.size:" +keepRecordOverloaded.size()+" AllRecords.size():"+keepAllRecords.size();
    }

    public final static String CSTRADARLISTENER_NAME = "RadarConnectorListener"; 
    public String getName() {
        return CSTRADARLISTENER_NAME;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;

    }

    public void notifyStopTracking() {
        isTracking = false;
    }

    public void notifyStartTracking() {
        isTracking = true;

    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Indicator */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public List<Record> getRecordsOverloaded() {
        return keepRecordOverloaded;
    }

    public int getNbConnectorCall() {
        return keepAllRecords.size();
    }
}
