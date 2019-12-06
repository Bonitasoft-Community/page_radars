package org.bonitasoft.deepmonitoring.radar.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.deepmonitoring.radar.Radar;
import org.bonitasoft.deepmonitoring.radar.RadarPhoto;

public class WorkerRadarPhoto  extends RadarPhoto {
  
  public boolean isWorker;
  
  public static String cstDataAttributName = "name";
  public static String cstDataAttributState = "state";
  public static String cstDataAttributWorkInformation = "workinformation";
  public static String cstDataAttributShortworkinformation = "shortworkinformation";
  
  public  WorkerRadarPhoto(Radar radar, String name, String label, boolean isWorker) {
    super(radar, name,  label);
    this.isWorker=isWorker;
  }
/* -------------------------------------------------------------------- */
/*                                                                      */
/* workerPhoto : extend and populate
/*                                                                      */
/* -------------------------------------------------------------------- */

  public static class WorkerPhoto 
  {
      public int id;
      public String name;
      public Thread.State state;
      public String workInformation="";
      public String shortWorkInformation= null;
      
      public Map<String,Object> getMap()
      {
          Map<String,Object>  map = new HashMap<String,Object>();
          map.put( cstDataAttributName ,  name);
          map.put( cstDataAttributState, state.toString());
          map.put( cstDataAttributWorkInformation, workInformation);
          map.put( cstDataAttributShortworkinformation, shortWorkInformation);
          return map;
      }
      
      public void detectCurrentWork( Thread th, boolean isWorker)
      {
        StackTraceElement[] listSt = th.getStackTrace();
        /* go from the lower to the Upper level */
        for (int i=0;i<listSt.length;i++ )
        {
          StackTraceElement st = listSt[  i ];
          workInformation+=st.getClassName()+".<i>"+st.getMethodName()+"</i><br>";
          if (i==0)
            shortWorkInformation=st.getClassName()+".<i>"+st.getMethodName();
     
          if (i==0 || i==listSt.length-1)
            continue;
          
            
          if (st.getClassName().equals("java.util.concurrent.FutureTask") && shortWorkInformation==null)
            shortWorkInformation = listSt[  i-1 ].getClassName()+listSt[  i-1 ].getMethodName();
          if (st.getClassName().equals("org.bonitasoft.engine.core.connector.impl.ConnectorServiceImpl.executeConnectorInClassloader"))
            shortWorkInformation = listSt[  i+1 ].getClassName()+listSt[  i+1 ].getMethodName();
          
          // 2 case : execute the thread (for a ConnectorThread) OR wait connector Thread (for a Worker)
          if (st.getClassName().equals("org.bonitasoft.engine.execution.work.ExecuteConnectorWork"))
          {
            if (isWorker)
              shortWorkInformation = "Wait Connector Thread";
            else      
              shortWorkInformation = listSt[  i+1 ].getClassName()+listSt[  i+1 ].getMethodName();
          }
          if (st.getClassName().equals("org.bonitasoft.engine.connector.AbstractConnector"))
            shortWorkInformation = listSt[  i-1 ].getClassName()+listSt[  i-1 ].getMethodName();
          
          if (st.getClassName().equals("bitronix.tm.resource.common.XAPool.getConnectionHandle"))
            shortWorkInformation = listSt[  i ].getClassName()+listSt[  i ].getMethodName();
          if (st.getClassName().equals("java.util.concurrent.locks.LockSupport.park"))
            shortWorkInformation = listSt[  i ].getClassName()+listSt[  i ].getMethodName();
        
          

          
          
          
        }
       
      }
  }
  
  /**
   * 
   */
    public int countRunnable=0;
    public int countBlocked=0;
    public int countWaiting=0;
    public int countTimedWaiting=0;

    //worker
    public List< WorkerPhoto> listWorkers = new ArrayList< WorkerPhoto>();
    
    // 
    long workerQueueNumber;
    
    
    public WorkerPhoto createWorkerPhoto( int workerId, Thread th )
    {
      WorkerPhoto workerPhoto = new WorkerPhoto();
      listWorkers.add(  workerPhoto);

      workerPhoto.id = workerId;
          workerPhoto.detectCurrentWork( th, isWorker );
          workerPhoto.name = th.getName();
        workerPhoto.state = th.getState();
        if (workerPhoto.state == Thread.State.RUNNABLE)
          countRunnable++;
        if (workerPhoto.state == Thread.State.BLOCKED)
          countBlocked++;
        if (workerPhoto.state == Thread.State.WAITING)
          countWaiting++;
        if (workerPhoto.state == Thread.State.TIMED_WAITING)
          countTimedWaiting++;
        return workerPhoto;
    }
    
