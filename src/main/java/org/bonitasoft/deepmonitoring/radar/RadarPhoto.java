package org.bonitasoft.deepmonitoring.radar;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* A photo is take, keep it */
/*
 * A photo contains :
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

    private List<BEvent> listEvents = new ArrayList<>();
    private Radar radar;
    private List<IndicatorPhoto> listIndicators = new ArrayList<>();

    public List<DataHeaderPhoto> listHeader = new ArrayList<>();
    public List<Map<String, Object>> listData = new ArrayList<>();

    public static class IndicatorPhoto {

        private String name;
        public String label;
        public String display;
        public boolean isMainIndicator = false;
        public String analysis;
        public String details;

        /**
         * private to avoid error
         */
        private double value;
        private double valuePercent;
        private boolean containsPercent = false;
        
        
        public IndicatorPhoto(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
        public void setValue( double value ) {
            this.value = value;
        }
        public void setPercent( double value ) {
            this.containsPercent=true;
            this.valuePercent = value;
        }
        /**
         * return the value. Maybe a percent or a absolute value
         * @return
         */
        public double getValue() {
            return value;
        }
        public boolean isPercent() {
            return containsPercent;
        }

        public Map<String, Object> getMap() {
            Map<String, Object> indicatorMap = new HashMap<>();
            indicatorMap.put("name", name);
            indicatorMap.put("label", label);
            indicatorMap.put("value", value);
            indicatorMap.put("ispercent", containsPercent);
            indicatorMap.put("display", display);
            indicatorMap.put("percent", valuePercent);
            indicatorMap.put("ismainindicator", isMainIndicator);
            indicatorMap.put("analysis", analysis);

            return indicatorMap;
        }
        
    }

    public static class DataHeaderPhoto {

        public String name;
        public String label;
        public boolean isDetail = false;

        public DataHeaderPhoto(String name, String label, boolean isDetail) {
            this.name = name;
            this.label = label;
            this.isDetail = isDetail;
        }

        public Map<String, Object> getMap() {
            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put("name", name);
            headerMap.put("label", label);
            headerMap.put("isdetail", isDetail);

            return headerMap;
        }

    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Object definition */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public RadarPhoto(Radar radar, String name, String label) {
        datePhoto = new Date();
        this.name = name;
        this.label = label;

        this.radar = radar;

    }

    public Radar getRadar() {
        return radar;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Register data */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    
    /**
     * Indicator carry the main value 
     * @param indicator
     */
    public void addIndicator(IndicatorPhoto indicator) {
        listIndicators.add(indicator);
    }

    public List<IndicatorPhoto> getListIndicators() {
    return listIndicators;
}
    
    /**
     * A photo can carry additional informations, in plus than indicator
     * @param dataHeader
     */
    public void addDataHeader(DataHeaderPhoto dataHeader) {
        listHeader.add(dataHeader);
    }

   
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /*                                                                      */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> getMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("datephotoms", datePhoto.getTime());

        // a radar photo provide : main information
        map.put("label", label);
        map.put("name", name);
        map.put("listevents", BEventFactory.getHtml(listEvents));

        Map<String, Object> photo = new HashMap<String, Object>();
        map.put("photo", photo);

        //---------------- list of indicators
        List<Map<String, Object>> listMapIndicators = new ArrayList<Map<String, Object>>();

        for (IndicatorPhoto indicator : listIndicators) {

            listMapIndicators.add(indicator.getMap());

        }
        map.put("listindicators", listMapIndicators);

        //--------------- list of detail
        List<Map<String, Object>> listMapDetails = new ArrayList<Map<String, Object>>();
        for (DataHeaderPhoto headerPhoto : listHeader) {
            listMapDetails.add(headerPhoto.getMap());
        }
        map.put("listheaders", listMapDetails);
        map.put("listdata", listData);
        return map;
    }

}
