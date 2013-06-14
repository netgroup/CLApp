package it.uniroma2.clappdroidalpha;

import it.uniroma2.audioclean.CleaningAlgorithm;

import java.io.IOException;
import java.util.ArrayList;
import android.util.Log;


public class CleanTask extends Thread {
	String fileName;
	ArrayList<byte[]> data;
	MainService st;
	
	CleanTask(ArrayList<byte[]> data,String name, MainService st){
		this.data=data;
		fileName=name;
		this.st=st;
		this.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
	}
	
	@Override
	public void run() {
		try {
			if(!data.isEmpty()){
				//lockClean.lock();
				byte[] returned=CleaningAlgorithm.cleaner(data, fileName);
				/*
				Bundle container=new Bundle();
				container.putByteArray("data", returned);
				Message msg=st.mHandler.obtainMessage(30);
				msg.setData(container);
				msg.sendToTarget();
				lockClean.unlock();
				*/
				st.cleaned.lock();
				st.dataCleaned.add(returned);
				st.cleaned.unlock();
				
				Log.i("cleaner","track returned");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

}
