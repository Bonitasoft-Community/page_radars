package org.bonitasoft.custompage.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * a  photo is a set of different indicator
 *
 */
public class Photo
{
  
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
          map.put("name",  name);
          map.put("state", state.toString());
          map.put("workinformation", workInformation);
          map.put("shortworkinformation", shortWorkInformation);
          return map;
      }
      
      public void detectCurrentWork( Thread th)
      {
        StackTraceElement[] listSt = th.getStackTrace();
        
        for (int i=0;i<listSt.length;i++ )
        {
          StackTraceElement st = listSt[  i ];
          workInformation+=st.getClassName()+".<i>"+st.getMethodName()+"</i><br>";
          if (i==0 || i==listSt.length-1)
            continue;
            
          if (st.getClassName().equals("java.util.concurrent.FutureTask") && shortWorkInformation==null)
            shortWorkInformation = listSt[  i-1 ].getClassName()+listSt[  i-1 ].getMethodName();
          if (st.getClassName().equals("org.bonitasoft.engine.core.connector.impl.ConnectorServiceImpl.executeConnectorInClassloader"))
            shortWorkInformation = listSt[  i+1 ].getClassName()+listSt[  i+1 ].getMethodName();
          
          if (st.getClassName().equals("org.bonitasoft.engine.execution.work.ExecuteConnectorWork"))
            shortWorkInformation = listSt[  i+1 ].getClassName()+listSt[  i+1 ].getMethodName();
          
          if (st.getClassName().equals("org.bonitasoft.engine.connector.AbstractConnector"))
            shortWorkInformation = listSt[  i-1 ].getClassName()+listSt[  i-1 ].getMethodName();
          
        }
       
      }
  }
  
  public Date datePhoto;
  
  // cpu
  public double cpuLoad;
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
    
    
    public WorkerPhoto createWorkerPhoto( int workerId, Thread th)
    {
      WorkerPhoto workerPhoto = new WorkerPhoto();
      listWorkers.add(  workerPhoto);

      workerPhoto.id = workerId;
          workerPhoto.detectCurrentWork( th );
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
    
    public Map<String,Object> getMap()
    {
        Map<String,Object>  map = new HashMap<String,Object>();
        map.put("datephotoms", datePhoto.getTime());
        map.put("cpuload", cpuLoad);
        map.put("workqueuenumber", workerQueueNumber);
        
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
        map.put("workers", list);
        
        Collections.sort(listWorkers, new Comparator<WorkerPhoto>() {

          public int compare(final WorkerPhoto s1, final WorkerPhoto s2) {
              
              if (s1.name.compareTo(s2.name)==0)
                  return Integer.valueOf(s1.id).compareTo(Integer.valueOf( s2.id));
               return s1.name.compareTo(s2.name);
          }
      });
        for (WorkerPhoto workerPhoto : listWorkers)
        {
            list.add( workerPhoto.getMap());
        }
        
        // synthesis
        List<Map<String,Object>> listIndicators = new ArrayList<Map<String,Object>>();
        map.put("indicators", listIndicators);
        listIndicators.add( getIndicator( "runnable", countRunnable,listWorkers.size(), 0,80,90 ));
        listIndicators.add( getIndicator( "blocked", countBlocked,listWorkers.size(),0,20,30 ));
        listIndicators.add( getIndicator( "waiting", countWaiting,listWorkers.size(),0,100,100 ));
        listIndicators.add( getIndicator( "timedwaiting", countTimedWaiting,listWorkers.size(),20,80,90 ));
        
        
        return map;
    }
    private Map<String,Object> getIndicator( String label, int value, int total, int successLevel, int warningLevel, int dangerLevel)
    {
      Map<String,Object> map = new HashMap<>();
      map.put("label", label);
      map.put("value", value);
      int percent=total==0 ? 0 : (100*value)/total ;
      map.put("percent", percent);
      if (percent>dangerLevel)
        map.put("display", "label label-danger");
      else if (percent>warningLevel)
        map.put("display", "label label-warning");
      else if (percent>successLevel)
        map.put("display", "label label-success");
      else
        map.put("display", "label label-default");
        
      return map;
    }
    
}