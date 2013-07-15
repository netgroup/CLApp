package it.uniroma2.clappdroidalpha.tools;

import java.io.*;
import java.net.*;
import java.util.*;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.*;

/**
 * Some utility functions
 * 
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class Utils {
	private final static String LOGTAG = "Utils";
	private static String WIFI_INTERFACE_NAME = null;
	private static Boolean ARMV7_PROCESSOR = null;
	
	/**
	 * Executes a command with root permissions
	 * @param command	The command to be executed
	 * @return			The command output
	 */
	@SuppressWarnings("deprecation")
	public static String[] rootExec(String command) {
		Process process = null;
		DataInputStream input = null;
		DataOutputStream output = null;
		String outputResponse = null;
		try {
			if(Build.VERSION.SDK_INT < 11) {
				process = Runtime.getRuntime().exec("su");
			} else {
				String[] cmd = {"su", "-c", "/system/bin/sh"};
				process = Runtime.getRuntime().exec(cmd);
			}
			input = new DataInputStream(process.getInputStream());
			output = new DataOutputStream(process.getOutputStream());
			output.writeBytes(command);
			output.writeBytes("exit\n");
			output.flush();
			process.waitFor();

			Vector<String> res = new Vector<String>();
			while ((outputResponse = input.readLine()) != null) {
				Log.d(LOGTAG, "Command: "+command+" - Response: "+outputResponse);
				res.add(outputResponse);
			}
			return res.toArray(new String[0]);
		} catch (Exception e) {
			Log.e(LOGTAG, "rootExecGB()", e);
		} finally {
			try {
				if(output!=null) output.close();
				if(input!=null) input.close();
				if(process!=null) process.destroy();
			} catch (Exception e) {
				Log.e(LOGTAG, "rootExecGB()", e);
			}
		}
		return null;
	}
	
	public static InetAddress localAddressNotLo() throws SocketException {
	    System.setProperty("java.net.preferIPv4Stack", "true");
	    
	    for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
	        NetworkInterface ni = niEnum.nextElement();
	        if (!ni.isLoopback()) {
	            for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
	            	return interfaceAddress.getAddress();
	            }
	        }
	    }
	    return null;
	}
	
	
	
	/**
	 * Gets the wifi interface name
	 * @return	The wifi interface
	 */
	public static String getWifiInterface() {
		// The interface name is cached for future requests
		if(WIFI_INTERFACE_NAME==null) {
			String[] out = rootExec("getprop wifi.interface\n");
			if(out.length>0 && out[0]!=null && out[0]!="") {
				WIFI_INTERFACE_NAME = out[0];
				Log.i(LOGTAG, "Found Wifi interface: "+out[0]);
				return out[0];
			}
		} else {
			return WIFI_INTERFACE_NAME;
		}
		
		// Else, property wifi.interface was not set. So I guess it's eth0.
		WIFI_INTERFACE_NAME = "eth0";
		return WIFI_INTERFACE_NAME;
	}
	
	
	/**
	 * Gets a new link-local IPv4 address
	 * @return	Found free IPv4 address
	 */
	public static String getLinkLocalAddress() {
		int numTests = 2;
		
		Random random = new Random(System.nanoTime());
		boolean ok = false;
		while(!ok) {
			int a = random.nextInt(253)+1;
			int b = random.nextInt(253)+1;
			String newAddress = "169.254."+a+"."+b;
			Log.i("getLinkLocalAddress", "Trying address: "+newAddress);
			
			String res[] = rootExec(BaseSettings.APPDIR+"busybox arping -I "+getWifiInterface()+" -D -c "+numTests+" "+newAddress+" \n");
			for(String s : res) {
				if(s.startsWith("Received 0")) {
					ok = true;
					Log.i("getLinkLocalAddress", "Free address found: "+newAddress);
					return newAddress;
				}
			}
		}
		return null;
	}
	
	/**
	 * Checks if the current device has an ARMv7 processor. Otherwise it's supposed there's an ARMv6.
	 * @return		True if device has an ARMv7 processor, false otherwise
	 */
	public static boolean processorIsARMv7() {
		// The information is cached for future requests		
		if(ARMV7_PROCESSOR==null) {
			String cpuinfo[] = Utils.rootExec("cat /proc/cpuinfo\n");
			for(String info : cpuinfo) {
				if(info.contains("ARMv7")) {
					Log.i("LOGTAG", "ARMv7 processor found");
					ARMV7_PROCESSOR = true;
					return ARMV7_PROCESSOR;
				}
			}
		} else {
			return ARMV7_PROCESSOR;
		}
		ARMV7_PROCESSOR = false;
		return ARMV7_PROCESSOR;
	}
}
