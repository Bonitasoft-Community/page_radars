package org.bonitasoft.custompage.workers;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;


public class JmxAccess {

  public void getJMXIndicator()
  {
  try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> objectNames = server.queryNames(null, null);
            for (ObjectName name : objectNames) {
                MBeanInfo info = server.getMBeanInfo(name);
                if (info.getClassName().equals(
                        "org.apache.tomcat.jdbc.pool.jmx.ConnectionPool")) {
                    for (MBeanAttributeInfo mf : info.getAttributes()) {
                        Object attributeValue = server.getAttribute(name,
                                mf.getName());
                        if (attributeValue != null) {
                            // writer.println("" + mf.getName() + " : "  + attributeValue.toString() + "<br/>");

                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
