package kr.re.keti.os;

import java.io.PrintWriter;
import java.net.Socket;

import kr.re.keti.Main;
import kr.re.keti.PortNum;

public class TcpRequest {
	public static void send() {
		try ( Socket socket = new Socket(Main.masterIP, PortNum.DEFAULT_TCP_RECEPTOR_PORT);
		){
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			out.println(Main.deviceIP);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
