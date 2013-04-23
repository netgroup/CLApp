package com.CLApp;

import java.io.File;
import java.io.IOException;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class regService extends Service{
	
	//private FileOutputStream fos;
	public File tmp;
	public String fileName;
	private ExtAudioRecorder ar;
	private Intent i;
	
	public void onCreate(){
		super.onCreate();
		Log.i("reg", "Service created");
		//i=new Intent(this,BroadcastSender.class);
	}
	
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		Log.i("reg", "Service started");
		String take=intent.getStringExtra("fileName");
		fileName=new String("/sdcard/"+take+".wav");
		//i.putExtra("fileName", fileName);
		try {
			startRegistration();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void startRegistration() throws IllegalStateException, IOException{
		ar=ExtAudioRecorder.getInstanse(false);
		//ar.reset();
		ar.setOutputFile(fileName);
		ar.prepare();
		//ar.reset();
		ar.start();		
	}
	
	public void onDestroy(){
		ar.stop();
		ar.release();
		
		//this.stopSelf();
		Log.i("reg", "Registration stopped");
		super.onDestroy();
	}
}
