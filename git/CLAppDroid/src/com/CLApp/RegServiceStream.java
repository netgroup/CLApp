package com.CLApp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class RegServiceStream extends Service{
	
	//TODO have to decide of their future
	public File tmp;
	public String fileName;
	//Object custom recorder
	private ExtAudioRecorderForStream ar;
	//Pipe to send the data to the Sending thread
	private ArrayBlockingQueue<Byte> pipeTX;
	//Pipe to send the data to the Listening thread
	private ArrayBlockingQueue<Byte> pipeIN;
	//Thread for the broadcast sending
	private BroadcastSenderStream bs;
	//Thread for the broadcast receiving
	private ListeningStream server;
	
	//Creation of the service and initialization of the pipes
	@Override
	public void onCreate(){
		super.onCreate();
		Log.i("reg", "Service created");
		pipeTX=new ArrayBlockingQueue<Byte>(1000);
		pipeIN=new ArrayBlockingQueue<Byte>(1000000);
		//i=new Intent(this,BroadcastSender.class);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		//on starting the string in the intent extras is recovered
		super.onStartCommand(intent, flags, startId);
		Log.i("reg", "Service started");
		String take=intent.getStringExtra("fileName");
		fileName=new String(Environment.getExternalStorageDirectory().getPath()+"/"+take+".wav");
		//i.putExtra("fileName", fileName);
		
		//the pipes are initialized and the function startRegistration is called
		bs=new BroadcastSenderStream(pipeTX);
		server=new ListeningStream(pipeIN,fileName);
		try {
			startRegistration();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
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
	
	//This functions sets the recorder and starts all the thread for the sending and receiving
	public void startRegistration() throws IllegalStateException, IOException, InterruptedException{
		//MainActivity.lock.acquire();
		//The recorder is set to record wave file uncompressed
		ar=ExtAudioRecorderForStream.getInstanse(false);
		//Broadcast sender started
		bs.start();
		//Server in listen started
		server.start();
		ar.setOutputFile(fileName);
		ar.prepare();
		//Recording started
		ar.start(pipeTX,pipeIN);		
	}
	
	//On closure the recording is interrupted as the threads created by him
	@Override
	public void onDestroy(){
		//The stopIt function are usefull to interrupt the while(true) in the run function of the threads
		bs.interrupt();
		server.interrupt();
		try {
			//Waiting the threads destructions
			bs.join();
			server.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//The recording is stopped and the mic is released
		ar.stop();
		ar.release();
		
		//this.stopSelf();
		Log.i("reg", "Registration stopped");
		//MainActivity.lock.release();
		super.onDestroy();
	}
}
