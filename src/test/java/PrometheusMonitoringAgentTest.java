import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openbaton.exceptions.MonitoringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PrometheusMonitoringAgentTest {
	
	PrometheusMonitoringAgent prometheusMonitoringAgent = null;
	Logger log = null;
	List<String> hosts = null;
	List<String> metrics = null;
	
	@Before
	public void setUp() {
		log = LoggerFactory.getLogger(this.getClass());
		hosts = new ArrayList<>();
		hosts.add("localhost:9090");
		metrics = new ArrayList<>();
		metrics.add("http_requests_total");
	}
	
	@Test
	public void testQueryPMJob() {
		prometheusMonitoringAgent = new PrometheusMonitoringAgent();
		try {
			log.info(prometheusMonitoringAgent.queryPMJob(hosts, metrics, "1").toString());
		} catch(MonitoringException e) {
			org.junit.Assert.fail();
		}
	}
	
	@Ignore
	@Test
	public void testRangeQueryJob() {
		prometheusMonitoringAgent = new PrometheusMonitoringAgent();
		try {
			log.info(prometheusMonitoringAgent.rangeQueryJob(hosts, metrics, "30").toString());
		} catch(MonitoringException e) {
			org.junit.Assert.fail();
		}
	}
}
