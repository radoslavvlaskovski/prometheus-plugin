package org.openbaton.monitoring.agent;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openbaton.catalogue.mano.common.monitoring.AbstractVirtualizedResourceAlarm;
import org.openbaton.catalogue.mano.common.monitoring.Alarm;
import org.openbaton.catalogue.mano.common.monitoring.AlarmEndpoint;
import org.openbaton.catalogue.mano.common.monitoring.ObjectSelection;
import org.openbaton.catalogue.mano.common.monitoring.PerceivedSeverity;
import org.openbaton.catalogue.mano.common.monitoring.ThresholdDetails;
import org.openbaton.catalogue.mano.common.monitoring.ThresholdType;
import org.openbaton.catalogue.nfvo.EndpointType;
import org.openbaton.catalogue.nfvo.Item;
import org.openbaton.exceptions.MonitoringException;
import org.openbaton.monitoring.agent.ZabbixMonitoringAgent.MyHandler;
import org.openbaton.monitoring.agent.performance.management.catalogue.PmJob;
import org.openbaton.monitoring.agent.performance.management.catalogue.Threshold;
import org.openbaton.monitoring.agent.zabbix.api.ZabbixApiManager;
import org.openbaton.monitoring.interfaces.MonitoringPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;

public class PrometheusMonitoringAgent extends MonitoringPlugin {
	
	private int historyLength;
	private int requestFrequency;
	private String prometheusPluginIp;
	private PrometheusSender prometheusSender;
//	private ZabbixApiManager zabbixApiManager;
	private String notificationReceiverServerContext;
	private int notificationReceiverServerPort;
	private Gson mapper;
	private Random random = new Random();
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private List<AlarmEndpoint> subscriptions;
	private Map<String, PmJob> pmJobs;
	private Map<String, Threshold> thresholds;
	private Map<String, List<Alarm>> datacenterAlarms;
	private String type;
	private Map<String, List<String>> triggerIdHostnames;
	private Map<String, String> triggerIdActionIdMap;
	private LimitedQueue<State> history;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	  //Server properties
	private HttpServer server;	
	private MyHandler myHandler;
	
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
	public List<Alarm> getAlarmList(String vnfId, PerceivedSeverity perceivedSeverity) throws MonitoringException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createPMJob(ObjectSelection resourceSelector, List<String> performanceMetric,
			List<String> performanceMetricGroup, Integer collectionPeriod, Integer reportingPeriod)
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
		// TODO Auto-generated method stub
		return null;
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
	public String createThreshold(ObjectSelection objectSelector, String performanceMetric, ThresholdType thresholdType,
			ThresholdDetails thresholdDetails) throws MonitoringException {
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
