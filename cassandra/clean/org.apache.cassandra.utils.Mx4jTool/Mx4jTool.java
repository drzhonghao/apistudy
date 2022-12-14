import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.*;


import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If mx4j-tools is in the classpath call maybeLoad to load the HTTP interface of mx4j.
 *
 * The default port is 8081. To override that provide e.g. -Dmx4jport=8082
 * The default listen address is 0.0.0.0. To override that provide -Dmx4jaddress=127.0.0.1
 */
public class Mx4jTool
{
    private static final Logger logger = LoggerFactory.getLogger(Mx4jTool.class);

    /**
     * Starts a JMX over http interface if and mx4j-tools.jar is in the classpath.
     * @return true if successfully loaded.
     */
    public static boolean maybeLoad()
    {
        try
        {
            logger.trace("Will try to load mx4j now, if it's in the classpath");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName processorName = new ObjectName("Server:name=XSLTProcessor");

            Class<?> httpAdaptorClass = Class.forName("mx4j.tools.adaptor.http.HttpAdaptor");
            Object httpAdaptor = httpAdaptorClass.newInstance();
            httpAdaptorClass.getMethod("setHost", String.class).invoke(httpAdaptor, getAddress());
            httpAdaptorClass.getMethod("setPort", Integer.TYPE).invoke(httpAdaptor, getPort());

            ObjectName httpName = new ObjectName("system:name=http");
            mbs.registerMBean(httpAdaptor, httpName);

            Class<?> xsltProcessorClass = Class.forName("mx4j.tools.adaptor.http.XSLTProcessor");
            Object xsltProcessor = xsltProcessorClass.newInstance();
            httpAdaptorClass.getMethod("setProcessor", Class.forName("mx4j.tools.adaptor.http.ProcessorMBean")).
                    invoke(httpAdaptor, xsltProcessor);
            mbs.registerMBean(xsltProcessor, processorName);
            httpAdaptorClass.getMethod("start").invoke(httpAdaptor);
            logger.info("mx4j successfuly loaded");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            logger.trace("Will not load MX4J, mx4j-tools.jar is not in the classpath");
        }
        catch(Exception e)
        {
            logger.warn("Could not start register mbean in JMX", e);
        }
        return false;
    }

    private static String getAddress()
    {
        return System.getProperty("mx4jaddress", FBUtilities.getBroadcastAddress().getHostAddress());
    }

    private static int getPort()
    {
        int port = 8081;
        String sPort = System.getProperty("mx4jport");
        if (sPort != null && !sPort.equals(""))
        {
            port = Integer.parseInt(sPort);
        }
        return port;
    }
}
