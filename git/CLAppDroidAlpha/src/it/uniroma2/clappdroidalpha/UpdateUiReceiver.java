package it.uniroma2.clappdroidalpha;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class UpdateUiReceiver extends BroadcastReceiver{ 
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// Get extra data included in the Intent
    	Bundle received=arg1.getBundleExtra("Envelope");
    	@SuppressWarnings("unchecked")
		HashMap<InetAddress,ArrayList<Integer>> neighbours=(HashMap<InetAddress, ArrayList<Integer>>) received.getSerializable("Addresses");
    	int bitRate=received.getInt("BitRate");
    	MainActivity.current.stampAddressList(neighbours);
		MainActivity.current.updateBitRate(bitRate);
		MainActivity.current.updateGraph();
		try {
			MainActivity.current.performanceInfo();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	Log.d("Debug receiver","Broadcast received");
	}

}
