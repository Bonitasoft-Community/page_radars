package org.bonitasoft.deepmonitoring.radar.process;

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
import org.bonitasoft.deepmonitoring.radar.Radar.TypeRadar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.deepmonitoring.tool.BonitaEngineConnection;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;



public class RadarCase  extends Radar {
final static Logger logger = Logger.getLogger(EngineMonitoringAPI.class.getName());
public static String loggerLabel = "DeepMonitoring ##";

public static String classRadarName = "RadarCases";

@Override
public String getClassName() {
  return classRadarName;
}
@Override
public String getLabel() {
 return "Number of active case";
}

@Override
public TypeRadar getType() {
 return TypeRadar.LIGHT;
}

APISession apiSession;

@Override
public void initialization(APISession apiSession) {
   this.apiSession = apiSession;
}
@Override
public List<RadarPhoto> takePhoto() {
  List<RadarPhoto> listPhotos = new ArrayList<RadarPhoto>();
  RadarPhoto photoWorkers = new RadarPhoto(this, "Workers Thread","Workers thread" );
  listPhotos.add(photoWorkers);

  try
  {
      ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI( apiSession );
  
  long processInstance = processAPI.getNumberOfProcessInstances();
  
  
    IndicatorPhoto indicatorCases = new IndicatorPhoto();
    indicatorCases.label="Nb cases";
    indicatorCases.value=processInstance;
    photoWorkers.addIndicator( indicatorCases );
  } catch (Exception e) {
    final StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString());
  } finally {
    
  }
  return listPhotos;
}
}

