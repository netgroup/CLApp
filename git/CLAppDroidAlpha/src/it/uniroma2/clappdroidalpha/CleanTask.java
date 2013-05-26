package it.uniroma2.clappdroidalpha;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.audioclean.CleaningAlgorithm;

public class CleanTask extends Thread {
	String fileName;
	ArrayList<byte[]> data;
	ServiceTest st;
	
	CleanTask(ArrayList<byte[]> data,String name, ServiceTest st){
		this.data=data;
		fileName=name;
		this.st=st;
		this.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
	}
	
	@Override
	public void run() {
		try {
			byte[] returned=CleaningAlgorithm.cleaner(data, fileName);
			Bundle container=new Bundle();
			container.putByteArray("data", returned);
			Message msg=st.mHandler.obtainMessage(30);
			msg.setData(container);
			msg.sendToTarget();
			Log.i("cleaner","track returned");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

}
