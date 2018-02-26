import java.io.FileNotFoundException;

public class Starter2 {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		PrometheusSender ps = new PrometheusSender(null, null, null, false);
		ps.addTarget("prometheus.yml", "node", "10.147.65.145", 9100);
	}

}
