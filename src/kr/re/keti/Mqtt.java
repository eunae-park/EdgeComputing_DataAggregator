package kr.re.keti;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Mqtt implements MqttCallback{
	private MqttClient client;
	private MqttConnectOptions options;
	private String deviceIp;
	
	//-------------------------------- init --------------------------------------------
	public Mqtt(String masterIp, String deviceIp) {
		String addr = "tcp://"+masterIp+":1883";
		this.deviceIp = deviceIp;
		if (masterIp.equals(deviceIp)) {
			publisher(addr, deviceIp);
		}
		else {
			subscriber(addr, deviceIp);
		}
		
	}
	public boolean isConnected() {
		return client.isConnected();
	}
	// master
	private void publisher(String masterIp, String deviceIp) {
		try {
			client = new MqttClient(masterIp, deviceIp);
			client.connect();
		} catch (MqttException e) {
		}
	}
	// slave
	private void subscriber(String masterIp, String deviceIp) {
		try {
			options = new MqttConnectOptions();
			options.setCleanSession(true);
			options.setKeepAliveInterval(30);
			client = new MqttClient(masterIp, deviceIp);
			client.setCallback(this);
			client.connect(options);
            client.subscribe("/", 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	//----------------------------------------------------------------------------------

    //메세지 전송을 위한 메소드 master
    public boolean send(String ip, String type, String data){
        try {
        	String remote_cmd = "{[{REQ::"+ip+"::"+type+"::"+data+"}]}";
            //broker로 전송할 메세지 생성 -MqttMessage
            MqttMessage message = new MqttMessage();
            message.setPayload(remote_cmd.getBytes()); //실제 broker로 전송할 메세지
            client.publish("/",message);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean send(String originalData){
        try {
        	String remote_cmd = originalData;
            //broker로 전송할 메세지 생성 -MqttMessage
            MqttMessage message = new MqttMessage();
            message.setPayload(remote_cmd.getBytes()); //실제 broker로 전송할 메세지
            client.publish("/",message);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
   
    
	// 서버와의 연결이 끊어졌을 때
	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	// 메시지 전달이 완료
	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	// 메서드는 서버에서 메시지가 도착
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("=====================메세지 도착=================");
        System.out.println(message);
        String m = message+"";
        FileMonitor.lastCmd = m;
        m = m.substring(m.indexOf("{[{"), m.lastIndexOf("}]}"));
        String[] array = m.split("::");
        String requestIp = array[1];
        String code = array[2];
        String fileName = array[3];
        if(!requestIp.equals(deviceIp)) {
			try {
				EdgeDataAggregator.fileMonitor.stop();
			} catch (Exception e1) {
//				e1.printStackTrace();
			}
			
        	switch(code) {
	        	case "013":
	        		DataProcess dataProcess = SlaveWorker.dataprocess;
	        		dataProcess.IndividualDataRemove(fileName.split("\\.")[0]);
	        		break;
        	}
        	
			try {
				EdgeDataAggregator.fileMonitor.start();
			} catch (Exception e1) {
//				e1.printStackTrace();
			}
        }
		
	}
}
