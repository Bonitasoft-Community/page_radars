package org.bonitasoft.radar.archive;

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
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaEngineConnection;
import org.bonitasoft.radar.Radar;
import org.bonitasoft.radar.RadarPhoto;
import org.bonitasoft.radar.Radar.RadarPhotoParameter;
import org.bonitasoft.radar.Radar.RadarPhotoResult;
import org.bonitasoft.radar.Radar.RadarResult;
import org.bonitasoft.radar.Radar.TypeRadar;
import org.bonitasoft.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.radar.process.RadarCase;
import org.bonitasoft.radar.workers.RadarWorkers;
import org.bonitasoft.radar.workers.RadarWorkers.StuckFlowNodes;

public class RadarCleanArchivedDross extends Radar {

    final static Logger logger = Logger.getLogger(RadarCleanArchivedDross.class.getName());
    private final static String LOGGER_LABEL = "RadarCleanArchivedDross ##";

    public static String CLASS_RADAR_NAME = "RadarCleanArchive";

    private final static BEvent eventErrorExecutionQuery = new BEvent(RadarCleanArchivedDross.class.getName(), 1,
            Level.ERROR,
            "Error during the SqlQuery", "The SQL Query to detect a stuck flow node failed", "No stick flow nodes can be detected",
            "Check exception");

    // please use the RadarFactory method    
    public RadarCleanArchivedDross(String name, long tenantId, APIAccessor apiAccessor) {
        super(name, CLASS_RADAR_NAME, tenantId, apiAccessor);
    }

    @Override
    public String getLabel() {
        return "Clean archive records in the database, which are not related to a parent";
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

    @Override
    public RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter) {

        RadarPhotoResult radarPhotoResult = new RadarPhotoResult();
        RadarPhoto photoWorkers = new RadarPhoto(this, "Clean Archived record", "Get number of record to be purged");
        photoWorkers.startShooting();
        radarPhotoResult.listPhotos.add(photoWorkers);

        try {
            DrossExecution drossesExecution = getStatusAll(tenantId);
            for (TypeDross drossExecution : drossesExecution.listDross) {
                IndicatorPhoto indicatorCases = new IndicatorPhoto(drossExecution.name);
                indicatorCases.label = drossExecution.label;
                indicatorCases.setValue(drossExecution.nbRecordsDetected);

                photoWorkers.addIndicator(indicatorCases);
            }

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
        }
        photoWorkers.stopShooting();
        return radarPhotoResult;
    }

