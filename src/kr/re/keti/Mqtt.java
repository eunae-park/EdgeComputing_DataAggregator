package kr.re.keti;


import java.io.FileOutputStream;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Mqtt implements MqttCallback{
	private MqttClient client;
	private MqttConnectOptions options;
	private String masterIp;
	private String deviceIp;
	
	//-------------------------------- init --------------------------------------------
	public Mqtt(String masterIp, String deviceIp) {
		String addr = "tcp://"+masterIp+":1883";
		this.masterIp = masterIp;
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
    public boolean publish(String ip, String type, String data){
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
    public void send(String message) {
    	String[] messages = message.substring(0, message.indexOf("}]}")).split("::");
    	String ip = messages[1];
    	String type = messages[2];
    	String data = messages[3];
    	
    	send(ip, type, data);
    }
    public void send(String ip, String type, String data) {
		if (masterIp.equals(deviceIp)) {
			publish(ip, type, data);
		}
		else {
			SlaveWorker.dataprocess.RequestMqtt(masterIp, deviceIp, type, data);
		}
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
        System.out.println("===================== receive message =====================");
        String m = message+"";
        FileMonitor.lastCmd = m;
        m = m.substring(m.indexOf("{[{"), m.lastIndexOf("}]}"));
        String[] array = m.split("::");
        String requestIp = array[1];
        String code = array[2];
        String data = array[3];
        // message print
    	if(code.equals("000")) {
    		System.out.println("new client "+requestIp);
    	}
    	else if(!requestIp.equals(deviceIp)) {
            System.out.println(message);
    	}
        else if(requestIp.equals(deviceIp)) return;
        
        // logic
		try {
			EdgeDataAggregator.fileMonitor.stop();
		} catch (Exception e1) {
//				e1.printStackTrace();
		}
    	switch(code) {
        	case "000":
        		String uuid = array[3].split(",")[0];
        		String file = array[3].split(",")[1];
        		keyDownload(uuid, file);
        		break;
        	case "013":
        		DataProcess dataProcess = SlaveWorker.dataprocess;
        		dataProcess.IndividualDataRemove(data.split("\\.")[0]);
        		break;
    	}
    	
		try {
			EdgeDataAggregator.fileMonitor.start();
		} catch (Exception e1) {
//				e1.printStackTrace();
		}
	}
	public static void keyDownload(String uuid, String dataStr) {
		byte[] data = dataStr.getBytes();
		String certPath = EdgeDataAggregator.cert_folder;
		String keyPath = certPath+"key/"+uuid+".key";
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(keyPath);
			for(int i=0; i<data.length; i++) {
				fileOutputStream.write(data[i]);
			}
			fileOutputStream.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
