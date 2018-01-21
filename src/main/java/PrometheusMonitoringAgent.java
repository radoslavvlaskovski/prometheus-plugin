
import com.google.gson.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.net.httpserver.HttpServer;
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

  private int historyLength;
  private int requestFrequency;
  private String prometheusPluginIp;
  private PrometheusSender prometheusSender;
  private String notificationReceiverServerContext;
  private int notificationReceiverServerPort;
  private Gson mapper;
  private Random random = new Random();
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private List<AlarmEndpoint> subscriptions;
  //private Map<String, PmJob> pmJobs;
  //private Map<String, Threshold> thresholds;
  private Map<String, List<Alarm>> datacenterAlarms;
  private String type;
  private Map<String, List<String>> triggerIdHostnames;
  private Map<String, String> triggerIdActionIdMap;
  //private LimitedQueue<State> history;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  //Server properties
  private HttpServer server;
  //private MyHandler myHandler;

  public PrometheusMonitoringAgent() {
      super();
      loadProperties();
      String prometheusHost = properties.getProperty("prometheus-host", "localhost");
      String prometheusPort = properties.getProperty("prometheus-port", "9090");

      prometheusPluginIp = properties.getProperty("prometheus-plugin-ip", "");

      String prometheusEndpoint = properties.getProperty("prometheus-endpoint", "/api/v1/query_range?query=");
      prometheusSender =
        new PrometheusSender(
            prometheusHost, prometheusPort, prometheusEndpoint, false);
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
  public String createPMJob(
      ObjectSelection resourceSelector,
      List<String> performanceMetric,
      List<String> performanceMetricGroup,
      Integer collectionPeriod,
      Integer reportingPeriod)
      throws MonitoringException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> deletePMJob(List<String> itemIdsToDelete) throws MonitoringException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Item> queryPMJob(List<String> hostnames, List<String> metrics, String period)
      throws MonitoringException {
    List<Item> result = new ArrayList<>();
    for (String metric : metrics) {
      try {
//        JsonObject jsonObject = prometheusSender.callGetInstantQuery(metric);
        JsonObject jsonObject = prometheusSender.callGetRangeQuery(metric, period, 1);
        log.info(String.valueOf(jsonObject));
        JsonArray ms = jsonObject.get("data").getAsJsonObject().get("result").getAsJsonArray();

        for (JsonElement m : ms) {
          String host =
              m.getAsJsonObject().get("metric").getAsJsonObject().get("instance").getAsString();
          if (hostnames.contains(host)) {
            Item instance = new Item();
            instance.setMetric(metric);
            instance.setHostname(host);
            String value = m.getAsJsonObject().get("value").getAsJsonArray().get(1).getAsString();
            instance.setValue(value);

            result.add(instance);
          }
        }
      } catch (UnirestException e) {
        e.printStackTrace();
      }
    }

    return result;
  }
  
  public List<Item> rangeQueryJob(List<String> hostnames, List<String> metrics, String period)
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
    // TODO Auto-generated method stub
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
}
