package it.uniroma2.clappdroidalpha;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class MainService extends Service {
	
	private ThreadListen tl;
	private ThreadSender ts;
	private AudioRecorder ar;
	public final IBinder loc=new LocalBinder();
	private String name;
	private String pathname;
	public Lock lockServ=new ReentrantLock();
	private Context ctx;
	
	public ArrayList<byte[]> dataRecorded;
	public ArrayList<byte[]> dataCleaned;
	public ArrayList<byte[]> toSend;
	public Lock record;
	public Lock cleaned;
	public Lock send;
	
	public Lock syncByteRate;
	public int byteRate;
	
	
	public class LocalBinder extends Binder {
        MainService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MainService.this;
        }
    }
	/*
	@SuppressLint("HandlerLeak")
	public Handler mHandler=new Handler(){
		public void handleMessage(Message msg) {
			
			if(msg.what==30){
				lockReceive.lock();
				cleaned.add(msg.getData().getByteArray("data"));
				Log.i("Debug Service","Cleaned part received");
				//msg.recycle();
				super.handleMessage(msg);
				lockReceive.unlock();
			}
		}
	};
	*/
	public void startServiceTest(Context ctx,String name,String pathname){
		Log.i("Debug service","Button on");
		this.ctx=ctx;
		ar=AudioRecorder.getInstanse(false);
		this.name=name;
		this.pathname=pathname;
		
		dataRecorded=new ArrayList<byte[]>();
		dataCleaned=new ArrayList<byte[]>();
		toSend=new ArrayList<byte[]>();
		record=new ReentrantLock();
		cleaned=new ReentrantLock();
		send=new ReentrantLock();
		
		syncByteRate=new ReentrantLock();
		byteRate=0;
		
		ts=new ThreadSender(this);
		tl=new ThreadListen(this, this.pathname+"/"+this.name);
		ts.start();
		tl.start();
		ar.setOutputFile(this.pathname+"/"+this.name);
		ar.prepare();
		ar.start(this);
	}
	
	public void stopServiceTest() throws InterruptedException, IOException{
		Log.i("Debug service","Button off");
		RandomAccessFile output=ar.stop();
		Toast finish=Toast.makeText(ctx, "Wait completion...", Toast.LENGTH_SHORT);
		ThreadListen.stop=true;
		ThreadSender.exitLoop=true;
		if(tl.isAlive()){
			finish.show();
			while(tl.isAlive()){
				finish.setDuration(Toast.LENGTH_SHORT);
			}
		}
		//lockServ.lock();
		//lockReceive.lock();
		this.cleaned.lock();
		byte[] finalData=reformFinalTrack();
		output.write(finalData);
		this.cleaned.unlock();
		//Wave temp=new Wave(Environment.getExternalStorageDirectory()+"/"+this.pathName);
		//WaveHeader waveHead=temp.getWaveHeader();
		//Wave toSave=new Wave(waveHead,finalData);
		//WaveFileManager wfm=new WaveFileManager();
		//wfm.setWave(toSave);
		//wfm.saveWaveAsFile(pathName);
		//lockReceive.unlock();
		//lockServ.unlock();	
		//output.seek(4); // Write size to RIFF header
		//output.writeInt(Integer.reverseBytes(36+tl.payloadSize));
	
		//output.seek(40); // Write size to Subchunk2Size field
		//output.writeInt(Integer.reverseBytes(tl.payloadSize));
		output.close();
		
		ar.release();
		ar.reset();
	}
	
	//The function is used to reassemble the bytes arrays for the final track save 
	public byte[] reformFinalTrack(){
		byte[] track;
		int size=0, j=0;
		for(int i=0;i<dataCleaned.size();i++){
			size+=dataCleaned.get(i).length;
		}
		track=new byte[size];
		for(int i=0;i<dataCleaned.size();i++){
			for(int t=0;t<dataCleaned.get(i).length;t++){
				track[j]=dataCleaned.get(i)[t];
				j++;
			}
		}
		return track;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
	    return loc;
	}
	

}
