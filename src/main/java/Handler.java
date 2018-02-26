import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.openbaton.exceptions.BadFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rados on 1/21/2018.
 */
class Handler implements HttpHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handle(HttpExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            String message = read(is);
            checkRequest(message);
            t.sendResponseHeaders(200, 0);
            try (OutputStream os = t.getResponseBody()) {
                os.close();
            }
        } catch (Exception e) {
            t.sendResponseHeaders(500, 0);
            try (OutputStream os = t.getResponseBody()) {
                os.close();
            }
            log.error(e.getMessage(), e);
        }
    }

    private void checkRequest(String message) throws Exception {
        log.info("\n\n");
        log.info("Received: " + message);
        PrometheusJob prometheusJob;
        try {
            Gson mapper = new GsonBuilder().setPrettyPrinting().create();
            prometheusJob = mapper.fromJson(message, PrometheusJob.class);
        } catch (JsonSyntaxException e) {
            throw new BadFormatException(
                    "JsonSyntaxException: Impossible to retrieve the ZabbixNotification received");
        }
        log.debug("\n");
        log.debug("Prometheus Notification: " + prometheusJob);
        handleJob(prometheusJob);
    }

    private String read(InputStream is) throws IOException {
        StringBuilder responseStrBuilder = new StringBuilder();
        try (BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) responseStrBuilder.append(inputStr);
        }
        return responseStrBuilder.toString();
    }

    private void handleJob(PrometheusJob prometheusJob) throws Exception {
        String pathToFile = "prometheus.yml";
        try {
            InputStream input = new FileInputStream(new File(pathToFile));
            Yaml yaml = new Yaml();
            Map<String, Object> globalMap = (Map<String, Object>) yaml.load(input);
            input.close();
            ArrayList<Map<String, Object>> jobs = (ArrayList<Map<String, Object>>) globalMap.get("scrape_configs");

            for (Map<String, Object> scrapeJob : jobs) {
                if (scrapeJob.containsKey("job_name")){
                    if(scrapeJob.get("job_name").equals(prometheusJob.getJobName())){
                        if(scrapeJob.containsKey("static_configs")){
                            ArrayList<Map<String, Object>> configs = (ArrayList<Map<String, Object>>) scrapeJob.get("static_configs");
                            for(Map<String, Object> config : configs){
                                if(config.containsKey("targets")){
                                    ArrayList<String> targets = (ArrayList<String>) config.get("targets");
                                    targets.add(prometheusJob.getIp() + ":" + prometheusJob.getPort());
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
            throw new Exception(e);
        }
    }

    private void handleNotification(PrometheusNotification notification) {
        //TODO: Implement
        log.debug("NOT IMPLEMENTED YET!");
    }
}