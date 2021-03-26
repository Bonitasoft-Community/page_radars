package org.bonitasoft.radar.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaEngineConnection;
import org.bonitasoft.radar.Radar;
import org.bonitasoft.radar.RadarPhoto;
import org.bonitasoft.radar.RadarPhoto.IndicatorPhoto;

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

    @SuppressWarnings("unchecked")
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

            result = processAPI.searchFailedProcessInstances(new SearchOptionsBuilder(0, 1).done());
            addIndicator(photoWorkers, "NbFailedCases", "Nb Failed cases", result.getCount());

            result = processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, 1).done());
            addIndicator(photoWorkers, "HumanTask", "Human task in progress", result.getCount());

            sob = new SearchOptionsBuilder(0, 1);
            sob.greaterOrEquals(HumanTaskInstanceSearchDescriptor.DUE_DATE, new Date().getTime());
            result = processAPI.searchHumanTaskInstances(sob.done());
            addIndicator(photoWorkers, "TaskDueDate", "Task with an overduedate", result.getCount());

            // how many cases in the last 24 h?
            long currentTimeMinus24H = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
            List parametersLast24H = Arrays.asList(currentTimeMinus24H);

            String sqlRequest = "select count(ID) from arch_process_instance where archivedate > ? and sourceobjectid=rootprocessinstanceid and stateid=0";
            Integer value = getSqlInteger(sqlRequest, parametersLast24H);
            IndicatorPhoto indicatorCaseCreated = new IndicatorPhoto("Cases created in last 24H");
            indicatorCaseCreated.label = "Cases created in last 24H";
            indicatorCaseCreated.setValue(value == null ? 0 : value);
            photoWorkers.addIndicator(indicatorCaseCreated);
          
            sqlRequest = "select count(ID) from arch_flownode_instance where archivedate > ? and statename='initializing'";
            value = getSqlInteger(sqlRequest, parametersLast24H);
            IndicatorPhoto indicatorTaskInitializing = new IndicatorPhoto("Tasks created in last 24H");
            indicatorTaskInitializing.label = "Tasks created in last 24H";
            indicatorTaskInitializing.setValue(value == null ? 0 : value);
            photoWorkers.addIndicator(indicatorTaskInitializing);

            sqlRequest = "select count(ID) from arch_connector_instance where archivedate > ?";
            value = getSqlInteger(sqlRequest, parametersLast24H);
            IndicatorPhoto indicatorTaskConnectorExecution = new IndicatorPhoto("Connectors Executed in last 24H");
            indicatorTaskConnectorExecution.label = "Connectors Executed ";
            indicatorTaskConnectorExecution.setValue(value == null ? 0 : value);
            photoWorkers.addIndicator(indicatorTaskConnectorExecution);

            // TODO : explore the Valve
            

            sqlRequest = "select count(ID) from flownode_instance where statename='failed'";
             value = getSqlInteger(sqlRequest, null);
            IndicatorPhoto indicatorFailedTasks = new IndicatorPhoto("Failed Tasks");
            indicatorFailedTasks.label = "Failed tasks";
            indicatorFailedTasks.setValue(value == null ? 0 : value);
            photoWorkers.addIndicator(indicatorFailedTasks);

            sqlRequest = "select count(ID) from arch_flownode_instance where archivedate > ? ";
            
            value = getSqlInteger(sqlRequest, parametersLast24H);
            IndicatorPhoto indicatorArchiveFailedTasks = new IndicatorPhoto("Failed Tasks last 24H");
            indicatorArchiveFailedTasks.label = "Failed Tasks last 24H";
            indicatorArchiveFailedTasks.setValue(value == null ? 0 : value);
            photoWorkers.addIndicator(indicatorArchiveFailedTasks);

            sqlRequest = "select ROOTCONTAINERID, count(*) as NBTASKS from arch_flownode_instance where archivedate > ? group by rootcontainerid having NBTASKS > 1000";
            List<Map<String, Object>> resultLargeCases = getSqlResult(sqlRequest, parametersLast24H, 50);
            StringBuilder resultIndicator = new StringBuilder();
            resultIndicator.append(resultLargeCases.size() + " cases: ");
            
            for (Map<String, Object> record : resultLargeCases) {
                resultIndicator.append(record.get("ROOTCONTAINERID") + " (" + record.get("NBTASKS") + "), ");
            
            }
            IndicatorPhoto indicatorLotOfTasksPerCase = new IndicatorPhoto("LotOfTasks");
            indicatorLotOfTasksPerCase.label = "Cases with more than 1000 task in last 24H";
            indicatorLotOfTasksPerCase.setValue(resultIndicator.toString());
            photoWorkers.addIndicator(indicatorLotOfTasksPerCase);

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGER_LABEL + "During getAllProcessInstance : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
            radarPhotoResult.listEvents.add(new BEvent(eventErrorExecution, e, ""));

        }
        photoWorkers.stopShooting();
        return radarPhotoResult;
    }

    @Override
    public boolean hasHtmlDasboard() {
        return true;
    }

    public void addIndicator(RadarPhoto photo, String name, String label, long value) {
        IndicatorPhoto indicator = new IndicatorPhoto(name);
        indicator.label = label;
        indicator.setValue(value);
        photo.addIndicator(indicator);

    }

    public List<Map<String, Object>> getSqlResult(String sqlRequest, List<Object> parameters, int maxRecords) {

        ResultSet rs = null;
        int numberOfResults = 0;

        List<Map<String, Object>> listResult = new ArrayList<>();
        try (Connection con = BonitaEngineConnection.getConnection(); PreparedStatement pstmt = con.prepareStatement(sqlRequest)) {
            if (parameters != null) {

                for (int i = 0; i < parameters.size(); i++) {
                    pstmt.setObject(i + 1, parameters.get(i));
                }
            }
        
            rs = pstmt.executeQuery();
            ResultSetMetaData rsMeta = rs.getMetaData();
            while (rs.next()) {
                if (numberOfResults > maxRecords)
                    break;
                numberOfResults++;
                Map<String, Object> record = new HashMap<>();
                for (int i = 1; i < rsMeta.getColumnCount(); i++) {
                    record.put(rsMeta.getColumnName(i).toUpperCase(), rs.getObject( i ));
                }
                listResult.add(record);

            }
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (final SQLException localSQLException) {
                    // do not trace it
                }
            }
        }
        return listResult;
    }

    public Integer getSqlInteger(String sqlRequest, List<Object> parameters) {
        ResultSet rs = null;
        int numberOfResults = 0;
        try (Connection con = BonitaEngineConnection.getConnection(); PreparedStatement pstmt = con.prepareStatement(sqlRequest)) {
            if (parameters != null) {

                for (int i = 0; i < parameters.size(); i++) {
                    pstmt.setObject(i + 1, parameters.get(i));
                }
            }

            rs = pstmt.executeQuery();
            if (rs.next()) {
                numberOfResults = rs.getInt(1);
            }
            return numberOfResults;
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (final SQLException localSQLException) {
                    // do not trace it
                }
            }
        }
        return null;
    }

}