    public void compute( boolean isWorker)
    {
      listData = new ArrayList<Map<String,Object>>();
      
        Collections.sort(listWorkers, new Comparator<WorkerPhoto>() {

          public int compare(final WorkerPhoto s1, final WorkerPhoto s2) {
              
              if (s1.name.compareTo(s2.name)==0)
                  return Integer.valueOf(s1.id).compareTo(Integer.valueOf( s2.id));
               return s1.name.compareTo(s2.name);
          }
      });
        for (WorkerPhoto workerPhoto : listWorkers)
        {
          listData.add( workerPhoto.getMap());
        }
        
        addDataHeader( new DataHeaderPhoto( cstDataAttributName, "Name", false ));
        addDataHeader( new DataHeaderPhoto( cstDataAttributState, "State",  false ));
        addDataHeader( new DataHeaderPhoto( cstDataAttributShortworkinformation, "ShortDescription" , false ));
        
        addDataHeader( new DataHeaderPhoto( cstDataAttributWorkInformation, "Location", true ));
        
        
        // main indicator : the workerQueueNumber
        if (isWorker)
        {
          IndicatorPhoto indicatorPhoto = new IndicatorPhoto();
          indicatorPhoto.label="Work Queue Number";
          indicatorPhoto.value=workerQueueNumber;
          indicatorPhoto.isMainIndicator = true;
          int totalNumberThread = countRunnable +countBlocked+ countWaiting+countTimedWaiting;
          if (workerQueueNumber < totalNumberThread)
            indicatorPhoto.analysis = "No waiting list, all in progress";
          else
            indicatorPhoto.analysis = "Waiting list : "+( workerQueueNumber - totalNumberThread);
          addIndicator( indicatorPhoto );
          
          indicatorPhoto = new IndicatorPhoto();
          indicatorPhoto.label="Worker Pool load";
           indicatorPhoto.isMainIndicator = true;
           indicatorPhoto.value=totalNumberThread ==0 ? 0 : (countRunnable+countBlocked+countTimedWaiting) / totalNumberThread;
           indicatorPhoto.percent = totalNumberThread ==0 ? 0 : (100* (countRunnable+countBlocked+countTimedWaiting)) / totalNumberThread;
           if ( indicatorPhoto.percent > 80)
            indicatorPhoto.analysis = "Works pool used, consider increased the number of Connector worker is CPU and Memory are low";
          else
            indicatorPhoto.analysis = "Working correctly";
          addIndicator( indicatorPhoto );
        }
        else
        {
          IndicatorPhoto indicatorPhoto = new IndicatorPhoto();
          indicatorPhoto.label="Connector Pool load";
           indicatorPhoto.isMainIndicator = true;
           int totalNumberThread = countRunnable +countBlocked+ countWaiting+countTimedWaiting;
           indicatorPhoto.value=totalNumberThread ==0 ? 0 : (countRunnable+countBlocked) / totalNumberThread;
           indicatorPhoto.percent=totalNumberThread ==0 ? 0 : (100* (countRunnable+countBlocked)) / totalNumberThread;
           if ( indicatorPhoto.value > 80)
            indicatorPhoto.analysis = "Connectors used, consider increased the number of Connector worker is CPU and Memory are low";
          else
            indicatorPhoto.analysis = "Working correctly";
          addIndicator( indicatorPhoto );
          
        }
        
        // synthesis
        addIndicator( getIndicator( "runnable", countRunnable,listWorkers.size(), 0,80,90 ));
        addIndicator( getIndicator( "blocked", countBlocked,listWorkers.size(),0,20,30 ));
        addIndicator( getIndicator( "waiting", countWaiting,listWorkers.size(),0,100,100 ));
        addIndicator( getIndicator( "timedwaiting", countTimedWaiting,listWorkers.size(),20,80,90 ));
     }
    
    
    
    
    private IndicatorPhoto getIndicator( String label, int value, int total, int successLevel, int warningLevel, int dangerLevel)
    {
      IndicatorPhoto indicatorPhoto = new IndicatorPhoto();
      indicatorPhoto.label=  label;
      indicatorPhoto.value= value;
      indicatorPhoto.isMainIndicator= false;
      int percent=total==0 ? 0 : (100*value)/total ;
      indicatorPhoto.percent= percent;
      if (percent>dangerLevel)
        indicatorPhoto.display= "label label-danger";
      else if (percent>warningLevel)
        indicatorPhoto.display="label label-warning";
      else if (percent>successLevel)
        indicatorPhoto.display= "label label-success";
      else
        indicatorPhoto.display= "label label-default";
        
      return indicatorPhoto;
    }
}