    @Override
    public boolean hasHtmlDasboard() {
        return true;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static class TypeDross {

        public long nbRecordsDetected;
        public String name;
        public String label;
        public String explanation;
        public String sqlQuery;
        public String limiterToOneProcessInstance;

        public TypeDross(String name, String label, String explanation, String sqlQuery, String limiterToOneProcessInstance) {
            this.name = name;
            this.label = label;
            this.explanation = explanation;
            this.sqlQuery = sqlQuery;
            this.limiterToOneProcessInstance = limiterToOneProcessInstance;
        }

        public TypeDross cloneDross() {
            return new TypeDross(name, label, explanation, sqlQuery, limiterToOneProcessInstance);
        }
        
    }

    private static TypeDross[] listTypeDross = new TypeDross[] {
            new TypeDross("ProcessWithoutTerminateEvent",
                    "Process Inconsistent",
                    "A archived process instance record (state!=6) must reference a archived (state=6), or active, process instance",
                    " from arch_process_instance api" +
                            " where api.tenantid = ? " +
                            "   and api.stateid != 6" +
                            "   and api.sourceobjectid not in" +
                            "    (select ar.sourceobjectid from arch_process_instance ar " +
                            "       where ar.stateid = 6 and ar.sourceobjectid = api.sourceobjectid)" +
                            "   and api.sourceobjectid not in (select ar.id from process_instance ar where ar.id = api.sourceobjectid)",
                    " and api.sourceobjectid = ?"),
            new TypeDross("SubProcessWithoutRootProcess",
                    "Sub Process Without Root Process",
                    "A sub process record reference a root process, which must exists in the Arch table or the Active table.",
                    " from arch_process_instance " +
                            " where tenantid=? " +
                            "   and rootprocessinstanceid != sourceobjectid" +
                            "   and rootprocessinstanceid not in " +
                            "      (select ar.rootprocessinstanceid from arch_process_instance ar " +
                            "        where ar.rootprocessinstanceid = ar.sourceobjectid " +
                            "            and ar.rootprocessinstanceid = rootprocessinstanceid)" +
                            "    and rootprocessinstanceid not in " +
                            "      (select ar.id from process_instance ar " +
                            "        where ar.id = rootprocessinstanceid)",
                    " and sourceobjectid = ?"),
            
            
            new TypeDross("SubProcessWithoutParentProcess",
                    "Sub Process Without Parent Process",
                    "A parent process may have been purged, but not the root process.",
                    " from arch_process_instance " +
                    " where tenantid=? "+
                    "  and rootprocessinstanceid != sourceobjectid " +
                    "  and callerid not in " +
                    "      (select ar.sourceobjectid from arch_flownode_instance ar " + 
                    "         where ar.rootcontainerid = rootprocessinstanceid)",
                    " and sourceobjectid = ?"),
           
            
            
            new TypeDross("ArchivedActivityAttachedToAProcessInstance",
                    "Archived Activity Attached To a ProcessInstance",
                    "A archived activity must be attached to an archived, or active, process instance",
                    " from arch_flownode_instance " +
                            " where tenantid=? " +
                            "  and parentcontainerid not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=?)" +
                            "  and parentcontainerid not in (select po.id from process_instance po where po.tenantid=?)",
                    " and parentcontainerid=?"),

            new TypeDross("FlowNodeWithoutProcessInstance",
                    "Flow Node without process instance",
                    "A flow-node must be attached to an archived, or active, process instance",
                    " from arch_flownode_instance" +
                            " where tenantid=?" +
                            " and parentcontainerid not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=?)" +
                            " and parentcontainerid not in (select po.id from process_instance po where po.tenantid=?)",
                    " and parentcontainerid=?"),

            new TypeDross("ProcessDataInstanceWithoutProcessInstance",
                    "Process Data Without Process Instance",
                    "A Process Data must be attached to an archived, or active, process instance",
                    " from arch_data_instance " +
                            "where tenantid=?" +
                            " and containertype='PROCESS_INSTANCE' " +
                            " and containerid not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=?)" +
                            " and containerid not in (select po.id from process_instance po where po.tenantid=?)",
                    " and containerid=?"),

            new TypeDross("ActivityDataInstanceWithoutActivityInstance",
                    "Activity Data without an activity instance",
                    "An activity data must be attached to an archived, or active, activity instante",
                    "from arch_data_instance " +
                            " where tenantid=?" +
                            "  and containertype='ACTIVITY_INSTANCE' " +
                            "  and containerid not in (select fl.sourceobjectid from arch_flownode_instance fl where fl.tenantid=?)",
                    " and containerid=?"),

            new TypeDross("ProcessContractDataWithoutProcessInstance",
                    "Process Contract Data without a process instance",
                    "A Contract data attached to an archived, or active, process instance",
                    " from arch_contract_data " +
                            " where tenantid=?" +
                            " and kind='PROCESS' " +
                            " and scopeid not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=? )" +
                            " and scopeid not in (select po.id from process_instance po where po.tenantid=?)",
                    "and scopeid=?"),

            new TypeDross("ActivityContractDataWithoutActivityInstance",
                    "Activity contract data without an activity instance",
                    "A Activity Contract data must be attached to an archived, or active, activity instance",
                    " from arch_contract_data " +
                            " where tenantid=?" +
                            " and kind='TASK' " +
                            " and scopeid not in (select ar.sourceobjectid from arch_flownode_instance ar where ar.tenantid=?)" +
                            " and scopeid not in (select po.id from flownode_instance po where po.tenantid=?)",
                    " and scopeid in (select ar.sourceobjectid from arch_flownode_instance ar where ar.tenantid=arch_contract_data.tenantid and ar.sourceobjectid=?)"),

            new TypeDross("DocumentMappingWithoutProcessInstance",
                    "Document mapping without a process instance",
                    "A Document Mapping must be attached to an archived, or active, process instance",
                    " from document_mapping " +
                            " where tenantid=?" +
                            " and processinstanceid not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=?)" +
                            " and processinstanceid not in (select po.id from process_instance po where po.tenantid=?)",
                    "and processintanceid=?"),

            new TypeDross("DocumentWithoutDocumentMapping",
                    "Document Without Mapping ",
                    "A Document must be attached to a document mapping",
                    " from document " +
                            " where tenantid=?" +
                            " and id not in (select documentid from document_mapping where tenantid=?)" +
                            " and id not in (select documentid from arch_document_mapping where tenantid=?)",
                    " and id in (select documentid from arch_document_mapping adm where adm.processid=?)"),

            new TypeDross("ConnectorWithoutFlowNodeInstance",
                    "Connector without flow node",
                    "A Connector must be attached to an archived, or active, process instance",
                    " from ARCH_CONNECTOR_INSTANCE " +
                            " where tenantid=?" +
                            " and CONTAINERTYPE = 'flowNode'" +
                            " and containerid not in (select ar.sourceobjectid from arch_flownode_instance ar where ar.tenantid=? )",
                    "and containerid in (select ar.sourceobjectid from arch_flownode_instance ar where ar.tenantid=ARCH_CONNECTOR_INSTANCE.tenantid and ar.parentcontainerid=?)"),

            new TypeDross("ConnectorWithoutFlowNodeInstance",
                    "Connector Without Flow Node instance",
                    "A Connector must be attached to an archived, or active, flow node instance",
                    " from ARCH_CONNECTOR_INSTANCE " +
                            " where tenantid=?" +
                            " and CONTAINERTYPE= 'process'" +
                            " and containerid not in (select ar.sourceobjectid from arch_process_instance ar where tenantid=?)",
                    "and containerid=?"),

            new TypeDross("CommentWithoutProcessInstance",
                    "Comment Without process instance",
                    "A comment must be attached to an archived, or active, process instance",
                    " from ARCH_PROCESS_COMMENT " +
                            " where tenantid=?" +
                            " and processinstanceid not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=? )" +
                            " and processinstanceid not in (select po.id from process_instance po where po.tenantid=?)",
                    " and processintanceid=?"),

            new TypeDross("BusinessReferenceWithoutProcessInstance",
                    "Business Reference without process instance",
                    "A Business Reference must be attached to an archived, or active, process instance",
                    " from ARCH_REF_BIZ_DATA_INST " +
                            " where tenantid=?" +
                            " and orig_proc_inst_id not in (select ar.sourceobjectid from arch_process_instance ar where ar.tenantid=? )",
                    "and orig_proc_inst_id=? ")

    };

    public static TypeDross[] getListTypeDross() {
        return listTypeDross;
    }

    public static class DrossExecution {

        public List<TypeDross> listDross = new ArrayList<>();
        public List<BEvent> listEvents = new ArrayList<>();
    }

    /**
     * 
     * @param tenantId
     * @return
     */
    public static DrossExecution getStatusAll(long tenantId) {
        DrossExecution drossExecution = new DrossExecution();
        for (TypeDross typeDross : listTypeDross) {
            DrossExecution drossExecutionOne = getStatus(tenantId, typeDross); 
            
            drossExecution.listDross.addAll(drossExecutionOne.listDross);            
            drossExecution.listEvents.addAll(drossExecution.listEvents);
        }
        return drossExecution;
    }
/**
 * 
 * @param tenantId
 * @param typeDross
 * @return
 */
    public static DrossExecution getStatus(long tenantId, TypeDross typeDross) {
        DrossExecution drossExecution = new DrossExecution();

        TypeDross drossCalculated = typeDross.cloneDross();
        drossExecution.listDross.add(drossCalculated);

        long count = drossCalculated.sqlQuery.chars().filter(ch -> ch == '?').count();
        List<Object> parameters = new ArrayList<>();
        for (int i = 0; i < count; i++)
            parameters.add(Long.valueOf(tenantId));
        SqlExecution sqlExecution = executeQuery("select count(*) as C " + drossCalculated.sqlQuery, parameters);
        drossExecution.listEvents.addAll(sqlExecution.listEvents);
        try {
            drossCalculated.nbRecordsDetected = Long.valueOf(sqlExecution.record.get("C").toString());
        } catch (Exception e) {
        }
        return drossExecution;
        
    }
    /**
     * 
     * @param tenantId
     * @return
     */
    public static DrossExecution deleteDrossAll(long tenantId) {
        DrossExecution drossExecution = new DrossExecution();
        for (TypeDross typeDross : listTypeDross) {
            DrossExecution drossExecutionOne = deleteDross(tenantId, typeDross); 
            
            drossExecution.listDross.addAll(drossExecutionOne.listDross);            
            drossExecution.listEvents.addAll(drossExecution.listEvents);
        }
        
        return drossExecution;
    }
    /**
     * 
     * @param tenantId
     * @param typeDross
     * @return
     */
    public static DrossExecution deleteDross(long tenantId, TypeDross typeDross) {
        DrossExecution drossExecution = new DrossExecution();
        TypeDross drossCalculated = typeDross.cloneDross();
        drossExecution.listDross.add(drossCalculated);

        long count = drossCalculated.sqlQuery.chars().filter(ch -> ch == '?').count();
        List<Object> parameters = new ArrayList<>();
        for (int i = 0; i < count; i++)
            parameters.add(Long.valueOf(tenantId));
        SqlExecution sqlExecution = executeUpdate("delete " + drossCalculated.sqlQuery, parameters);
        drossExecution.listEvents.addAll(sqlExecution.listEvents);
        drossCalculated.nbRecordsDetected = sqlExecution.nbRow;
        return drossExecution;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Private */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static class SqlExecution {

        public Map<String, Object> record;
        public int nbRow;
        public List<BEvent> listEvents = new ArrayList<>();

    }

    /**
     * internal method to get information on the waiting flownode instance
     * 
     * @param selectResult
     * @param tenantId
     * @param count
     * @param orderBy
     * @return
     */
    private static SqlExecution executeUpdate(String sqlQuery, List<Object> parameters) {
        SqlExecution sqlExecution = new SqlExecution();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Object> record = null;

        try (Connection con = BonitaEngineConnection.getConnection();) {
            pstmt = con.prepareStatement(sqlQuery);
            for (int i = 0; i < parameters.size(); i++)
                pstmt.setObject(i + 1, parameters.get(i));

            sqlExecution.nbRow = pstmt.executeUpdate();
            con.commit();
            return sqlExecution;
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGER_LABEL + "During getCountOfFlowNode : " + e.toString() + " SqlQuery[" + sqlQuery + "] at " + sw.toString());
            sqlExecution.listEvents.add(new BEvent(eventErrorExecutionQuery, e, "SqlQuery[" + sqlQuery + "]"));
            return sqlExecution;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (final SQLException localSQLException) {
                    logger.severe(LOGGER_LABEL + "During close : " + localSQLException.toString());
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (final SQLException localSQLException) {
                    logger.severe(LOGGER_LABEL + "During close : " + localSQLException.toString());
                }
            }

        }

    }

    private static SqlExecution executeQuery(String sqlQuery, List<Object> parameters) {
        SqlExecution sqlExecution = new SqlExecution();

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try (Connection con = BonitaEngineConnection.getConnection();) {
            pstmt = con.prepareStatement(sqlQuery);
            for (int i = 0; i < parameters.size(); i++)
                pstmt.setObject(i + 1, parameters.get(i));

            rs = pstmt.executeQuery();
            ResultSetMetaData rmd = pstmt.getMetaData();
            int line = 0;
            if (rs.next()) {
                sqlExecution.record = new HashMap<>();
                for (int column = 1; column <= rmd.getColumnCount(); column++)
                    sqlExecution.record.put(rmd.getColumnName(column).toUpperCase(), rs.getObject(column));
            }
            return sqlExecution;
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGER_LABEL + "During getCountOfFlowNode : " + e.toString() + " SqlQuery[" + sqlQuery + "] at " + sw.toString());
            sqlExecution.listEvents.add(new BEvent(eventErrorExecutionQuery, e, "SqlQuery[" + sqlQuery + "]"));
            return sqlExecution;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (final SQLException localSQLException) {
                    logger.severe(LOGGER_LABEL + "During close : " + localSQLException.toString());
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (final SQLException localSQLException) {
                    logger.severe(LOGGER_LABEL + "During close : " + localSQLException.toString());
                }
            }

        }

    }
}
