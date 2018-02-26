public class PrometheusJob {

    /*
    Example for request: '{"jobName":"node", "ip":"10.120.30.10", "port":"9100"}'
     */

    private String jobName;

    private String ip;

    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
