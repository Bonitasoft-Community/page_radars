package org.bonitasoft.custompage.workers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.bonitasoft.custompage.workers.Photo.WorkerPhoto;


/* -------------------------------------------------------------------- */
/*                                                                      */
/* Give information on workers */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class EngineMonitoringAPI {

  final static Logger logger = Logger.getLogger(EngineMonitoringAPI.class.getName());
  public static String loggerLabel = "LongBoard ##";
    
   
    
    
    public static Map<String,Object> getWorkersInfo()
    {
        
        Photo photo = calculatePhoto();
        
        return photo.getMap();
        
    }
    
    private static Photo calculatePhoto()
    {
        Photo photo = new Photo();
        photo.datePhoto = new Date();

        // --------------------------- Thread
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread th : threadSet )
        {
            // Bonita-Worker-1-10
            if (th.getName().startsWith("Bonita-Worker"))
            {
                try
                {
                    int posId = th.getName().lastIndexOf("-");
                    int workerId = Integer.valueOf(  th.getName().substring(posId+1));
                    
                    // get or create it
                    WorkerPhoto workerPhoto = photo.createWorkerPhoto( workerId, th );
                                      
                }
                catch(Exception e)
                {
                    
                }
            }
            if (th.getName().startsWith("ConnectorExecutor"))
            {
                
                // how do we know which worker is link to this connector?
                int posId = th.getName().lastIndexOf("-");
                int workerId = Integer.valueOf(  th.getName().substring(posId+1));
                // get or create it
                WorkerPhoto workerPhoto = photo.createWorkerPhoto( workerId, th );
            }
        }
        // --------------------------- CPU and memory
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
         photo.cpuLoad = osBean.getSystemLoadAverage();
        
        
        // now synthesis

        
        // number of item in the queue
        String sqlRequest="SELECT count(f.id) FROM FLOWNODE_INSTANCE f ";
        sqlRequest += " WHERE (f.STATE_EXECUTING = ? "; // true
        sqlRequest += " OR f.STABLE = ? "; // false
        sqlRequest += " OR f.TERMINAL = ? "; // true
        sqlRequest += " OR f.STATECATEGORY = 'ABORTING' OR f.STATECATEGORY='CANCELLING')";
        
        Connection con = null;
        PreparedStatement pstmt=null;
        ResultSet rs = null;
        try
        {
          con = getConnection();
        pstmt = con.prepareStatement(sqlRequest);
        pstmt.setBoolean(1,  true);
        pstmt.setBoolean(2,  false);
        pstmt.setBoolean(3,  true);
        
        rs = pstmt.executeQuery();
        while (rs.next()) {
            photo.workerQueueNumber = rs.getInt(1);
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
              }
          }
          if (pstmt != null) {
              try {
                  pstmt.close();
                  pstmt = null;
              } catch (final SQLException localSQLException) {
              }
          }
          if (con != null) {
              try {
                  con.close();
                  con = null;
              } catch (final SQLException localSQLException1) {
              }
          }
      }
        
        
        return photo;
    }
    
    private static void sortTheList(List<Map<String, Object>> listToSort, final String attributName) {

      Collections.sort(listToSort, new Comparator<Map<String, Object>>() {

          public int compare(final Map<String, Object> s1, final Map<String, Object> s2) {
              
              StringTokenizer st = new StringTokenizer(attributName,  ";");
              while (st.hasMoreTokens())
              {
                  String token = st.nextToken();
                  Object d1 = s1.get( token );
                  Object d2 = s2.get( token );
                  if (d1!=null && d2!=null)
                  {
                      int comparaison=0;
                      if (d1 instanceof String)
                          comparaison=((String) d1).compareTo( ((String)d2));
                      if (d1 instanceof Integer)
                          comparaison=((Integer) d1).compareTo( ((Integer)d2));
                      if (d1 instanceof Long)
                          comparaison=((Long) d1).compareTo( ((Long)d2));
                      if (comparaison!=0)
                          return comparaison;
                  }
                  // one is null, or both are null : continue
              }
              return 0;
          }
      });
      ;
  }

    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* getConnection */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    private static List<String> listDataSources = Arrays.asList("java:/comp/env/bonitaSequenceManagerDS",
            "java:jboss/datasources/bonitaSequenceManagerDS");

    /**
     * getConnection
     * 
     * @return
     * @throws NamingException
     * @throws SQLException
     */

    public static Connection getConnection() throws SQLException {
        // logger.info(loggerLabel+".getDataSourceConnection() start");

        String msg = "";
        List<String> listDatasourceToCheck = new ArrayList<String>();
        for (String dataSourceString : listDataSources)
            listDatasourceToCheck.add(dataSourceString);

        for (String dataSourceString : listDatasourceToCheck) {
            logger.info(loggerLabel + ".getDataSourceConnection() check[" + dataSourceString + "]");
            try {
                final Context ctx = new InitialContext();
                final DataSource dataSource = (DataSource) ctx.lookup(dataSourceString);
                logger.info(loggerLabel + ".getDataSourceConnection() [" + dataSourceString + "] isOk");
                return dataSource.getConnection();

            } catch (NamingException e) {
                logger.info(
                        loggerLabel + ".getDataSourceConnection() error[" + dataSourceString + "] : " + e.toString());
                msg += "DataSource[" + dataSourceString + "] : error " + e.toString() + ";";
            }
        }
        logger.severe(loggerLabel + ".getDataSourceConnection: Can't found a datasource : " + msg);
        return null;
    }
    
    
}
