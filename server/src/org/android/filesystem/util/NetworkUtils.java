package org.android.filesystem.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class NetworkUtils {
	public static Set<String> getLocalMachineIpAddresses(){
		Set<String> ipAddresses = new HashSet<String>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()){
				NetworkInterface networkInterface = interfaces.nextElement();
				Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
				while(addresses.hasMoreElements()){
					InetAddress address = addresses.nextElement();
					
					if(address instanceof Inet6Address){
						continue;
					}
					
					if(!address.isLoopbackAddress() && !address.isLinkLocalAddress() && !address.isAnyLocalAddress()){
						ipAddresses.add(address.getHostAddress());
					}
				}
			}	
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return ipAddresses;
	}
}
