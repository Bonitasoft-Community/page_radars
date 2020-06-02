package org.bonitasoft.radar.workers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaEngineConnection;
import org.bonitasoft.radar.Radar;


/* -------------------------------------------------------------------- */
/*                                                                      */
/*
 * Workers monitor Workers component
 * /*
 */
/* -------------------------------------------------------------------- */

public class RadarWorkers extends Radar {


    public final static String CLASS_RADAR_NAME = "Workers";

    private final static BEvent eventErrorExecutionQuery = new BEvent(RadarWorkers.class.getName(), 1,
            Level.ERROR,
            "Error during the SqlQuery", "The SQL Query to detect a stuck flow node failed", "No stick flow nodes can be detected",
            "Check exception");

    
    private final static String LOGGER_LABEL = "DeepMonitoring ##";
    private final static Logger lOGGER = Logger.getLogger(RadarWorkers.class.getName());

 

    
    // please use the RadarFactory method    
    public RadarWorkers(String name, long tenantId, APIAccessor apiAccessor) {
        super( name, CLASS_RADAR_NAME, tenantId, apiAccessor);
    }
    
    @Override
    public String getLabel() {
        return "Display worker and Connector workers activity";
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
    /* Operation*/
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    
    @Override
    public RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter) {
        RadarPhotoResult radarPhotoResult = new RadarPhotoResult();

        WorkerRadarPhoto photoWorkers = new WorkerRadarPhoto(this, "Workers Thread", "Workers thread", true);
        WorkerRadarPhoto photoConnector = new WorkerRadarPhoto(this, "Connectors Thread", "Connectors thread", false);
        radarPhotoResult.listPhotos.add(photoWorkers);
        radarPhotoResult.listPhotos.add(photoConnector);

        // --------------------------- Thread
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread th : threadSet) {
            // Bonita-Worker-1-10
            if (th.getName().startsWith("Bonita-Worker")) {
                try {
                    int posId = th.getName().lastIndexOf('-');
                    int workerId = Integer.parseInt(th.getName().substring(posId + 1));

                    // get or create it
                    photoWorkers.createWorkerPhoto(workerId, th);

                } catch (Exception e) {
                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    lOGGER.severe(LOGGER_LABEL + "During takePhoto : " + e.toString() + " at " + sw.toString());

                }
            }
            if (th.getName().startsWith("ConnectorExecutor")) {

                // how do we know which worker is link to this connector?
                int posId = th.getName().lastIndexOf('-');
                int workerId = Integer.parseInt(th.getName().substring(posId + 1));
                // get or create it
                photoConnector.createWorkerPhoto(workerId, th);
            }
        }

        // now synthesis database
        int delayInMs=1000*60*5; // 5 mn
        photoWorkers.workerQueueNumber = getNumberOfFlowNodesWaitingForExecution(tenantId,delayInMs);
        if (photoWorkers.workerQueueNumber == -1)
            photoWorkers.workerQueueNumber = 0;

        // In this node
        // JMX 
        // bonitaBpmengineWorkPending.tenant.1
        // bonitaBpmengineWorkRunning.tenant.1
        // bonitaBpmengineConnectorPending.tenant.1
        // bonitaBpmengineConnectorRunning.tenant.1
        
        // compute the main indicator
        photoWorkers.compute(true);
        photoConnector.compute(false);

