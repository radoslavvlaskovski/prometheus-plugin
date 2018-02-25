package prometheus.api;

public class PrometheusApiManager {
	
	// write the alarm to alert.rules
	public void createTrigger(String expression) {
	    String pathToAlertRules = "./openbaton-compose/prometheus/alert.rules";
	    String severity = "page";
	}
}
