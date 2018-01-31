import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.openbaton.exceptions.BadFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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

    private void checkRequest(String message) throws BadFormatException {
        log.info("\n\n");
        log.info("Received: " + message);
        PrometheusNotification monitoringNotification;
        try {
            Gson mapper = new GsonBuilder().setPrettyPrinting().create();
            monitoringNotification = mapper.fromJson(message, PrometheusNotification.class);
        } catch (JsonSyntaxException e) {
            throw new BadFormatException(
                    "JsonSyntaxException: Impossible to retrieve the ZabbixNotification received");
        }
        log.debug("\n");
        log.debug("Prometheus Notification: " + monitoringNotification);
        handleNotification(monitoringNotification);
    }

    private String read(InputStream is) throws IOException {
        StringBuilder responseStrBuilder = new StringBuilder();
        try (BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) responseStrBuilder.append(inputStr);
        }
        return responseStrBuilder.toString();
    }

    private void handleNotification(PrometheusNotification notification) {
        //TODO: Implement
        log.debug("NOT IMPLEMENTED YET!");
    }
}