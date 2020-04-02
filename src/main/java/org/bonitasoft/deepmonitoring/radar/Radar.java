package org.bonitasoft.deepmonitoring.radar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.deepmonitoring.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* The Radar is something to monitor */
/*                                                                      */
/* -------------------------------------------------------------------- */

public abstract class Radar {

    /**
     * a LIGHT radar does not have any footprint, does not require to install any additionnal component
     */
    public enum TypeRadar {
        LIGHT, HEAVY
    }

    protected APIAccessor apiAccessor;
    protected String name;
    protected String className;
    protected long tenantId;

    
  
    /**
     * Name should be fixed at creation, it must be give here.
     * Default Constructor.
     * @param name
     * @param tenantId
     * @param apiAccessor
     */
    protected Radar(String name, String className, long tenantId, APIAccessor apiAccessor) {
        this.name = name;
        this.className = className;
        this.tenantId = tenantId;
        this.apiAccessor = apiAccessor;
    }

    /**
     * name can be change by caller
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Information on the radar
     * @return
     */
    public String getClassName() {
        return className;
    }

    public abstract String getLabel();

    /**
     * return the type. If the type is HEAVY, the TourControl will call the Radar not so often
     * 
     * @return
     */
    public abstract TypeRadar getType();

    private boolean isActive = false;

    public Map<String, Object> getMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", getName());
        map.put("classname", getClassName());
        map.put("label", getLabel());
        return map;
    }

 
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Radar may want to register / start internal mechanism on start / stop */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public static class RadarResult {
        public List<BEvent> listEvents = new ArrayList<>();
        /**
         * Return True of False if operation is a success.
         * In case of failure, listEvents returns the reason
         */
        public boolean operationSuccess;
        /*
         * return the status of the radar
         */
        public boolean isActivated;
        public static RadarResult getInstance( boolean operationSuccess, boolean isActivated ) {
            RadarResult radarResult = new RadarResult();
            radarResult.operationSuccess = operationSuccess;
            radarResult.isActivated = isActivated;
            return radarResult;
        }
        
    }
    /**
     * this method is call when the radar is activate or started
     * After, caller can call a "takePhoto"
     * If the radar need more configuration, it has to be set before by direct method
     * 
     * @return true if the operation is a success
     */
    public abstract RadarResult activate();

    /**
     * @return
     */
    public abstract RadarResult deactivate();

    public abstract RadarResult isActivated();

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /*
     * Caller ask for a photo. 
     */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public interface RadarPhotoParameter { }
        
    public static class RadarPhotoResult {
        public List<BEvent> listEvents = new ArrayList<>();
        public List<RadarPhoto> listPhotos = new ArrayList<>();
        
        
        public List<IndicatorPhoto> getIndicators(String indicatorName ) {
            List<IndicatorPhoto> listIndicators =new ArrayList<>();
            
            for (RadarPhoto photo : listPhotos) {
               for (IndicatorPhoto indicator : photo.getListIndicators() )
                   if (indicator.getName().equals( indicatorName ))
                       listIndicators.add( indicator );
            }
            return listIndicators;
        }
    }
  
    /** one photo has to be take now 
     * Nb : some radar can have different parameters to take a photo. In that situation, the RadarPhotoParameter can be updated by Radar 
     *
     * @return
     */
    public abstract RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter);

}
