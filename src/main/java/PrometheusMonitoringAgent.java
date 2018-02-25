
import com.google.gson.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.net.httpserver.HttpServer;

import prometheus.api.PrometheusApiManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
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
  private PrometheusApiManager pam;

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

    String prometheusEndpoint =
        properties.getProperty("prometheus-endpoint", "/api/v1/query_range?query=");
    prometheusSender =
        new PrometheusSender(prometheusHost, prometheusPort, prometheusEndpoint, false);
    pam = new PrometheusApiManager();
    // Launch server for receiving http notifications from prometheus
    String nrsp = properties.getProperty("notification-receiver-server-port", "8010");
    notificationReceiverServerPort = Integer.parseInt(nrsp);

    notificationReceiverServerContext =
        properties.getProperty("notification-receiver-server-context", "/notifications");
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
  
  @Override
  public List<Item> queryPMJob(List<String> hostnames, List<String> metrics, String period)
 	      throws MonitoringException {
 	    List<Item> result = new ArrayList<>();
	    for (String metric : metrics) {
 	      try {
 	        JsonObject jsonObject = prometheusSender.callGetRangeQuery(metric, period, 1);
 	        log.info(String.valueOf(jsonObject));
 	        JsonArray ms = jsonObject.get("data").getAsJsonObject().get("result").getAsJsonArray();

 	        for(String searchedHost : hostnames){
                Item instance = new Item();
                instance.setMetric(metric);
                instance.setHostname(searchedHost);
                instance.setValue(null);
                for (JsonElement m : ms) {
                    String host =
                            m.getAsJsonObject().get("metric").getAsJsonObject().get("name").getAsString();
                    String job = m.getAsJsonObject().get("metric").getAsJsonObject().get("job").getAsString();
                    if (host.contains(searchedHost + "-") && job.equals("cadvisor")) {
                        String avgValue = null;
                        double absValue = 0;
                        JsonArray values = m.getAsJsonObject().get("values").getAsJsonArray();
                        for(JsonElement value : values) {
                            absValue += value.getAsJsonArray().get(1).getAsDouble();
                        }
                        avgValue = Double.toString((absValue / values.size()));
                        instance.setValue(avgValue);
                    }
                }
                if(instance.getValue() != null)
                    result.add(instance);

            }

 	      } catch (UnirestException e) {
 	        e.printStackTrace();
 	      }
 	    }
        log.debug(String.valueOf(result));
 	    return result;
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

    if (objectSelector == null)
      throw new MonitoringException("The objectSelector is null or empty");
    if ((performanceMetric == null && performanceMetric.isEmpty()))
      throw new MonitoringException("The performanceMetric needs to be present");
    if (thresholdDetails == null) throw new MonitoringException("The thresholdDetails is null");
    //TODO Investigate which are the cases where we need more than one objectSelector
    
    String thresholdExpression = "expr: " + performanceMetric + thresholdDetails.getTriggerOperator() 
    	+ thresholdDetails.getValue();
    pam.createTrigger(thresholdExpression);
    
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
    return null;
  }

  @Override
  public List<String> deletePMJob(List<String> itemIdsToDelete) throws MonitoringException {
    // Zabbix specific
    return null;
  }
}
