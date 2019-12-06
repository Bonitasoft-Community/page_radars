package org.bonitasoft.deepmonitoring.tool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.bonitasoft.custompage.workers.EngineMonitoringAPI;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* getConnection */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class BonitaEngineConnection {

  final static Logger logger = Logger.getLogger(EngineMonitoringAPI.class.getName());
  public static String loggerLabel = "DeepMonitoring ##";
    
 
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
