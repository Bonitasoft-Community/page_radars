package org.bonitasoft.deepmonitoring.radar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.deepmonitoring.radar.workers.RadarWorkers;
import org.bonitasoft.engine.session.APISession;

public class RadarFactory {

  private static RadarFactory radarFactory = new RadarFactory();
  public static RadarFactory getinstance()
  {
    return radarFactory;
  }
  /**
   * reference all the different existing radar
   */
  @SuppressWarnings("rawtypes")
private Map<String, Class> listClassRadars = new HashMap<String, Class>();
  /**
   * a new radar is instantiate. Then keep all the instantiate radar
   */
  private List<Radar> listRadars = new ArrayList<Radar>();

  private RadarFactory() {
    listClassRadars.put( RadarWorkers.classRadarName, RadarWorkers.class );
    //listClassRadars.put( RadarSql.classRadarName,  RadarSql.class);
    //listClassRadars.put( RadarCase.classRadarName, RadarCase.class );
  }
  
  public void initialisation( APISession apiSession)
  {
    
    
  for (String classRadarName : radarFactory.getListClassRadars()) {
    Radar radar = radarFactory.newRadar(classRadarName, classRadarName, apiSession);
    if (radar == null)
      continue;
  }
  }

  
  
  /** a radar is an object to monitor the different part
   * 
   */ 
  public List<String> getListClassRadars()
  {   
    List<String> listNames=new ArrayList<String>();
    for (String classRadarName : listClassRadars.keySet())
      listNames.add( classRadarName );
    return listNames;
  }
  
  /** instantiate a new Radar from its name, and instantiate it.
   * 
   * @param name
   * @param apiAccessor
   * @return
   */
  public Radar newRadar(String className, String name, APISession apiSession )
  {
    @SuppressWarnings("rawtypes")
    Class classRadar = listClassRadars.get( className );
    if (classRadar==null)
      return null;
    try
    {
      Radar radar= (Radar) classRadar.newInstance();
      radar.setName( name );
      radar.initialization(apiSession);
      listRadars.add( radar );
      return radar;
    }
    catch(Exception e)
    {
      return null;
    }
  }
  
  public List<Radar> getRadars()
  {
    return listRadars;
  }
  /**
   * instantiate a new Radar
   * @param name
   * @return
   */
  public Radar getRadars( String className, String name )
  {
    for (Radar radar : listRadars )
    {
      if (radar.getName() == name && radar.getClassName().equals( className ))
        return radar;
    }
    // we don't have one like this !
    return null;
  }
}
