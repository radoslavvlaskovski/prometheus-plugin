import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.openbaton.exceptions.MonitoringException;
import org.openbaton.plugin.PluginStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by rados on 12/17/2017. */
public class Starter {

  public static void main(String[] args)
      throws NoSuchMethodException, InterruptedException, IOException, InstantiationException,
          TimeoutException, IllegalAccessException, InvocationTargetException, MonitoringException {
    if (args.length == 4) {
        PluginStarter.registerPlugin(
                PrometheusMonitoringAgent.class,
                args[0],
                args[1],
                Integer.parseInt(args[2]),
                //          Integer.parseInt(args[3]));
                1);
    } else {
        PluginStarter.registerPlugin(PrometheusMonitoringAgent.class, "zabbix", "localhost",
                5672, 1);
    }
  }
}
