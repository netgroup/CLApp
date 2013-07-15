package it.uniroma2.clappdroidalpha.tools;

import android.util.Log;
/**
 * Class containing the function to start the Wifi ad-hoc
 * 
 */
public class IFManager {
	private static String LOGTAG="IFManager";
	
	/**
	 * Starts the wifi ad-hoc
	 */
	public static void startWifiAdHoc() {
		String command = "modprobe dhd\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
		String[] ctrl;
		
		String wifiInterface = Utils.getWifiInterface();
		String response="";
		// Load wifi driver
		while(response.equals("")||response.contains("Starting wifi... Error")){
			command = BaseSettings.APPDIR+"wifiloader start\n";
			Log.i(LOGTAG, command);
			ctrl=Utils.rootExec(command);
			response=ctrl[0];
		}
		
		// Set wifi ad-hoc
		command = BaseSettings.APPDIR+"iwconfig "+wifiInterface+" mode ad-hoc essid "+BaseSettings.WIFI_ESSID+" channel "+BaseSettings.WIFI_CHANNEL+" commit\n";
		Log.i(LOGTAG, command);
		ctrl=Utils.rootExec(command);
		
		// Wait for interface to be ready
		Log.i(LOGTAG, "Waiting for interface to be ready...");
		try { Thread.sleep(5000); } catch(Exception e) {}
		
		// Set IP address
		// NOT NEEDED FOR IPv6
		String address = Utils.getLinkLocalAddress();
		command = BaseSettings.APPDIR+"ifconfig "+wifiInterface+" "+address+" netmask 255.255.0.0 up\n";
		Log.i(LOGTAG, command);
		ctrl=Utils.rootExec(command);
	}
	
	/**
	 * Stops the wifi ad-hoc
	 */
	public static void stopWifiAdHoc() {
		String command = BaseSettings.APPDIR+"ifconfig eth0 down\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
		
		//command = BaseSettings.APPDIR+"wifiloader stop\n";
		command = "rmmod dhd\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
	}
}
