
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.openbaton.exceptions.MonitoringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusSender {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private Gson mapper = new GsonBuilder().setPrettyPrinting().create();
  private String TOKEN;
  private String prometheusHost;
  private String prometheusPort;
  private String prometheusURL;
  private String username;
  private String password;
  protected boolean isAvailable;
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

  /*
   * 6 types of Prometheus HTTP API Endpoints:
   * 	- Instant queries: /api/v1/query
   * 	- Range queries: /api/v1/query_range
   * 	- Finding series by label matchers: /api/v1/series
   * 	- Querying label values: /api/v1/label/<label_name>/values
   * 	- Targets: /api/v1/targets
   * 	- Alertmanagers: /api/v1/alertmanagers
   */
  public PrometheusSender(
      String prometheusHost,
      String prometheusPort,
      String prometheusEndpoint,
      Boolean prometheusSsl,
      String username,
      String password) {
    this.prometheusHost = prometheusHost;
    this.username = username;
    this.password = password;
    String protocol = prometheusSsl ? "https://" : "http://";

    if (prometheusPort == null || prometheusPort.equals("")) {
      prometheusURL = protocol + prometheusHost + prometheusEndpoint;
    } else {
      prometheusURL = protocol + prometheusHost + ":" + prometheusPort + prometheusEndpoint;
      this.prometheusPort = prometheusPort;
    }
  }

  // Make a GET request to the Prometheus HTTP API, returning JsonObject
  public JsonObject callGet(String query) throws MonitoringException, UnirestException {
    //if(!isAvailable) throw new MonitoringException("Prometheus Server is not reachable");
    HttpResponse<String> jsonResponse = Unirest.get(prometheusURL + query).asString();
    JsonElement jsonResponseEl = null;

    try {
      jsonResponseEl = mapper.fromJson(jsonResponse.getBody(), JsonElement.class);
    } catch (Exception e) {
      log.error("Could not map the Prometheus server's response to JsonElement", e);
      throw new MonitoringException(
          "Could not map the Prometheus server's response to JsonElement", e);
    }

    if (jsonResponseEl == null)
      throw new MonitoringException("The json received from Prometheus Server is null");

    if (!jsonResponseEl.isJsonObject()) {
      throw new MonitoringException("The json received from Prometheus Server is not a JsonObject");
    } else {
      JsonObject jsonResponseObj = jsonResponseEl.getAsJsonObject();
      return jsonResponseObj;
    }
  }
}
