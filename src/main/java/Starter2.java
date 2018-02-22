
public class Starter2 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PrometheusSender ps = new PrometheusSender(null, null, null, false);
		ps.addJob("prometheus.yml", "test", 4, null, null, 7060, false);
		ps.addJob("prometheus.yml", "test2", 3, null, null, 7070, true);
	}

}
