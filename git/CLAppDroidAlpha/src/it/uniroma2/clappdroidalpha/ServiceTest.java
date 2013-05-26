package it.uniroma2.clappdroidalpha;


import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.musicg.wave.Wave;
import com.musicg.wave.WaveFileManager;
import com.musicg.wave.WaveHeader;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

@SuppressLint("HandlerLeak")
public class ServiceTest extends Service {
	
	private ThreadListen tl;
	private ThreadSender ts;
	private AudioRecorder ar;
	public final IBinder loc=new LocalBinder();
	private ArrayList<byte[]> cleaned;
	private String pathName;
	public Lock lockServ=new ReentrantLock();
	private Lock lockReceive=new ReentrantLock();
	
	
	public class LocalBinder extends Binder {
        ServiceTest getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServiceTest.this;
        }
    }
	
	@SuppressLint("HandlerLeak")
	public Handler mHandler=new Handler(){
		public void handleMessage(Message msg) {
			
			if(msg.what==30){
				lockReceive.lock();
				cleaned.add(msg.getData().getByteArray("data"));
				Log.i("Debug Service","Cleaned part received");
				lockReceive.unlock();
			}
		}
	};
	
	public void startServiceTest(String pathName){
		Log.i("Debug service","Button on");
		new ArrayBlockingQueue<Byte>(5000);
		new ArrayBlockingQueue<Byte>(5000);
		ar=AudioRecorder.getInstanse(false);
		this.pathName=pathName;
		cleaned=new ArrayList<byte[]>();
		ar.setOutputFile(Environment.getExternalStorageDirectory()+"/"+this.pathName);
		ar.prepare();
		ts=new ThreadSender();
		tl=new ThreadListen("Receiver", this, this.pathName);
		ts.start();
		tl.start();
		ar.start(ts, tl);
	}
	
	public void stopServiceTest() throws InterruptedException{
		Log.i("Debug service","Button off");
		ar.stop();
		ar.release();
		ar.reset();
		ThreadListen.stop=true;
		/*while(tl.isAlive()){
			finish.setDuration(Toast.LENGTH_SHORT);
		}*/
		lockServ.lock();
		lockReceive.lock();
		byte[] finalData=reformFinalTrack();
		Wave temp=new Wave(Environment.getExternalStorageDirectory()+"/"+this.pathName);
		WaveHeader waveHead=temp.getWaveHeader();
		Wave toSave=new Wave(waveHead,finalData);
		WaveFileManager wfm=new WaveFileManager();
		wfm.setWave(toSave);
		wfm.saveWaveAsFile(pathName);
		lockReceive.unlock();
		lockServ.unlock();			
	}
	
	//The function is used to reassemble the bytes arrays for the final track save 
	public byte[] reformFinalTrack(){
		byte[] track;
		int size=0, j=0;
		for(int i=0;i<cleaned.size();i++){
			size+=cleaned.get(i).length;
		}
		track=new byte[size];
		for(int i=0;i<cleaned.size();i++){
			for(int t=0;t<cleaned.get(i).length;t++){
				track[j]=cleaned.get(i)[t];
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
