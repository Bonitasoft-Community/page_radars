package org.bonitasoft.radar.jmx;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.radar.Radar;
import org.bonitasoft.radar.RadarPhoto;
import org.bonitasoft.radar.RadarPhoto.IndicatorPhoto;
import org.bonitasoft.radar.process.RadarCase;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;

// import javax.management.remote.JmxBuilder;
// import javax.management.*

public class JmxCall {

    // https://documentation.bonitasoft.com/bcd/3.0/how_to_enable_remote_monitoring_jmx

    /*
     * Dcom.sun.management.jmxremote=true
     * -Dcom.sun.management.jmxremote.port=9010
     * -Dcom.sun.management.jmxremote.rmi.port=9010
     * -Dcom.sun.management.jmxremote.local.only=false
     * -Dcom.sun.management.jmxremote.authenticate=false
     * -Dcom.sun.management.jmxremote.ssl=false
     * -Djava.rmi.server.hostname={{ ec2_public_dns_name | default(ansible_fqdn) }}
     */

    //Setup JMX connection.
    /*
     * def connection = new JmxBuilder().client(port: 9010, host: 'localhost') // Rajout du port
     * connection.connect()
     * // Get the MBeanServer.
     * def mbeans = connection.MBeanServerConnection
     * def activeBean = new GroovyMBean(mbeans, 'Catalina:type=Manager,host=localhost,context=/')
     * println "Active sessions: " + activeBean['activeSessions']
     */

    final static Logger logger = Logger.getLogger(JmxCall.class.getName());
    private final static String LOGGER_LABEL = "RadarJmx ##";

    public final static String CLASS_RADAR_NAME = "RadarJmx";

    private final static BEvent eventErrorExecution = new BEvent(RadarCase.class.getName(), 1,
            Level.ERROR,
            "Error during access information", "The calculation failed", "Result is not available",
            "Check exception");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Radar may want to register / start internal mechanism on start / stop */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

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

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Pulic method to access JMX Call configuration */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public String connectJmx() {
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:8080/jmxrmi");
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
            MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();
            //ObjectName should be same as your MBean name
            ObjectName mbeanName = new ObjectName("com.journaldev.jmx:type=SystemConfig");

            //Get MBean proxy instance that will be used to make calls to registered MBean
            /*
             * Object mbeanProxy =
             * MBeanServerInvocationHandler.newProxyInstance(
             * mbeanServerConnection, mbeanName, SystemConfigMBean.class, true);
             * ObjectName servletMBean = new ObjectName("WebSphere:type=ServletStats,name=myApp.Example Servlet");
             * if (mbs.isRegistered(servletMBean)) {
             * CompositeData responseTimeDetails = (CompositeData) mbs.getAttribute(servletMBean, "ResponseTimeDetails");
             */

            //close the connection
            jmxConnector.close();
        } catch (Exception e) {
            logger.info("JMX Call error");
        }
        return "";
    }
}
