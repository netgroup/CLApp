package it.uniroma2.clappdroidalpha;

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
    	//String message = arg1.getStringExtra("message");
		Bundle received=arg1.getBundleExtra("Envelope");
    	@SuppressWarnings("unchecked")
		HashMap<InetAddress,ArrayList<Integer>> neighbours=(HashMap<InetAddress, ArrayList<Integer>>) received.getSerializable("Addresses");
    	int byteRate=received.getInt("ByteRate");
		MainActivity.current.stampAddressList(neighbours);
		MainActivity.current.updateByteRate(byteRate);
    	Log.d("Debug receiver","Broadcast received");
	}

}
