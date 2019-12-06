package org.bonitasoft.deepmonitoring.radar;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* A photo is take, keep it */
/* A photo contains :
 * - main information (date, radar name)
 * - indicators (IndicatorPhoto)
 * - data (list of data in fact).
 */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class RadarPhoto {

  

  /**
   * main photo parameters
   */
  private String name;
  private String label;
    private Date datePhoto;
   
  private List<BEvent> listEvents = new ArrayList<BEvent>();
  private Radar radar;
  private List<IndicatorPhoto> listIndicators = new ArrayList<IndicatorPhoto>();
  
  public List<DataHeaderPhoto> listHeader = new ArrayList<DataHeaderPhoto>();
  public List<Map<String,Object>> listData = new ArrayList<Map<String,Object>>();

  
  
  
  
  public static class IndicatorPhoto
  {
    public String label;
    public long value;
    public boolean ispercent=true;
    public String display;
    public int percent;
    public boolean isMainIndicator = false;
    public String analysis;
    
    public Map<String,Object> getMap()
    {
      Map<String,Object> indicatorMap = new HashMap<String,Object>();
    indicatorMap.put("label", label);
    indicatorMap.put("value", value);
    indicatorMap.put("ispercent", ispercent);
    indicatorMap.put("display", display);
        indicatorMap.put("percent", percent); 
        indicatorMap.put("ismainindicator", isMainIndicator);
        indicatorMap.put("analysis", analysis);
        
        return indicatorMap;
    }
  }
  
  public static class DataHeaderPhoto
  {
    public String name;
    public String label;
    public boolean isDetail=false;
    public DataHeaderPhoto( String name, String label, boolean isDetail)
    {
      this.name = name;
      this.label = label;
      this.isDetail = isDetail;
    }
    
    public Map<String,Object> getMap()
     {
      Map<String,Object> headerMap = new HashMap<String,Object>();
      headerMap.put("name", name);
      headerMap.put("label", label);
      headerMap.put("isdetail", isDetail);
      
      return headerMap;
     }
    
  }
 
  /* -------------------------------------------------------------------- */
  /*                                                                      */
  /*    Object definition                                                 */
  /*                                                                      */
  /* -------------------------------------------------------------------- */
  
  public RadarPhoto(Radar radar,String name, String label)
  {
    datePhoto = new Date();
    this.name= name;
    this.label=label;
    
    this.radar = radar;
   
  }
  
  /* -------------------------------------------------------------------- */
  /*                                                                      */
  /*   Register data                                                                   */
  /*                                                                      */
  /* -------------------------------------------------------------------- */
  public void addIndicator( IndicatorPhoto indicator)
  {
    listIndicators.add(indicator );
  }
  public void addDataHeader( DataHeaderPhoto dataHeader)
  {
    listHeader.add( dataHeader);
  }
  public void setData( DataHeaderPhoto dataHeader)
  {
    listHeader.add( dataHeader);
  }
  /* -------------------------------------------------------------------- */
  /*                                                                      */
  /*                                                                      */
  /*                                                                      */
  /* -------------------------------------------------------------------- */

  public  Map<String,Object> getMap()
  {
    Map<String,Object>  map = new HashMap<String,Object>();
    map.put("datephotoms", datePhoto.getTime());
    
    // a radar photo provide : main information
    map.put("label", label);
    map.put("name", name);
    map.put("listevents", BEventFactory.getHtml( listEvents ));
    
    Map<String,Object>  photo = new HashMap<String,Object>();
    map.put("photo", photo);
    
    //---------------- list of indicators
    List<Map<String,Object>> listMapIndicators = new ArrayList<Map<String,Object>>();
      
    for (IndicatorPhoto indicator : listIndicators)
    {
  
       listMapIndicators.add( indicator.getMap() );
       
    }
    map.put("listindicators", listMapIndicators);
    
    //--------------- list of detail
    List<Map<String,Object>> listMapDetails = new ArrayList<Map<String,Object>>();
    for (DataHeaderPhoto headerPhoto : listHeader)
    {
      listMapDetails.add( headerPhoto.getMap());
    }
    map.put("listheaders", listMapDetails);
    map.put("listdata", listData);
    return map;
  }
  
  
  
}
