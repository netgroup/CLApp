package it.uniroma2.clappdroidalpha;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

@SuppressLint("HandlerLeak")
/**
 * Class of the application service
 * @author Daniele De Angelis
 *
 */
public class MainService extends Service {
	
	private ThreadListen tl;
	private ThreadSender ts;
	protected AudioRecorder ar;
	public final IBinder loc=new LocalBinder();
	private String name; //File name
	private String pathname; //File path
	
	protected ArrayList<byte[]> dataRecorded; //Data structure for data from the audio recorder
	protected ArrayList<byte[]> toSend; //Data structure to send on the net
	// Data structures for the graph
	protected ArrayList<Integer> times;
	protected ArrayList<Double> RMSE;
	//Lock for the dataRecorded structure
	protected Lock record;
	//Lock for the data cleaning phase
	protected Lock cleaned;
	//Lock for the toSend structure
	protected Lock send;
	//Lock for the bit rate view updates
	protected Lock syncBitRate;
	//bit rate value
	protected int bitRate;
	protected boolean mode; //Boolean to control performance stats saving
	protected boolean ctrlNorm; //Boolean to activate the track normalization
	protected int counter; 
	protected double movingAverageOld=Double.POSITIVE_INFINITY; //Old average for the normalization
	
	public class LocalBinder extends Binder {
        MainService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MainService.this;
        }
    }
	
	/**
	 * Starts the audio recording and all the linked functions
	 * @param name
	 * 		file name
	 * @param pathname
	 * 		file path
	 */
	public void startMainService(String name,String pathname){
		Log.i("Debug service","Button on");
		ctrlNorm=false;
		//Setting up a new audio recorder
		ar=AudioRecorder.getInstance(false);
		this.name=name;
		this.pathname=pathname;
		
		//Initializing all the data structures
		dataRecorded=new ArrayList<byte[]>();
		toSend=new ArrayList<byte[]>();
		RMSE=new ArrayList<Double>();
		times=new ArrayList<Integer>();
		record=new ReentrantLock();
		cleaned=new ReentrantLock();
		send=new ReentrantLock();
		syncBitRate=new ReentrantLock();
		bitRate=0;
		counter=0;
		movingAverageOld=Double.POSITIVE_INFINITY;
		
		//constructing the threads
		ts=new ThreadSender(this);
		tl=new ThreadListen(this, this.pathname+"/"+this.name);
		//Starting the threads
		ts.start();
		tl.start();
		//Starting the audio recorder
		ar.setOutputFile(this.pathname+"/"+this.name);
		ar.prepare();
		ar.start(this);
	}
	
	/**
	 * Stops the audio recording and all the linked functions
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void stopMainService() throws InterruptedException, IOException{
		Log.i("Debug service","Button off");
		//Stopping the audio recorder
		RandomAccessFile output=ar.stop();
		//Stopping the threads
		ThreadListen.stop=true;
		ThreadSender.exitLoop=true;
		if(tl.isAlive()){
			tl.join();
		}
		//Closing the file and resetting the audio recorder
		output.close();
		ar.release();
		ar.reset();
	}
	
	/**
	 * The function is used to reassemble the bytes arrays for the final track save 
	 * @param wind
	 * 		Track clusterized
	 * @return
	 * 		Track not windowed
	 */
	@SuppressWarnings("unused")
	private float[] reformFinalTrack(float[][] wind){
		float[] track;
		int size=0, j=0;
		for(int i=0;i<wind.length;i++){
			size+=wind[i].length;
		}
		track=new float[size];
		j=size-1;
		for(int i=wind.length-1;i>=0;i--){
			for(int t=wind[i].length-1;t>=0;t--){
				track[j]=wind[i][t];
				j--;
			}
			wind[i]=null;
		}
		wind=null;
		return track;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
	    return loc;
	}
	

}
