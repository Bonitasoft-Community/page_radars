import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;



import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Clob;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils


import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.service.TenantServiceSingleton
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;

import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.business.data.BusinessDataRepository

import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;

import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;


import org.bonitasoft.engine.session.APISession;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ApplicationAPI;
import org.bonitasoft.engine.api.PermissionAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.ThemeAPI;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

import org.bonitasoft.properties.BonitaProperties;

import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.api.APIAccessor;


import org.bonitasoft.custompage.workers.EngineMonitoringAPI;


public class Actions {

    private static String pageName="workers";
    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage."+pageName+".groovy");




    // 2018-03-08T00:19:15.04Z
    public final static SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* doAction */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {

        // logger.info("#### PingActions:Actions start");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer();
        List<BEvent> listEvents=new ArrayList<BEvent>();
        Object jsonParam = (paramJsonSt==null ? null : JSONValue.parse(paramJsonSt));

        try {
            String action=request.getParameter("action");
            logger.info("#### "+pageName+":Actions action is["+action+"] !");
            if (action==null || action.length()==0 )
            {
                actionAnswer.isManaged=false;
                logger.info("#### "+pageName+":Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;

            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);

            long tenantId = apiSession.getTenantId();
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);
            EngineMonitoringAPI engineMonitoringAPI = new EngineMonitoringAPI();

            APIAccessor myApiAccessor = new myApiAccessor( apiSession );
            // ok, you do what you want with the initialisation
            engineMonitoringAPI.initialisation( tenantId, myApiAccessor );

            if ("refresh".equals(action))
            {
                actionAnswer.responseMap.put("collect", engineMonitoringAPI.getAllPhotos());
            }


            // actionAnswer.responseMap.put("listevents",BEventFactory.getHtml( listEvents));


            logger.info("#### "+pageName+":Actions END responseMap ="+actionAnswer.responseMap.size());
            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### "+pageName+":Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("Error", "log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);



            return actionAnswer;
        }
    }

    /**
     to create a simple chart
     */
    public static class ActivityTimeLine
    {
        public String activityName;
        public Date dateBegin;
        public Date dateEnd;

        public static ActivityTimeLine getActivityTimeLine(String activityName, int timeBegin, int timeEnd)
        {
            Calendar calBegin = Calendar.getInstance();
            calBegin.set(Calendar.HOUR_OF_DAY , timeBegin);
            Calendar calEnd = Calendar.getInstance();
            calEnd.set(Calendar.HOUR_OF_DAY , timeEnd);

            ActivityTimeLine oneSample = new ActivityTimeLine();
            oneSample.activityName = activityName;
            oneSample.dateBegin		= calBegin.getTime();
            oneSample.dateEnd 		= calEnd.getTime();

            return oneSample;
        }
        public long getDateLong()
        { return dateBegin == null ? 0 : dateBegin.getTime(); }
    }


    /** create a simple chart 
     */
    public static String getChartTimeLine(String title, List<ActivityTimeLine> listSamples){
        Logger logger = Logger.getLogger("org.bonitasoft");

        /** structure 
         * "rows": [
         {
         c: [
         { "v": "January" },"
         { "v": 19,"f": "42 items" },
         { "v": 12,"f": "Ony 12 items" },
         ]
         },
         {
         c: [
         { "v": "January" },"
         { "v": 19,"f": "42 items" },
         { "v": 12,"f": "Ony 12 items" },
         ]
         },
         */
        String resultValue="";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");

        for (int i=0;i<listSamples.size();i++)
        {
            logger.info("sample [i] : "+listSamples.get( i ).activityName+"] dateBegin["+simpleDateFormat.format( listSamples.get( i ).dateBegin)+"] dateEnd["+simpleDateFormat.format( listSamples.get( i ).dateEnd) +"]");
            if (listSamples.get( i ).dateBegin!=null &&  listSamples.get( i ).dateEnd != null)
                resultValue+= "{ \"c\": [ { \"v\": \""+listSamples.get( i ).activityName+"\" }," ;
            resultValue+= " { \"v\": \""+listSamples.get( i ).activityName +"\" }, " ;
            resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateBegin) +")\" }, " ;
            resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateEnd) +")\" } " ;
            resultValue+= "] },";
        }
        if (resultValue.length()>0)
            resultValue = resultValue.substring(0,resultValue.length()-1);

        String resultLabel = "{ \"type\": \"string\", \"id\": \"Role\" },{ \"type\": \"string\", \"id\": \"Name\"},{ \"type\": \"datetime\", \"id\": \"Start\"},{ \"type\": \"datetime\", \"id\": \"End\"}";

        String valueChart = "	{"
        valueChart += "\"type\": \"Timeline\", ";
        valueChart += "\"displayed\": true, ";
        valueChart += "\"data\": {";
        valueChart +=   "\"cols\": ["+resultLabel+"], ";
        valueChart +=   "\"rows\": ["+resultValue+"] ";
        /*
         +   "\"options\": { "
         +         "\"bars\": \"horizontal\","
         +         "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
         +         "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
         */
        valueChart +=  "}";
        valueChart +="}";
        // 				+"\"isStacked\": \"true\","

        //		    +"\"displayExactValues\": true,"
        //
        //		    +"\"hAxis\": { \"title\": \"Date\" }"
        //		    +"},"
        logger.info("Value1 >"+valueChart+"<");


        return valueChart;
    }
    /**
     * TenantAPIAccessor is not an abject, so mwe must define our own apiAccessor to give it to the librairy
     * @author Firstname Lastname
     *
     */
    public static class myApiAccessor implements APIAccessor {
        APISession apiSession;
        
        public myApiAccessor( APISession apiSession ) {
            this.apiSession = apiSession;
        }
        
        public BusinessDataAPI getBusinessDataAPI() {
            return null;            
        }
        
        public CommandAPI  getCommandAPI() {
            return TenantAPIAccessor.getCommandAPI( apiSession );
        }
        
        public PageAPI getCustomPageAPI() {
            return TenantAPIAccessor.getCustomPageAPI( apiSession );            
        }
        
        public IdentityAPI getIdentityAPI() {
            return TenantAPIAccessor.getIdentityAPI( apiSession );            
        }
        
        public  ApplicationAPI  getLivingApplicationAPI() {
            return TenantAPIAccessor.getLivingApplicationAPI( apiSession );
        }
        
        public PermissionAPI getPermissionAPI() {
            return TenantAPIAccessor.getPermissionAPI( apiSession );            
        }
        
        public ProcessAPI  getProcessAPI() {
            return TenantAPIAccessor.getProcessAPI( apiSession );            
        }
        
        public ProfileAPI  getProfileAPI() {
            return TenantAPIAccessor.getProfileAPI( apiSession );            
        }
        
        public ThemeAPI getThemeAPI() {
            return TenantAPIAccessor.getThemeAPI( apiSession );
            
        }
    }


}
