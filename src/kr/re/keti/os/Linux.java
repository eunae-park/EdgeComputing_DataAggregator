package kr.re.keti.os;


public class Linux extends OSProcess{
	UdpReceptor receptor;
	
	@Override
	public String getMaster() {
		receptor = new UdpReceptor(edgeList);
		String masterIP = "none";
		masterIP = receptor.getMaster();
		return masterIP;
	}
	@Override
	public void start() {
		receptor.start();
	}

	@Override
	public void stop() {
		receptor.stop();
	}
}
