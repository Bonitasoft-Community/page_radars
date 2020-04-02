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

import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarPhotoParameter;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarPhotoResult;
import org.bonitasoft.deepmonitoring.radar.Radar.RadarResult;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.deepmonitoring.tool.BonitaEngineConnection;
import org.bonitasoft.engine.api.APIAccessor;

public class RadarSql extends Radar {

    final static Logger logger = Logger.getLogger(RadarSql.class.getName());
    public final static String LOGGER_LABEL = "DeepMonitoring ##";

    public final static String CLASS_RADAR_NAME = "SqlBonitaEngine";

     // please use the RadarFactory method    
    public RadarSql(String name, long tenantId, APIAccessor apiAccessor) {
        super( name, CLASS_RADAR_NAME, tenantId, apiAccessor);
    }
    
    
    @Override
    public String getLabel() {
        return "Query in the Bonita Database in order to verify the time to access the Bonita Engine Database";
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
        
        RadarPhoto photoWorkers = new RadarPhoto(this, "Workers Thread", "Workers thread");
        radarPhotoResult.listPhotos.add(photoWorkers);

        // number of item in the queue
        String sqlRequest = "SELECT count(a.id) FROM PROCESS_DEFINITION a ";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        long timeToExecuteInMs = 0;
        int numberOfResults = 0;
        try {
            con = BonitaEngineConnection.getConnection();
            pstmt = con.prepareStatement(sqlRequest);
            long beginTime = System.currentTimeMillis();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                numberOfResults = rs.getInt(1);
            }
            timeToExecuteInMs = System.currentTimeMillis() - beginTime;
            IndicatorPhoto indicatorSqlExecution = new IndicatorPhoto("SqlTime");
            indicatorSqlExecution.label = "SqlResult Time";
            indicatorSqlExecution.setValue( timeToExecuteInMs );
            photoWorkers.addIndicator(indicatorSqlExecution);
            
            indicatorSqlExecution = new IndicatorPhoto("SqlResult");
            indicatorSqlExecution.label = "SqlResult Result";
            indicatorSqlExecution.setValue( numberOfResults );
            photoWorkers.addIndicator(indicatorSqlExecution);
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
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (final SQLException localSQLException) {
                    // do not trace it
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (final SQLException localSQLException1) {
                    // do not trace it
                }
            }
        }
        return radarPhotoResult;
    }

}
