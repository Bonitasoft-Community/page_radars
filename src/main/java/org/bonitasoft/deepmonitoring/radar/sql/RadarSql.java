package org.bonitasoft.deepmonitoring.radar.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.custompage.workers.EngineMonitoringAPI;
import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.deepmonitoring.radar.workers.WorkerRadarPhoto;
import org.bonitasoft.deepmonitoring.tool.BonitaEngineConnection;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.session.APISession;

public class RadarSql extends Radar {

  final static Logger logger = Logger.getLogger(EngineMonitoringAPI.class.getName());
  public static String loggerLabel = "DeepMonitoring ##";
 
  public static String classRadarName = "SqlBonitaEngine";

  @Override
  public String getClassName() {
    return classRadarName;
  }
  @Override
  public String getLabel() {
   return "Query in the Bonita Database in order to verify the time to access the Bonita Engine Database";
  }

  @Override
  public TypeRadar getType() {
   return TypeRadar.LIGHT;
  }
  @Override
  public void initialization(APISession apiSession) {
     
  }
  @Override
  public List<RadarPhoto> takePhoto() {
    List<RadarPhoto> listPhotos = new ArrayList<RadarPhoto>();
    RadarPhoto photoWorkers = new RadarPhoto(this, "Workers Thread","Workers thread" );
    listPhotos.add(photoWorkers);
 
  
    // number of item in the queue
    String sqlRequest = "SELECT count(a.id) FROM PROCESS_DEFINITION a ";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    long timeToExecuteInMs=0;
    int numberOfResults=0;
    try {
      con = BonitaEngineConnection.getConnection();
      pstmt = con.prepareStatement(sqlRequest);
      long beginTime=System.currentTimeMillis();
      rs = pstmt.executeQuery();
      while (rs.next()) {
        numberOfResults = rs.getInt(1);
      }
      timeToExecuteInMs = System.currentTimeMillis() - beginTime;
      IndicatorPhoto indicatorSqlExecution = new IndicatorPhoto();
      indicatorSqlExecution.label="SqlResult Time";
      indicatorSqlExecution.value=timeToExecuteInMs;
      photoWorkers.addIndicator( indicatorSqlExecution );
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
    return listPhotos;
  }

}
