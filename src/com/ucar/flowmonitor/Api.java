package com.ucar.flowmonitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.content.Context;

public class Api {
	
	public static String scriptHeader(Context ctx,int uid) {
		Logger.d( "scriptHeader");
		StringBuilder sb=new StringBuilder("#!/bin/bash\n");
		sb.append("# Filtering rules\n");
		sb.append("iptables -A INPUT -m owner --uid-owner ").append(uid).append(" -j DROP   \n");
		sb.append("iptables -A FORWARD -m owner --uid-owner ").append(uid).append(" -j DROP  \n");
		sb.append("iptables -A OUTPUT -m owner --uid-owner ").append(uid).append(" -j DROP   \n");
		
		sb.append("\b");
		return sb.toString();
	}
	
	public static String getScript(boolean is3G,String ip){
		String str=null;
		String mIp=null;
		String rule=" -d ";
		if(is3G){
//			str="250/sec";
			str="200/sec";
			mIp=getLocalIpAddress(is3G);
		}else{
//			str="200/sec";
			str="160/sec";
			mIp=ip;
		}
		
		StringBuilder sb=new StringBuilder("#!/bin/bash\n");
		sb.append("iptables -A INPUT -m limit "+rule).append(mIp).append(" --limit "+str+" --limit-burst 100 -j ACCEPT\n");
		sb.append("iptables -A INPUT "+rule).append(mIp).append(" -j DROP\n");
		sb.append("iptables -A OUTPUT -m limit "+rule).append(mIp).append(" --limit "+str+" --limit-burst 100 -j ACCEPT\n");
		sb.append("iptables -A OUTPUT "+rule).append(mIp).append(" -j DROP\n");
		sb.append("iptables -A FORWARD -m limit "+rule).append(mIp).append(" --limit "+str+" --limit-burst 200 -j ACCEPT\n");
		sb.append("iptables -A FORWARD "+rule).append(mIp).append(" -j DROP\n");
		
//		iptables -A FORWARD -m limit -d 10.98.82.178 --limit 30/sec -j ACCEPT 
//		iptables -A FORWARD -d 10.98.82.178 -j DROP 
			
		sb.append("\b");
		return sb.toString();
	}
	
	public static String getLocalIpAddress(boolean is3G) {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String str=inetAddress.getHostAddress().toString();
						Logger.d( "str="+str);
						if(str.contains(".")){
							if(is3G){
								if(is3GIp(str,is3G)[0]){
									return str;
								}
							}else{
								if(is3GIp(str,is3G)[1]){
									return str;
								}
							}
						}
					}
				}
			}
		} catch (SocketException ex) {
			Logger.d( "ex="+ex.toString());
		}
		return null;
	}

	private static boolean[] is3GIp(String str,boolean is3G) {
		boolean[] is3g=new boolean[2];
		str.trim();
		String[] strs=str.split("\\.");
		if(strs.length>0){
			int first=Integer.parseInt(strs[0]);
			if(is3G){
				if(0<first&&first<127){
					is3g[0]=true;
				}
			}else{
				if(191<first&&first<224){
					is3g[1]=true;
				}
			}
		}
		return is3g;
	}
	
	public static String getCleanAllRules(){
		StringBuilder sb=new StringBuilder("#!/bin/bash\n");
		sb.append("iptables -F INPUT\n");
		sb.append("iptables -F FORWARD\n");
		sb.append("iptables -F OUTPUT\n");
		sb.append("\b");
		return sb.toString();
	}
	/**
	 * 获取到连接到本机wifi热点的所有设备IP
	 * @return
	 */
	public static ArrayList<String> getConnectedIP() {  
        ArrayList<String> connectedIP = new ArrayList<String>();  
        try {  
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));  
            String line;  
            while ((line = br.readLine()) != null) {  
            	Logger.d(""+ line);
                String[] splitted = line.split(" +");  
                if (splitted != null && splitted.length >= 4) {  
                    String ip = splitted[0]; 
                    if(ip.contains(".")){
                    	connectedIP.add(ip); 
                    	Logger.d( "ip="+ip);
                    }
                }  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        return connectedIP;  
    }  
	
}
