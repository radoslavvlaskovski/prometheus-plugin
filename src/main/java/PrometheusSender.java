
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openbaton.exceptions.MonitoringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

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
	  public PrometheusSender(String prometheusHost,
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

	  public synchronized HttpResponse<String> doRestCallWithJson(
	      String url, String json, HttpMethod method, String contentType) throws UnirestException {
	    HttpResponse<String> response = null;
	    switch (method) {
	      case PUT:
	        response =
	            Unirest.put(url)
	                .header("Content-type", contentType)
	                .header("KeepAliveTimeout", "5000")
	                .body(json)
	                .asString();
	        break;
	      case POST:
	        response =
	            Unirest.post(url)
	                .header("Content-type", contentType)
	                .header("KeepAliveTimeout", "5000")
	                .body(json)
	                .asString();
	        break;
	      case GET:
	        response = Unirest.get(url).asString();
	        break;
	    }
	    return response;
	  }
	  
	  // Make a GET request to the Prometheus HTTP API, returning JsonObject
	  public JsonObject callGet(String query) throws MonitoringException, UnirestException {
		  if(!isAvailable) throw new MonitoringException("Prometheus Server is not reachable");
		  HttpResponse<String> jsonResponse = Unirest.get(prometheusURL + query).asString();
		  JsonElement jsonResponseEl = null;
		  
		  try {
			  jsonResponseEl = mapper.fromJson(jsonResponse.getBody(), JsonElement.class);
		  } catch (Exception e) {
			  log.error("Could not map the Prometheus server's response to JsonElement", e);
			  throw new MonitoringException("Could not map the Prometheus server's response to JsonElement", e);
		  }
		  
		  if(jsonResponseEl == null) throw new MonitoringException(
				  "The json received from Prometheus Server is null");
		  
		  if(!jsonResponseEl.isJsonObject()) {
			  throw new MonitoringException("The json received from Prometheus Server is not a JsonObject");
		  } else {
			  JsonObject jsonResponseObj = jsonResponseEl.getAsJsonObject();
			  return jsonResponseObj;
		  }
	  }

	  public JsonObject callPost(String content, String method) throws MonitoringException {
	    if (!isAvailable) throw new MonitoringException("Prometheus Server is not reachable");
	    HttpResponse<String> jsonResponse = null;

	    String body = prepareJson(content, method);
	    try {
	      jsonResponse = doRestCallWithJson(prometheusURL, body, HttpMethod.POST, "application/json-rpc");
	      if (checkAuthorization(jsonResponse.getBody())) {
	        this.TOKEN = null;
	        /*
	         * authenticate again, because the last token is expired
	         */
	        authenticate(prometheusHost, username, password);
	        body = prepareJson(content, method);
	        jsonResponse = doRestCallWithJson(prometheusURL, body, HttpMethod.POST, "application/json-rpc");
	      }
	      //log.debug("Response received: " + jsonResponse);
	    } catch (UnirestException e) {
	      log.error("Post on the Prometheus server failed", e);
	      throw new MonitoringException(e.getMessage(), e);
	    }

	    JsonElement responseEl = null;
	    try {
	      responseEl = mapper.fromJson(jsonResponse.getBody(), JsonElement.class);
	    } catch (Exception e) {
	      log.error("Could not map the Prometheus server's response to JsonElement", e);
	      throw new MonitoringException("Could not map the Prometheus server's response to JsonElement", e);
	    }
	    if (responseEl == null || !responseEl.isJsonObject())
	      throw new MonitoringException(
	          "The json received from Prometheus Server is not a JsonObject or null");
	    JsonObject responseObj = responseEl.getAsJsonObject();

	    if (responseObj.get("error") != null) {
	      JsonObject errorObj = (JsonObject) responseObj.get("error");
	      throw new MonitoringException(
	          errorObj.get("message").getAsString() + " " + errorObj.get("data").getAsString());
	    }
	    return responseObj;
	  }

	  private String prepareJson(String content, String method) {

	    String s = "{'params': " + content + "}";

	    JsonObject jsonContent = mapper.fromJson(s, JsonObject.class);
	    JsonObject jsonObject = jsonContent.getAsJsonObject();

	    jsonObject.addProperty("jsonrpc", "2.0");
	    jsonObject.addProperty("method", method);

	    if (TOKEN != null && !method.equals("user.login")) jsonObject.addProperty("auth", TOKEN);
	    jsonObject.addProperty("id", 1);

	    //log.debug("Json for zabbix:\n" + mapper.toJson(jsonObject));
	    return mapper.toJson(jsonObject);
	  }

	  private boolean checkAuthorization(String body) {
	    boolean isAuthorized = false;
	    JsonElement error;
	    JsonElement data = null;
	    JsonObject responseOb;

	    responseOb = mapper.fromJson(body, JsonObject.class);

	    if (responseOb == null) {
	      return isAuthorized;
	    }

	    //log.debug("Check authorization in this response:" + responseOb);

	    error = responseOb.get("error");
	    if (error == null) {
	      return isAuthorized;
	    }
	    //log.debug("AUTHENTICATION ERROR  ----->   "+error + " ---> Retrying");

	    if (error.isJsonObject()) data = ((JsonObject) error).get("data");
	    if (data.getAsString().equals("Not authorised")) {
	      isAuthorized = true;
	      return isAuthorized;
	    }

	    return false;
	  }

	  public void authenticate(String prometheusHost, String username, String password)
	      throws MonitoringException {
	    this.prometheusHost = prometheusHost;
	    this.username = username;
	    this.password = password;
	    this.authenticateToPrometheus();
	  }

	  private void startWatcher() {
	    Watcher watcher = new Watcher();
	    executorService.scheduleAtFixedRate(watcher, 0, 10, TimeUnit.SECONDS);
	  }

	  public void destroy() {
	    executorService.shutdown();
	  }

	  public void authenticate() {
	    startWatcher();
	  }

	  protected void authenticateToPrometheus() throws MonitoringException {
	    String params = "{'user':'" + username + "','password':'" + password + "'}";

	    JsonObject responseObj = callPost(params, "user.login");
	    JsonElement result = responseObj.get("result");
	    if (result == null) {
	      throw new MonitoringException("problem during the authentication");
	    }
	    this.TOKEN = result.getAsString();

	    log.debug("Authenticated to Prometheus Server " + prometheusURL + " with TOKEN " + TOKEN);
	  }

	  private class Watcher implements Runnable {
	    @Override
	    public void run() {
	      String jsonRequest =
	          "{\"jsonrpc\":\"2.0\",\"method\":\"apiinfo.version\",\"id\":1,\"auth\":null,\"params\":{}}";
	      String prometheusVersion = null;
	      try {
	        if (!isAvailable) {
	          log.info("");
	          log.info("Trying to connect to Prometheus at url: " + prometheusURL + "...");
	        }
	        HttpResponse<String> response =
	            doRestCallWithJson(prometheusURL, jsonRequest, HttpMethod.POST, "application/json-rpc");
	        JsonElement responseEl = mapper.fromJson(response.getBody(), JsonElement.class);
	        JsonObject responseObj = responseEl.getAsJsonObject();
	        prometheusVersion = responseObj.get("result").getAsString();
	      } catch (Exception e) {
	        log.error("Prometheus Server not reachable at this url: " + prometheusURL);
	        log.debug(e.getMessage());
	        isAvailable = false;
	        return;
	      }
	      if (!isAvailable) {
	        isAvailable = true;
	        log.info("Connected to Prometheus " + prometheusVersion + " at url: " + prometheusURL);
	        log.debug("trying autentication..");
	        try {
	          authenticateToPrometheus();
	        } catch (Exception e) {
	          log.error(e.getMessage());
	          isAvailable = false;
	          return;
	        }
	      }
	      isAvailable = true;
	    }
	  }
}
