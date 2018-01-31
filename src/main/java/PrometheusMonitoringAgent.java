
import com.google.gson.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.openbaton.catalogue.mano.common.monitoring.AbstractVirtualizedResourceAlarm;
import org.openbaton.catalogue.mano.common.monitoring.Alarm;
import org.openbaton.catalogue.mano.common.monitoring.AlarmEndpoint;
import org.openbaton.catalogue.mano.common.monitoring.ObjectSelection;
import org.openbaton.catalogue.mano.common.monitoring.PerceivedSeverity;
import org.openbaton.catalogue.mano.common.monitoring.ThresholdDetails;
import org.openbaton.catalogue.mano.common.monitoring.ThresholdType;
import org.openbaton.catalogue.nfvo.Item;
import org.openbaton.exceptions.MonitoringException;
import org.openbaton.monitoring.interfaces.MonitoringPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusMonitoringAgent extends MonitoringPlugin {


  private String prometheusPluginIp;
  private PrometheusSender prometheusSender;

  private String notificationReceiverServerContext;
  private int notificationReceiverServerPort;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  //Server properties
  private HttpServer server;
  private Handler myHandler;

  public PrometheusMonitoringAgent() throws RemoteException {
      super();
      loadProperties();
      String prometheusHost = properties.getProperty("prometheus-host", "192.168.56.3");
      String prometheusPort = properties.getProperty("prometheus-port", "9090");

      prometheusPluginIp = properties.getProperty("prometheus-plugin-ip", "");

      String prometheusEndpoint = properties.getProperty("prometheus-endpoint", "/api/v1/query_range?query=");
      prometheusSender =
        new PrometheusSender(
            prometheusHost, prometheusPort, prometheusEndpoint, false);

      // Launch server for receiving http notifications from prometheus
      String nrsp = properties.getProperty("notification-receiver-server-port", "8010");
      notificationReceiverServerPort = Integer.parseInt(nrsp);

      notificationReceiverServerContext =
              properties.getProperty(
                      "notification-receiver-server-context", "/notifications");
      try {
          launchServer(notificationReceiverServerPort, notificationReceiverServerContext);
      } catch (IOException e) {
          throw new RemoteException(e.getMessage(), e);
      }
  }

  private void launchServer(int port, String context) throws IOException {
      server = HttpServer.create(new InetSocketAddress(port), 1);
      myHandler = new Handler();
      server.createContext(context, myHandler);
      log.debug(
              "PrometheusNotification receiver server running on url: "
                      + server.getAddress()
                      + " port:"
                      + server.getAddress().getPort());
      server.setExecutor(null);
      server.start();
  }

  @Override
  public String subscribeForFault(AlarmEndpoint filter) throws MonitoringException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String unsubscribeForFault(String alarmEndpointId) throws MonitoringException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void notifyFault(AlarmEndpoint endpoint, AbstractVirtualizedResourceAlarm event) {
    // TODO Auto-generated method stub

  }

  @Override
  public List<Alarm> getAlarmList(String vnfId, PerceivedSeverity perceivedSeverity)
      throws MonitoringException {
    // TODO Auto-generated method stub
    return null;
  }

  // Do an instant query (without period)
//  @Override
//  public List<Item> queryPMJob(List<String> hostnames, List<String> metrics, String period)
//      throws MonitoringException {
//      log.debug(String.valueOf(hostnames));
//    List<Item> result = new ArrayList<>();
//    for (String metric : metrics) {
//      try {
//        JsonObject jsonObject = prometheusSender.callGetInstantQuery(metric);
//        log.info(String.valueOf(jsonObject));
//        JsonArray ms = jsonObject.get("data").getAsJsonObject().get("result").getAsJsonArray();
//
//        for (JsonElement m : ms) {
//          String host =
//              m.getAsJsonObject().get("metric").getAsJsonObject().get("name").getAsString();
//          if (hostnames.contains(host)) {
//            Item instance = new Item();
//            instance.setMetric(metric);
//            instance.setHostname(host);
//            String value = m.getAsJsonObject().get("value").getAsJsonArray().get(1).getAsString();
//            instance.setLastValue(value);
//            instance.setValue(value);
//
//            result.add(instance);
//          }
//        }
//      } catch (UnirestException e) {
//        e.printStackTrace();
//      }
//    }
//
//    return result;
//  }
  
  @Override
  public List<Item> queryPMJob(List<String> hostnames, List<String> metrics, String period)
 	      throws MonitoringException {
 	    List<Item> result = new ArrayList<>();
	    for (String metric : metrics) {
 	      try {
 	        JsonObject jsonObject = prometheusSender.callGetRangeQuery(metric, period, 1);
 	        log.info(String.valueOf(jsonObject));
 	        JsonArray ms = jsonObject.get("data").getAsJsonObject().get("result").getAsJsonArray();
 
 	        for (JsonElement m : ms) {
 	          String host =
 	              m.getAsJsonObject().get("metric").getAsJsonObject().get("name").getAsString();
 	          if (hostnames.contains(host)) {
 	            Item instance = new Item();
 	            instance.setMetric(metric);
 	            instance.setHostname(host);
 	            String avgValue = null;
 	            double absValue = 0;
 	            JsonArray values = m.getAsJsonObject().get("values").getAsJsonArray();
 	            for(JsonElement value : values) {
 	            	absValue += value.getAsJsonArray().get(1).getAsDouble();
 	            }
 	            avgValue = Double.toString((absValue / values.size()));
 	            instance.setValue(avgValue);
 
 	            result.add(instance);
 	          }
 	        }
 	      } catch (UnirestException e) {
 	        e.printStackTrace();
 	      }
 	    }
 
 	    return result;
 	  }

  @Override
  public void subscribe() {
    // TODO Auto-generated method stub

  }

  @Override
  public void notifyInfo() {
    // TODO Auto-generated method stub

  }

  @Override
  public String createThreshold(
      ObjectSelection objectSelector,
      String performanceMetric,
      ThresholdType thresholdType,
      ThresholdDetails thresholdDetails)
      throws MonitoringException {

      /*
      TODO: Configure to create an alarm for Prometheus
       */
      return null;
  }

  @Override
  public List<String> deleteThreshold(List<String> thresholdIds) throws MonitoringException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void queryThreshold(String queryFilter) {
    // TODO Auto-generated method stub

  }


    @Override
    public String createPMJob(
            ObjectSelection objectSelection,
            List<String> performanceMetrics,
            List<String> performanceMetricGroup,
            Integer collectionPeriod,
            Integer reportingPeriod)
            throws MonitoringException {
        // Zabbix specific method
        return null;
    }

    @Override
    public List<String> deletePMJob(List<String> itemIdsToDelete) throws MonitoringException {
        // Zabbix specific
        return null;
    }
}
