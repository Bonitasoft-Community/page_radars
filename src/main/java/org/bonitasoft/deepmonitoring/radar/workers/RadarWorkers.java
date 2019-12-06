package org.bonitasoft.deepmonitoring.radar.workers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.custompage.workers.EngineMonitoringAPI;

import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;
import org.bonitasoft.deepmonitoring.radar.workers.WorkerRadarPhoto.WorkerPhoto;
import org.bonitasoft.deepmonitoring.tool.BonitaEngineConnection;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.session.APISession;

/* -------------------------------------------------------------------- */
/*                                                                      */
/*
 * Workers monitor Workers component
 * /*
 */
/* -------------------------------------------------------------------- */

public class RadarWorkers extends Radar {

  final static Logger logger = Logger.getLogger(EngineMonitoringAPI.class.getName());
  public static String loggerLabel = "DeepMonitoring ##";

  public RadarWorkers() {
  }

  public static String classRadarName = "Workers";

  public String getClassName() {
    return classRadarName;
  }

  public String getLabel() {
    return "Display worker and Connector workers activity";
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
    WorkerRadarPhoto photoWorkers = new WorkerRadarPhoto(this, "Workers Thread","Workers thread", true);
    WorkerRadarPhoto photoConnector = new WorkerRadarPhoto(this, "Connectors Thread", "Connectors thread", false);
    listPhotos.add(photoWorkers);
    listPhotos.add(photoConnector);

    // --------------------------- Thread
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    for (Thread th : threadSet) {
      // Bonita-Worker-1-10
      if (th.getName().startsWith("Bonita-Worker")) {
        try {
          int posId = th.getName().lastIndexOf("-");
          int workerId = Integer.valueOf(th.getName().substring(posId + 1));

          // get or create it
          WorkerPhoto workerPhoto = photoWorkers.createWorkerPhoto(workerId, th);

        } catch (Exception e) {

        }
      }
      if (th.getName().startsWith("ConnectorExecutor")) {

        // how do we know which worker is link to this connector?
        int posId = th.getName().lastIndexOf("-");
        int workerId = Integer.valueOf(th.getName().substring(posId + 1));
        // get or create it
        WorkerPhoto workerPhoto = photoConnector.createWorkerPhoto(workerId, th);
      }
    }

    // now synthesis

    // number of item in the queue
    String sqlRequest = "SELECT count(f.id) FROM FLOWNODE_INSTANCE f ";
    sqlRequest += " WHERE (f.STATE_EXECUTING = ? "; // true
    sqlRequest += " OR f.STABLE = ? "; // false
    sqlRequest += " OR f.TERMINAL = ? "; // true
    sqlRequest += " OR f.STATECATEGORY = 'ABORTING' OR f.STATECATEGORY='CANCELLING')";

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      con = BonitaEngineConnection.getConnection();
      pstmt = con.prepareStatement(sqlRequest);
      pstmt.setBoolean(1, true);
      pstmt.setBoolean(2, false);
      pstmt.setBoolean(3, true);

      rs = pstmt.executeQuery();
      while (rs.next()) {
        photoWorkers.workerQueueNumber = rs.getInt(1);
      }

      // ((SpringTenantServiceAccessor)getTenantApiAccessor()).getBeanAccessor().getContext().getEnvironment().getProperty("bonita.tenant.work.corePoolSize")


      
      // compute the main indicator
      photoWorkers.compute(true);
      photoConnector.compute(false);

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
