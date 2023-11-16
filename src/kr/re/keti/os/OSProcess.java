package kr.re.keti.os;

import java.util.ArrayList;

import kr.re.keti.Ssl;

public abstract class OSProcess {
	public static final ArrayList<String> edgeList = new ArrayList<String>();
	private Ssl ssl = Ssl.getInstance();
	public abstract String getMaster();
	public abstract void start();
	public abstract void stop();
	public static String getEdgeListAsString() {
		String list = edgeList.toString();
		list = list.substring(1, list.length()-1);
		list = list.replace(", ", ":");
		return list;
	}
	public static ArrayList<String> getEdgeListAsArrayList(){
		return edgeList;
	}
	public static void addKey() {
		System.out.println("addKey");
	}
}
