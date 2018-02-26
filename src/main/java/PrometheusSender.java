
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.openbaton.exceptions.MonitoringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class PrometheusSender {

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private Gson mapper = new GsonBuilder().setPrettyPrinting().create();
  private String prometheusHost;
  private String prometheusPort;
  private String prometheusURL;
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
      Boolean prometheusSsl) {
    this.prometheusHost = prometheusHost;
    String protocol = prometheusSsl ? "https://" : "http://";

    if (prometheusPort == null || prometheusPort.equals("")) {
      prometheusURL = protocol + prometheusHost + prometheusEndpoint;
    } else {
      prometheusURL = protocol + prometheusHost + ":" + prometheusPort + prometheusEndpoint;
      this.prometheusPort = prometheusPort;
    }
  }

  // Make a GET request to the Prometheus HTTP API, returning JsonObject
  public JsonObject callGetInstantQuery(String query) throws MonitoringException, UnirestException {
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
  
  public JsonObject callGetRangeQuery(String query, String period, int step) 
		  throws MonitoringException, UnirestException {
    //if(!isAvailable) throw new MonitoringException("Prometheus Server is not reachable");
    String endTime =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date(((System.currentTimeMillis() / 1000) - 3600) * 1000)); // convert to UTC
    String startTime =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date((((System.currentTimeMillis() / 1000) - 3600) - Long.parseLong(period)) * 1000));
    String endTimeFormatted =
        endTime.substring(0, 10) + 'T' + endTime.substring(11, endTime.length()) + 'Z';
    String startTimeFormatted =
        startTime.substring(0, 10) + 'T' + startTime.substring(11, startTime.length()) + 'Z';
	String url = prometheusURL + query + "&start=" + startTimeFormatted 
			+ "&end=" + endTimeFormatted + "&step=" + step + 's';
    HttpResponse<String> jsonResponse = Unirest.get(url).asString();
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
  
  public void addTarget(String pathToFile, String job, String ip, int port) throws Exception {

      try {
          InputStream input = new FileInputStream(new File(pathToFile));
          Yaml yaml = new Yaml();
          Map<String, Object> globalMap = (Map<String, Object>) yaml.load(input);
          input.close();
          ArrayList<Map<String, Object>> jobs = (ArrayList<Map<String, Object>>) globalMap.get("scrape_configs");

          for (Map<String, Object> scrapeJob : jobs) {
              if (scrapeJob.containsKey("job_name")){
                  if(scrapeJob.get("job_name").equals(job)){
                      if(scrapeJob.containsKey("static_configs")){
                          ArrayList<Map<String, Object>> configs = (ArrayList<Map<String, Object>>) scrapeJob.get("static_configs");
                          for(Map<String, Object> config : configs){
                              if(config.containsKey("targets")){
                                  ArrayList<String> targets = (ArrayList<String>) config.get("targets");
                                  targets.add(ip + ":" + port);
                                  FileWriter writer = new FileWriter(pathToFile);
                                  yaml.dump(globalMap, writer);
                                  writer.close();
                              }
                          }
                      }
                  }
              }

          }
      }
      catch (Exception e){
          throw new Exception("Failed updating prometheus config");
      }
  }
}