        return radarPhotoResult;
    }

    @Override
    public boolean hasHtmlDasboard() {
        return true;
    }
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Public method*/
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * Return the number of flow nodes waiting to be executed
     * 
     * @return
     */
    public int getNumberOfFlowNodesWaitingForExecution(long tenantId, long delayInMs ) {
        StuckFlowNodes stuckFlowNode = getListFlowNodesWaitingForExecution("count( fln.id )", tenantId, 1, delayInMs, null);
        if (!stuckFlowNode.listStuckFlowNode.isEmpty() && (!stuckFlowNode.listStuckFlowNode.get(0).isEmpty())) {
            String keyId = stuckFlowNode.listStuckFlowNode.get(0).keySet().iterator().next();
            return Integer.parseInt( stuckFlowNode.listStuckFlowNode.get(0).get(keyId).toString());
        }
        return -1;
    }

    /**
     * return a page of waitingFlowNode for execution, order by the lastest first.
     * 
     * @param tenantId
     * @param count
     * @return
     */
    public class StuckFlowNodes{
        public List<Map<String, Object>> listStuckFlowNode = new ArrayList<>();
        public String sqlQuery;
        public List<BEvent> listEvents = new ArrayList<>();
    }
    public StuckFlowNodes getOldestFlowNodesWaitingForExecution(long tenantId, long count, long delayInMs) {
        return getListFlowNodesWaitingForExecution("fln.ID, fln.STATEID, fln.STATENAME, fln.LASTUPDATEDATE, fln.ROOTCONTAINERID", tenantId, count, delayInMs, "lastupdatedate asc");        
    }

    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Private*/
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * internal method to get information on the waiting flownode instance
     * 
     * @param selectResult
     * @param tenantId
     * @param count
     * @param orderBy
     * @return
     */
    private StuckFlowNodes  getListFlowNodesWaitingForExecution(String selectResult, long tenantId, long count, long delayInMs, String orderBy) {
        StuckFlowNodes stuckFlowNodes = new StuckFlowNodes();
        long lastUpdateDate=System.currentTimeMillis() - delayInMs;
        stuckFlowNodes.sqlQuery="SELECT " + selectResult + " FROM FLOWNODE_INSTANCE fln "
                + " WHERE (fln.STATE_EXECUTING = true "
                + " OR fln.STABLE = false " // false
                + " OR fln.TERMINAL = true " // true
                + " OR fln.STATECATEGORY = 'ABORTING' OR fln.STATECATEGORY='CANCELLING') " // only waiting state
                + " AND fln.TENANTID =  "+tenantId // filter by the tenantId
                + " AND fln.LASTUPDATEDATE < "+lastUpdateDate
                + " AND fln.kind <> 'call'"
                + " AND fln.GATEWAYTYPE <> 'PARALLEL' and fln.GATEWAYTYPE <> 'INCLUSIVE' "
                + " AND fln.LASTUPDATEDATE <  "+lastUpdateDate
                + (orderBy != null ? " ORDER BY fln." + orderBy : "");
        
        
        String sqlQuery = "SELECT " + selectResult + " FROM FLOWNODE_INSTANCE fln "
                + " WHERE (fln.STATE_EXECUTING = ? " // true
                + " OR fln.STABLE = ? " // false
                + " OR fln.TERMINAL = ? " // true
                + " OR fln.STATECATEGORY = 'ABORTING' OR fln.STATECATEGORY='CANCELLING') " // only waiting state
                + " AND fln.TENANTID = ? " // filter by the tenantId
                + " AND fln.LASTUPDATEDATE < ? "
                + " AND fln.kind <> 'call'"
                + " AND fln.GATEWAYTYPE <> 'PARALLEL' and fln.GATEWAYTYPE <> 'INCLUSIVE' "
                + (orderBy != null ? "ORDER BY fln." + orderBy : "");

        
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try (Connection con = BonitaEngineConnection.getConnection(); )
        {
            pstmt = con.prepareStatement(sqlQuery);
            pstmt.setBoolean(1, true);
            pstmt.setBoolean(2, false);
            pstmt.setBoolean(3, true);
            pstmt.setLong(4, tenantId);
            pstmt.setLong(5, lastUpdateDate); // 5 minutes delay
            
            rs = pstmt.executeQuery();
            ResultSetMetaData rmd = pstmt.getMetaData();
            int line = 0;
            while (rs.next() && line < count) {
                line++;
                Map<String, Object> record = new HashMap<>();
                stuckFlowNodes.listStuckFlowNode.add(record);
                for (int column = 1; column <= rmd.getColumnCount(); column++)
                    record.put(rmd.getColumnName(column).toUpperCase(), rs.getObject(column));
            }
            return stuckFlowNodes;
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            lOGGER.severe(LOGGER_LABEL + "During getCountOfFlowNode : " + e.toString() + " SqlQuery["+sqlQuery+"] at " + sw.toString());
            stuckFlowNodes.listEvents.add( new BEvent(eventErrorExecutionQuery, e, " SqlQuery["+sqlQuery+"]" ));
            return stuckFlowNodes;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (final SQLException localSQLException) {
                    lOGGER.severe(LOGGER_LABEL + "During close : " + localSQLException.toString());
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (final SQLException localSQLException) {
                    lOGGER.severe(LOGGER_LABEL + "During close : " + localSQLException.toString());
                }
            }
           
        }

    }

}
