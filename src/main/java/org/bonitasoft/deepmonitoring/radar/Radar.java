package org.bonitasoft.deepmonitoring.radar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.session.APISession;


/* -------------------------------------------------------------------- */
/*                                                                      */
/* The Radar is something to monitor */
/*                                                                      */
/* -------------------------------------------------------------------- */


public abstract class Radar {

	
	public enum TypeRadar {LIGHT, HEAVY };
	
	protected APIAccessor mApiAccessor;
	protected String name;
	public Radar()
	{	
	}
	public abstract String getClassName();
	public abstract String getLabel();
	  
	 
	public String getName()
  {
    return name;
  }
	public  void setName(String name )
	{
	  this.name = name;
	}

	
	/**
	 * return the type. If the type is HEAVY, the TourControl will call the Radar not so often
	 * @return
	 */
	public abstract TypeRadar getType();
	private boolean isActive=false;
	    
	public Map<String,Object> getMap()
	{
	  Map<String,Object> map = new HashMap<String,Object>();
	  map.put("name", getClassName());
	  map.put("label", getLabel());
	  return map;
	}
	
	public void start(APISession apiSession)
	{
	  initialization( apiSession );
	  isActive=true;
	}
	public void stop()
	{
	  isActive=false;
	}
	public boolean isActive()
	{ return isActive; }
	
	
	public abstract void initialization(APISession apiSession);
		
	/** one photo has to be take now */
	public abstract List<RadarPhoto> takePhoto();
	  
}
