package org.bonitasoft.deepmonitoring.radar.connector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.deepmonitoring.tool.BonitaEngineConnection;
import org.bonitasoft.engine.tracking.Record;

public class ConnectorDecorator {

    private Record record;

    private Long connectorInstanceId = null;

    private String connectorName;
    private Long activityId;
    private String activityName;
    private Long activityDate;
    private Long rootCaseId;
    private Long parentCaseId;
    private Long processId;
    private String processName;
    private String processVersion;

    public ConnectorDecorator(Record record) {
        this.record = record;
        decode();
    }
    // EXECUTE_CONNECTOR_WORK/ 45126 ms processDefinitionId: 7717119015881338597 - connectorDefinitionName: Connector 45 s - connectorInstanceId: 20002

    public void decode() {
        int index = record.getDescription().lastIndexOf("connectorInstanceId:");
        if (index > 0) {
            try {
                connectorInstanceId = Long.parseLong(record.getDescription().substring(index + "connectorInstanceId:".length()).trim());

            } catch (Exception e) {
            }
        }

    }
    /* ******************************************************************************** */
    /*                                                                                  */
    /* all getter                                                                       */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
   

    public Long getConnectorInstanceId() {
        return connectorInstanceId;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public Long getRootCaseId() {
        return rootCaseId;
    }

    public void setRootCaseId(Long rootCaseId) {
        this.rootCaseId = rootCaseId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public Long getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(Long activityDate) {
        this.activityDate = activityDate;
    }

    public Long getParentCaseId() {
        return parentCaseId;
    }

    public void setParentCaseId(Long parentCaseId) {
        this.parentCaseId = parentCaseId;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }

    public void setConnectorInstanceId(Long connectorInstanceId) {
        this.connectorInstanceId = connectorInstanceId;
    }

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public String getInformation() {
        return sdf.format( new Date( record.getTimestamp()))+" Connector:[" + connectorName + "] activity:[" + activityName + "] process: [" + processName + "(" + processVersion + ")] rootCaseId: " + rootCaseId + " in " + record.getDuration() + " ms";
    }

    public long getDuration() {
        return record.getDuration();
    }
    public long getTimeStamp() {
        return record.getTimestamp();
    }
    
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Tools on ConnectorTimeRecord */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
   
    /**
     * Load a list of connector from the database
     * 
     * @param listConnectorDecorator
     * @throws Exception
     */
    public static void completeConnectorInformation(List<ConnectorDecorator> listConnectorDecorator) throws Exception {
        int i = 0;
        while (i < listConnectorDecorator.size()) {
            ConnectorDecorator.completePageConnectorInformation(listConnectorDecorator.subList(i, i + 100 < listConnectorDecorator.size() ? 100 : listConnectorDecorator.size()));
            i += 100;
        }
    }
    public static void sortByDuration(List<ConnectorDecorator> listConnectorDecorator, boolean asc)  {
        Collections.sort(listConnectorDecorator,  new Comparator<ConnectorDecorator>()
        {
            public int compare(ConnectorDecorator s1,
                    ConnectorDecorator s2)
            {
              int comparaison = Long.valueOf(s1.getDuration()).compareTo(s2.getDuration()); 
              return (asc? comparaison : - comparaison);
            }
          });
    }
        
    /**
     * Load a page, to be sure the SQL resquest is not too big
     * 
     * @param listConnectorDecorator
     * @throws Exception
     */
    private static void completePageConnectorInformation(List<ConnectorDecorator> listConnectorDecorator) throws Exception {
        List<Object> listIds = new ArrayList<>();
        
        // build the list of parameters
        Map<Long, ConnectorDecorator> mapConnector = new HashMap<>();
        StringBuilder sqlRequestParameters = new StringBuilder();
        sqlRequestParameters.append("(");
        for (int i = 0; i < listConnectorDecorator.size(); i++) {
            if (i > 0)
                sqlRequestParameters.append(",");
            sqlRequestParameters.append("?");
            mapConnector.put(listConnectorDecorator.get(i).getConnectorInstanceId(), listConnectorDecorator.get(i));
            listIds.add(listConnectorDecorator.get(i).getConnectorInstanceId());
        }
        sqlRequestParameters.append(")");
        
        List<Object> listSqlParameters = new ArrayList<>();
        
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select archCon.SOURCEOBJECTID as connectorInstanceId, archCon.name as connectorName");
        sqlRequest.append(", archFlow.SOURCEOBJECTID as activityId, archFlow.NAME as activityName, archFlow.REACHEDSTATEDATE as activityDate, archFlow.ROOTCONTAINERID as rootCaseId");
        sqlRequest.append(", archFlow.PARENTCONTAINERID as parentCaseId");
        sqlRequest.append(" , pdef.name as processName, pdef.version as processVersion, pdef.PROCESSID as processId");

        sqlRequest.append(" from ARCH_CONNECTOR_INSTANCE archCon ");
        sqlRequest.append("   left join ARCH_FLOWNODE_INSTANCE archFlow on (archFlow.SOURCEOBJECTID = archCon.CONTAINERID and archFlow.statename='completed') ");
        sqlRequest.append("   left join PROCESS_INSTANCE proc on (proc.ID = archFlow.PARENTCONTAINERID) ");
        sqlRequest.append("   left join ARCH_PROCESS_INSTANCE archPid on (archPid.SOURCEOBJECTID = archFlow.PARENTCONTAINERID and archPid.enddate>0) ");
        sqlRequest.append("   left join PROCESS_DEFINITION pdef on (pdef.PROCESSID = proc.PROCESSDEFINITIONID or pdef.PROCESSID =archPid.PROCESSDEFINITIONID) ");
        sqlRequest.append("where archCon.SOURCEOBJECTID in ");
        sqlRequest.append( sqlRequestParameters );
        listSqlParameters.addAll(listIds );

        sqlRequest.append(" union " );
        sqlRequest.append(" select conn.ID as connectorInstanceId, conn.name as connectorName ");
        sqlRequest.append(" , flow.ID as activityId, flow.NAME as activityName"); 
        sqlRequest.append(" , flow.REACHEDSTATEDATE as activityDate, flow.ROOTCONTAINERID as rootCaseId");
        sqlRequest.append(" , flow.PARENTCONTAINERID as parentCaseId");
        sqlRequest.append(" , pdef.name, pdef.version, pdef.PROCESSID as processId");
        sqlRequest.append(" from CONNECTOR_INSTANCE conn ");
        sqlRequest.append("   left join FLOWNODE_INSTANCE flow on (flow.ID = conn.CONTAINERID ) ");
        sqlRequest.append("   left join PROCESS_INSTANCE proc on (proc.ID = flow.PARENTCONTAINERID) ");
        sqlRequest.append("   left join PROCESS_DEFINITION pdef on (pdef.PROCESSID = proc.PROCESSDEFINITIONID ) ");
        sqlRequest.append(" where conn.id in ");
                
        sqlRequest.append( sqlRequestParameters );
        listSqlParameters.addAll(listIds );

                
        try {
            List<Map<String, Object>> listResult = BonitaEngineConnection.executeSqlRequest(sqlRequest.toString(), listSqlParameters);
            for (Map<String, Object> result : listResult) {
                ConnectorDecorator connectorDecorator = mapConnector.get(Long.valueOf(result.get("CONNECTORINSTANCEID").toString()));
                if (connectorDecorator != null) {
                    connectorDecorator.setActivityId(Long.valueOf(result.get("ACTIVITYID").toString()));

                    connectorDecorator.setConnectorName((String) result.get("CONNECTORNAME"));
                    connectorDecorator.setActivityName((String) result.get("ACTIVITYNAME"));
                    connectorDecorator.setActivityDate(Long.valueOf(result.get("ACTIVITYDATE").toString()));
                    connectorDecorator.setRootCaseId(Long.valueOf(result.get("ROOTCASEID").toString()));
                    connectorDecorator.setParentCaseId(Long.valueOf(result.get("PARENTCASEID").toString()));
                    connectorDecorator.setProcessName((String) result.get("PROCESSNAME"));
                    connectorDecorator.setProcessVersion((String) result.get("PROCESSVERSION").toString());
                    connectorDecorator.setProcessId(Long.valueOf(result.get("PROCESSID").toString()));
                }
            }
        } catch (Exception e) {
            throw e;
        }

    }
}
