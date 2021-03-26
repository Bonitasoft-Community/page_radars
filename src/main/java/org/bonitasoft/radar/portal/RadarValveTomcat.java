package org.bonitasoft.radar.portal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.radar.Radar;
import org.bonitasoft.radar.RadarPhoto;
import org.bonitasoft.radar.RadarPhoto.DataHeaderPhoto;
import org.bonitasoft.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.radar.process.RadarCase;

public class RadarValveTomcat extends Radar {

    static final Logger logger = Logger.getLogger(RadarCase.class.getName());
    private static final String LOGGER_LABEL = "RadarValveTomcat ##";
    public static final String CLASS_RADAR_NAME = "RadarRestApiPortal";

    private final static BEvent eventErrorExecution = new BEvent(RadarValveTomcat.class.getName(), 1,
            Level.ERROR,
            "Error during access information", "The calculation failed", "Result is not available",
            "Check exception");

    // please use the RadarFactory method    
    public RadarValveTomcat(String name, long tenantId, APIAccessor apiAccessor) {
        super(name, CLASS_RADAR_NAME, tenantId, apiAccessor);
    }

    @Override
    public String getLabel() {
        return "Analyse Tomcat Valve. Enable the valve in <TOMCAT>/server/server.xml\n"
                +" after <Host appBase=\"webapps\" autoDeploy=\"true\" name=\"localhost\" unpackWARs=\"true\">\n"
                +" add\n"
               +" <Valve className=\"org.apache.catalina.valves.AccessLogValve\" directory=\"logs\"\n"
                       +" prefix=\"localhost_access_log\" suffix=\".log\""
                       +" pattern=\"%D;%U;%h;%s;%b;%t;&quot;%r&quot;\" >";

                
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
        return RadarResult.getInstance(true, true);
    }

    @Override
    public RadarResult deactivate() {
        // nothing to do to deactived
        return RadarResult.getInstance(true, false);
    }

