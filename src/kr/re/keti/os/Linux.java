package kr.re.keti.os;

public class Linux implements OSProcess{
	UdpReceptor receptor;
	
	@Override
	public String getMaster() {
		receptor = new UdpReceptor();
		String masterIP = "none";
		masterIP = receptor.getMaster();
		return masterIP;
	}
	@Override
	public void start() {
	}
}
