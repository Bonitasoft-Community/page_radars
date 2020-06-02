package org.bonitasoft.radar;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.radar.archive.RadarCleanArchivedDross;
import org.bonitasoft.radar.connector.RadarTimeTrackerConnector;
import org.bonitasoft.radar.process.RadarCase;
import org.bonitasoft.radar.sql.RadarSql;
import org.bonitasoft.radar.workers.RadarWorkers;

public class RadarFactory {

    private static RadarFactory radarFactory = new RadarFactory();

    public static RadarFactory getInstance() {
        return radarFactory;
    }

    /**
     * reference all the different existing radar
     */
    @SuppressWarnings("rawtypes")
    private Map<String, Class> listClassRadars = new HashMap<>();
    /**
     * a new radar is instantiate. Then keep all the instantiate radar
     */
    private List<Radar> listRadars = new ArrayList<>();

    /**
     * Attention, add a new class here means add a getInstance()
     * Default Constructor.
     */
    private RadarFactory() {
        listClassRadars.put(RadarWorkers.CLASS_RADAR_NAME, RadarWorkers.class);
        listClassRadars.put(RadarTimeTrackerConnector.CLASS_RADAR_NAME, RadarTimeTrackerConnector.class);
        listClassRadars.put(RadarSql.CLASS_RADAR_NAME, RadarSql.class);
        listClassRadars.put(RadarCase.CLASS_RADAR_NAME, RadarCase.class);
        listClassRadars.put(RadarCleanArchivedDross.CLASS_RADAR_NAME, RadarCleanArchivedDross.class);
    }

    
   

    /**
     * a radar is an object to monitor the different part
     */
    public List<String> getListClassRadars() {
        List<String> listNames = new ArrayList<>();
        for (String classRadarName : listClassRadars.keySet())
            listNames.add(classRadarName);
        return listNames;
    }

    /**
     * instantiate a new Radar from its name, and instantiate it.
     * 
     * @param name
     * @param apiAccessor
     * @return
     */
    public Radar getInstance( String name, String className, long tenantId, APIAccessor apiAccessor) {
        Radar radar = getRadars(name, className );
        if (radar != null)
            return radar;
        // create a new one then
       
       
       
       
        try {
            if (RadarWorkers.CLASS_RADAR_NAME.equals(className))
                radar = new RadarWorkers(name,tenantId, apiAccessor);
            else if (RadarTimeTrackerConnector.CLASS_RADAR_NAME.equals(className))
                radar = new RadarTimeTrackerConnector(name,tenantId, apiAccessor);
            else if (RadarSql.CLASS_RADAR_NAME.equals(className))
                radar = new RadarSql(name,tenantId, apiAccessor);
            else if (RadarCase.CLASS_RADAR_NAME.equals(className))
                radar = new RadarCase(name,tenantId, apiAccessor);
            if (radar==null)
                return null;
            listRadars.add(radar);
            return radar;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Radar> getRadars() {
        return listRadars;
    }

    /**
     * get a new radar
     * 
     * @param name
     * @return
     */
    public Radar getRadars(String name, String className ) {
        for (Radar radar : listRadars) {
            if (radar.getName().equals( name) && radar.getName().equals(className))
                return radar;
        }
        // we don't have one like this !
        return null;
    }
    
    /**
     * get a radar, assuming the name is unique
     * 
     * @param name
     * @return
     */
    public Radar getRadarsByName(String name) {
        for (Radar radar : listRadars) {
            if (radar.getName().equals( name) )
                return radar;
        }
        // we don't have one like this !
        return null;
    }
    public Radar getRadarByClassName(String className) {
        for (Radar radar : listRadars) {
            if (radar.getClassName().equals( className) )
                return radar;
        }
        // we don't have one like this !
        return null;
    }
}