    public RadarResult isActivated() {
        return RadarResult.getInstance(true, true);
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Additionnal configuration */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    @SuppressWarnings("unchecked")
    @Override
    public RadarPhotoResult takePhoto(RadarPhotoParameter radarPhotoParameter) {

        RadarPhotoResult radarPhotoResult = new RadarPhotoResult();
        RadarPhoto photoValveTomcat = new RadarPhoto(this, "Rest API Portal", "REST API Performance");
        photoValveTomcat.startShooting();
        radarPhotoResult.listPhotos.add(photoValveTomcat);

        try {
        CollectorUrlPerf  collector = new CollectorUrlPerf();

            // access the Server Logs
            List<LogRessource> listLogRessources =  getLogPath();
            Set<String> uniqPath = new HashSet<>();
            for (LogRessource logPath : listLogRessources) {
                try {
                    if (logPath.folder == null)
                        continue;

                    if (uniqPath.contains(logPath.folder.getAbsolutePath()))
                        continue;
                    uniqPath.add(logPath.folder.getAbsolutePath());
                    logger.info("LogAccess: listFiles=" + logPath.folder.getCanonicalPath());
                    final File[] listOfFiles = logPath.folder.listFiles();
                    if (listOfFiles == null) {
                        logger.info("LogAccess: no file under =" + logPath.folder.getCanonicalPath());
                        continue;
                    }
                    long markerTime =System.currentTimeMillis() - 2*24*60*60*1000;
                    // format is localhost_access_log.2021-03-26.log
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    for (int i = 0; i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile()) {
                            String fileName = listOfFiles[i].getName();
                            StringTokenizer st = new StringTokenizer(fileName,".");
                            String prefix   = st.hasMoreTokens()? st.nextToken(): null;
                            String dateFileSt = st.hasMoreTokens()? st.nextToken(): null;
                            String suffix   = st.hasMoreTokens()? st.nextToken(): null;
                            if (! "localhost_access_log".equals( prefix))
                                continue;
                            if (! ("txt".equals(suffix) || "log".equals(suffix)))
                                continue;
                            try {
                                Date dateFile = sdf.parse(dateFileSt);
                                if (dateFile.getTime() > markerTime) {
                                    collectInformationInFile(logPath.folder.getAbsolutePath()+"/"+ fileName, collector );
                                }
                                    
                            }
                            catch(Exception e) {
                                logger.severe(LOGGER_LABEL+" Exception during parse date ["+dateFileSt+" : "+e.toString());
                            }
                        }
                    }
                } catch(Exception e) 
                {
                    
                }
            }
                            
                            
                            
            photoValveTomcat.addIndicator( new IndicatorPhoto("NbRequest","Number of request received", collector.nbRequest));
            photoValveTomcat.addIndicator( new IndicatorPhoto("NbRequestMore10000","Number of request > 10 s", getInfo(collector.nbRequestMore10000, collector.nbRequest)));
            photoValveTomcat.addIndicator( new IndicatorPhoto("NbRequestMore1000","Number of request > 1 s", getInfo(collector.nbRequestMore1000, collector.nbRequest)));
            photoValveTomcat.addIndicator( new IndicatorPhoto("NbRequestMore500","Number of request > 500 ms", getInfo(collector.nbRequestMore500, collector.nbRequest)));

            photoValveTomcat.addDataHeader( new DataHeaderPhoto( "timeInMs", "time(ms)", false ));
            photoValveTomcat.addDataHeader( new DataHeaderPhoto( "URL", "URL", false ));
            photoValveTomcat.addDataHeader( new DataHeaderPhoto( "RemoteAddress", "Remote Address", false ));
            photoValveTomcat.addDataHeader( new DataHeaderPhoto( "HttpCode", "Http Code", false ));
            photoValveTomcat.addDataHeader( new DataHeaderPhoto( "Size", "Size(Bytes)", false ));
            photoValveTomcat.addDataHeader( new DataHeaderPhoto( "date", "date", false ));
            

            for (CollectorLineUrlPerf collectorLine : collector.topList) {
                Map<String, Object> record = new HashMap<>();
                record.put("timeInMs", collectorLine.timeInMs);
                record.put("URL", collectorLine.URL);
                record.put("RemoteAddress", collectorLine.remoteAddress);
                record.put("HttpCode", collectorLine.httpStatusCode);
                record.put("Size", collectorLine.sizeInBytes);
                record.put("date", collectorLine.dateUrl);
                photoValveTomcat.listData.add( record );
            }

                
                
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGER_LABEL + "During getAllProcessInstance : " + e.toString() + " at " + sw.toString() + " tenantId[" + tenantId + "]");
            radarPhotoResult.listEvents.add(new BEvent(eventErrorExecution, e, ""));

        }
        photoValveTomcat.stopShooting();
        return radarPhotoResult;
    }

    @Override
    public boolean hasHtmlDasboard() {
        return true;
    }

    public void addIndicator(RadarPhoto photo, String name, String label, long value) {
        IndicatorPhoto indicator = new IndicatorPhoto(name);
        indicator.label = label;
        indicator.setValue(value);
        photo.addIndicator(indicator);

    }

    private String getInfo( int value, int totalValue) {
        if (totalValue ==0)
            return String.valueOf(value);
        int perThousand = (int) (1000.0* (double) value/ (double) totalValue);
        
        return value+" ("+ (perThousand/10)+" %)";
    }
    /* ******************************************************************** */
    /*                                                                      */
    /* collectFile */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */
    private class CollectorLineUrlPerf {
        public long timeInMs;
        public String URL;
        public String remoteAddress;
        public String httpStatusCode;
        public String sizeInBytes;
        public String dateUrl;
        CollectorLineUrlPerf( String[] values) {
            timeInMs        = (values.length>0 ? Long.valueOf( values[0]):null);
            URL             = (values.length>1 ? values[1]:null);
            remoteAddress   = (values.length>2 ? values[2]:null);
            httpStatusCode  = (values.length>3 ? values[3]:null);
            sizeInBytes     = (values.length>4 ? values[4]:null);
            dateUrl         = (values.length>5 ? values[5]:null);
        }
    }
    private static final int CST_MAX_URLINLIST=30;
    private class CollectorUrlPerf {
        int nbRequest=0;
        int nbRequestMore10000=0;
        int nbRequestMore1000=0;
        int nbRequestMore500=0;
        List<CollectorLineUrlPerf> topList = new ArrayList<>();
        
        public void addInTop( CollectorLineUrlPerf collectorLine ) {
            if (topList.isEmpty()) {
                topList.add(collectorLine);
                return;
            }
                
            for (int i=0;i<topList.size();i++) {
                if (collectorLine.timeInMs > topList.get( i ).timeInMs) {
                    topList.add(i, collectorLine);
                    break;
                }
            }
            if (topList.size()>CST_MAX_URLINLIST)
                topList.remove(topList.size()-1);
        }
    }
    private void collectInformationInFile( String fileName, CollectorUrlPerf collector ) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                long timeInMs = Long.parseLong(values[0]);
                if ( collector.topList.isEmpty() 
                        || timeInMs > collector.topList.get( collector.topList.size()-1).timeInMs) {
                    CollectorLineUrlPerf collectorLine = new CollectorLineUrlPerf( values);
                    collector.addInTop( collectorLine );
                }
                // add in statistics
                collector.nbRequest++;
                if (timeInMs > 10000)
                    collector.nbRequestMore10000++;
                if (timeInMs > 1000)
                    collector.nbRequestMore1000++;
                if (timeInMs > 500)
                    collector.nbRequestMore500++;
            }
        }
        catch(Exception e) {
            logger.severe(LOGGER_LABEL+"Error accessing "+fileName+" : "+e.toString());
        }
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* LogPath */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */
    /**
     * @param path
     * @return
     */
    public static class LogRessource {

        public File folder = null;
        public String origin;
        public String error;

        public LogRessource(String origin, File folder) {
            this.origin = origin;
            this.folder = folder;
        }

        public LogRessource(String error) {
            this.error = error;
        }
    }

    /**
     * return All paths where logs are
     *
     * @return
     */
    protected List<LogRessource> getLogPath() {

        // nota: by then handler, we may have nothing, because the handler may
        // be attach to a parent
        // secondly, even if the handler is a FileHandler, it will not give its
        // directory :-(
        Map<String,LogRessource> listPath = new HashMap<>();

        getFilePathThomcat(listPath);
        getFilePathJboss(listPath);

        return new ArrayList( listPath.values());
    }
    /**
     * get the file when the logging is a Java.util.logging (TOMCAT usage for
     * example)
     * 
     * @return
     */
    protected void getFilePathThomcat(Map<String,LogRessource> listPath) {
        LogManager logManager = LogManager.getLogManager();
        String handlers = logManager.getProperty("handlers");

        logger.fine("LogAccess.getFilePathThomcat : LogManagerClass[" + logManager.getClass().getName() + "] TOMCAT handlers[" + handlers + "]");
        if (handlers != null) {
            StringTokenizer st = new StringTokenizer(handlers, ",");
            while (st.hasMoreTokens()) {
                String handler = st.nextToken().trim();
                String directory = logManager.getProperty(handler + ".directory");
                String fileName = logManager.getProperty(handler + ".fileName");
                logger.fine("LogAccess.getFilePathThomcat: getCanonicalPath :detect [" + handler + "] directory[" + directory + "], fileName[" + fileName + "]");
                if (directory != null) {
                    File fileDirectory = new File(directory);
                    try {
                        if (!listPath.containsKey(fileDirectory.getCanonicalPath()))
                            listPath.put(fileDirectory.getAbsolutePath(), new LogRessource("(HAN)", fileDirectory));

                    } catch (IOException e) {
                        logger.severe(LOGGER_LABEL+": getCanonicalPath Error  [" + fileDirectory.getAbsolutePath() + "] file[" + fileDirectory.getName() + "] error[" + e.toString() + "]");
                    }
                }
                if (fileName != null) {
                    File fileFileName = new File(fileName);
                    if (!listPath.containsKey(fileFileName.getParent()))
                        listPath.put(fileFileName.getParent(), new LogRessource("(HAN)", new File(fileFileName.getParent())));

                }
            }
        }

        // bonita Cloud: nothing before works...
        URL url = RadarValveTomcat.class.getResource("");
        // file:/D:/bonita/BPM-SP-7.11.2/workspace/tomcat/server/temp/bonita_portal_4648@Dragon-Pierre-Yves/tenants/1/custompage_log1607562859040/bonita-log-2.8.0.jar693201804784843185.tmp!/org/bonitasoft/page/log/
        String urlString = url.getPath();
        // remove file:
        if (urlString.startsWith("file:"))
            urlString = urlString.substring("file:".length());

        // search /tomcat/server
        int pos = urlString.toLowerCase().indexOf("server");
        if (pos != -1) {
            urlString = urlString.substring(0, pos) + "server/logs";
            File filePath = new File(urlString);
            listPath.put(filePath.getAbsolutePath(), new LogRessource("(RES):", filePath));
        }

        // Hardcoded BonitaCloud
        // ${baseDir}/${env:HOSTNAME}/bonita.log
        String baseDir = "/opt/bonita_run/logs";
        boolean foundHostname = false;
        try {

            String hostName = InetAddress.getLocalHost().getHostName();
            File filePath = new File(baseDir + "/" + hostName);                    
            listPath.put(filePath.getAbsolutePath(), new LogRessource("(BCL)", filePath));
            foundHostname = true;
        } catch (Exception e) {
            listPath.put(baseDir, new LogRessource("(BCL) Exception baseDir:[" + baseDir + "] " + e.getMessage()));
            // then check all subdirectory here
        }
        if (!foundHostname) {
            try {
                File baseDirFile = new File(baseDir);
                for (File subDir : baseDirFile.listFiles()) {
                    listPath.put(subDir.getAbsolutePath(), new LogRessource("(BCL)", subDir));
                }
            } catch (Exception e) {
                listPath.put(baseDir, new LogRessource("(BCL):Exception SubbaseDir:[" + baseDir + "] " + e.getMessage()));
            }
        }

        //------------------ Catalina
        String logPathVariable = System.getProperty("catalina.home");
        if (logPathVariable != null) {
            File fileDirectory = new File(logPathVariable);
            try {
                if (!listPath.containsKey(fileDirectory.getCanonicalPath()))
                    listPath.put(fileDirectory.getCanonicalPath(), new LogRessource("(CAT):", fileDirectory));

            } catch (IOException e) {
                logger.severe("LogAccess.getFilePathThomcat: getCanonicalPath Error  [" + fileDirectory.getAbsolutePath() + "] file[" + fileDirectory.getName() + "] error[" + e.toString() + "]");
            }

        }
        logger.fine("LogAccess.getFilePathThomcat: getCanonicalPath : by env[catalina.home] logpath=" + logPathVariable);
        logger.info("LogAccess.getFilePathThomcat: listPath=" + listPath);
        return;

    }

    /**
     * in JBOSS, the fileHandler has a "getFile"
     */
    protected void getFilePathJboss(Map<String, LogRessource> listPath) {
        LogManager logManager = LogManager.getLogManager();
        Logger loggerBonitasoft = Logger.getLogger("org.bonitasoft");
        int loopParent = -1;
        while (loggerBonitasoft != null && loopParent < 10) {
            loopParent++;
            Handler[] handlers = loggerBonitasoft.getHandlers();
            logger.fine("LogAccess.getFilePathJboss : LogManagerClass[" + logManager.getClass().getName() + "] Logger[" + loggerBonitasoft.getName() + "] handlers[" + handlers.length + "] useParent[" + loggerBonitasoft.getUseParentHandlers() + "] loopParent=" + loopParent);

            for (int i = 0; i < handlers.length; i++) {
                Handler handler = handlers[i];
                logger.fine("LogAccess.getFilePathJboss handler.className[" + handler.getClass().getName() + "]");
                if (handler.getClass().getName().equals("org.jboss.logmanager.handlers.FileHandler") || handler.getClass().getName().equals("org.jboss.logmanager.handlers.PeriodicRotatingFileHandler")) {
                    try {

                        Class<?> classHandler = handler.getClass();
                        // I don't want to load the JBOSS class in that circumstance
                        Method methodeGetFile = classHandler.getMethod("getFile", (Class[]) null);
                        File fileDirectory = (File) methodeGetFile.invoke(handler);
                        if (fileDirectory != null) {
                            try {
                                String path;
                                if (fileDirectory.isDirectory())
                                    path = fileDirectory.getCanonicalPath();
                                else
                                    path = fileDirectory.getParent();
                                if (!listPath.containsKey(path))
                                    listPath.put(path, new LogRessource("(JAN):", new File(path)));
                                logger.fine("LogAccess.getFilePathJboss Handler : file name=[" + fileDirectory.getName() + "] path=" + fileDirectory.getPath() + "] getCanonicalPath=" + fileDirectory.getCanonicalPath() + "]  getParent=" + fileDirectory.getParent() + "]");
                            } catch (Exception e) {
                                logger.severe("LogAccess.getFilePathThomcat: getCanonicalPath Error  [" + fileDirectory.getAbsolutePath() + "] file[" + fileDirectory.getName() + "] error[" + e.toString() + "]");
                            }
                        }
                    } catch (Exception e) {
                        logger.severe("LogAccess.getFilePathJboss Error during call GetFile method on a JBOSS object" + e.toString());
                    }

                }
            } // end for
              // search on the parent now
            if (loggerBonitasoft.getUseParentHandlers()) {
                loggerBonitasoft = loggerBonitasoft.getParent();
            } else
                loggerBonitasoft = null;

        }
        logger.info("LogAccess.getFilePathJboss: end detection listPath=" + listPath);
    }
}