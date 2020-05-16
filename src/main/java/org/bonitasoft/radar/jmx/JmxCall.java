package org.bonitasoft.radar.jmx;

//import javax.management.remote.JmxBuilder;
// import javax.management.*

public class JmxCall {
    
    
    // https://documentation.bonitasoft.com/bcd/3.0/how_to_enable_remote_monitoring_jmx

    /*
    Dcom.sun.management.jmxremote=true
            -Dcom.sun.management.jmxremote.port=9010
            -Dcom.sun.management.jmxremote.rmi.port=9010
            -Dcom.sun.management.jmxremote.local.only=false
            -Dcom.sun.management.jmxremote.authenticate=false
            -Dcom.sun.management.jmxremote.ssl=false
            -Djava.rmi.server.hostname={{ ec2_public_dns_name | default(ansible_fqdn) }}
    */
    
    //Setup JMX connection.
    /*
    def connection = new JmxBuilder().client(port: 9010, host: 'localhost') // Rajout du port
    connection.connect()

    // Get the MBeanServer.
    def mbeans = connection.MBeanServerConnection

    def activeBean = new GroovyMBean(mbeans, 'Catalina:type=Manager,host=localhost,context=/')
    println "Active sessions: " + activeBean['activeSessions']
*/


}
