package org.bonitasoft.custompage.workers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarPhotoResult;
import org.bonitasoft.deepmonitoring.radar.RadarFactory;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;
import org.bonitasoft.engine.api.APIAccessor;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Give information on workers */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class EngineMonitoringAPI {

  private final static Logger logger = Logger.getLogger(EngineMonitoringAPI.class.getName());
  private final static String LOGGER_LABEL = "LongBoard ##";

  public static Map<String, Object> getListNameRadars() {
    Map<String, Object> map = new HashMap<>();
    RadarFactory radarFactory = RadarFactory.getInstance();
    map.put("radars", radarFactory.getListClassRadars());
    return map;
  }

  /**
   * Initialisation :
   * At this moment, we create one instance of radar per class
   * @param apiAccessor
   */
  public void initialisation(long tenantId, APIAccessor apiAccessor )
  {
    // Ok, let's prepare all initialisation
    RadarFactory radarFactory = RadarFactory.getInstance();
  }
  /**
   * get a static photo on all the radar
   * Nota: some radar may need to be started to do the job: then the give what they can give.
   * 
   * @return
   */
  public static Map<String, Object> getAllPhotos() {
    Map<String, Object> map = new HashMap<>();
    long timeBegin = System.currentTimeMillis();

    List<Map<String, Object>> listRadarsPhoto = new ArrayList<>();
    RadarFactory radarFactory = RadarFactory.getInstance();
    for (Radar radar : radarFactory.getRadars()) {
      
        RadarPhotoResult radarPhotoResult = radar.takePhoto( null );

      Map<String, Object> radarMap = radar.getMap();
      List<Map<String, Object>> listPhotos = new ArrayList<>();
      for (RadarPhoto photo : radarPhotoResult.listPhotos) {
        listPhotos.add(photo.getMap());
      }
      radarMap.put("photos", listPhotos);
      listRadarsPhoto.add( radarMap );
    }

    map.put("radars", listRadarsPhoto);
    map.put("timecollectms", System.currentTimeMillis() - timeBegin);

    map.put("datecollectms", timeBegin);

    return map;

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
      logger.info(LOGGER_LABEL + ".getDataSourceConnection() check[" + dataSourceString + "]");
      try {
        final Context ctx = new InitialContext();
        final DataSource dataSource = (DataSource) ctx.lookup(dataSourceString);
        logger.info(LOGGER_LABEL + ".getDataSourceConnection() [" + dataSourceString + "] isOk");
        return dataSource.getConnection();

      } catch (NamingException e) {
        logger.info(
            LOGGER_LABEL + ".getDataSourceConnection() error[" + dataSourceString + "] : " + e.toString());
        msg += "DataSource[" + dataSourceString + "] : error " + e.toString() + ";";
      }
    }
    logger.severe(LOGGER_LABEL + ".getDataSourceConnection: Can't found a datasource : " + msg);
    return null;
  }

}
