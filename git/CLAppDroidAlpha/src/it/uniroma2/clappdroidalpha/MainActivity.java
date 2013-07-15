package it.uniroma2.clappdroidalpha;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import it.uniroma2.clappdroidalpha.MainService.LocalBinder;
import it.uniroma2.clappdroidalpha.tools.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class that manages the UI of the application
 * @author Daniele De Angelis
 *
 */
public class MainActivity extends Activity {
	
	MainService mService=null; //Storages the service running from that activity
	boolean mBound = false;   // Boolean that manages the binding of the service
	private boolean clicked=false;//Boolean used to check if the record is already started
	private WakeLock wl; //WakeLock used during the recording
	private String pathDirectory; //Path of saves
	public LocalBroadcastManager lbm; //Manager for the broadcast receiver
	public static MainActivity current; //This activity
	private UpdateUiReceiver receiver; //BroadcastReceiver
	private InetAddress ipAddress; //Actual IP
	private double movingAvg; //MovingAvg
	private long appStartTime; //App starting time
	
	private int pid; //Actual process pid
	//Variable used by the graph
	GraphViewSeries gvs=null; 
	BarGraphView lgv;
	boolean resetGraph;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Copies required executables into the application directory
		copyFileFromAssets("iptables", false);
		copyFileFromAssets("wifiloader", false);
		copyFileFromAssets("iwconfig", false);
		copyFileFromAssets("ifconfig", true);
		copyFileFromAssets("ip", false);
		copyFileFromAssets("busybox", false);
		appStartTime=System.currentTimeMillis();
		
		//Initializes graph
		lgv=new BarGraphView(this, "RMSE");
		lgv.setScrollable(true);
		lgv.setScalable(true);
		lgv.setViewPort(0, 10);
		lgv.setBackgroundColor(Color.BLACK);
		LinearLayout ll = (LinearLayout) findViewById(R.id.linlay);
		ll.setBackgroundColor(Color.BLACK);
		CheckBox cb = (CheckBox) findViewById(R.id.checkBox1);
		cb.setTextColor(Color.WHITE);
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.List);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
		lp.addRule(RelativeLayout.BELOW, R.id.bitRate);
		lp.addRule(RelativeLayout.LEFT_OF, R.id.listOfSender);
		rl.addView(lgv,lp);
		resetGraph=false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onStart(){
		super.onStart();
		current=this;
		//Starting Wifi
		IFManager.startWifiAdHoc();
		try {
			//Memorizing the ip
			ipAddress=Utils.localAddressNotLo();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		//Binding the service
		Intent serv=new Intent(this,MainService.class);
		bindService(serv,mConnection,Context.BIND_AUTO_CREATE);
		//Create the wakelock
		PowerManager pm=(PowerManager) this.getSystemService(Context.POWER_SERVICE);
		wl=pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "ClappDroid");
		//Check if the output directories exist, otherwise they're created
		File f = new File(Environment.getExternalStorageDirectory() + "/WavFiles");
		if(!f.exists()) {
			f.mkdir();
		}
		
		File f2 = new File(Environment.getExternalStorageDirectory() + "/CLAppInfo");
		if(!f2.exists()) {
			f2.mkdir();
		}
		
		pathDirectory=f.getAbsolutePath();
		
		//Registering the broadcast receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,new IntentFilter("prova_broadcast_sender"));
		
		TextView ipAddr=(TextView) findViewById(R.id.ipAddr);
		ipAddr.setText(ipAddress.getHostAddress());
		pid= android.os.Process.myPid();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}
	
	@Override
	protected void onStop(){
		//Service unbinding
		if(mBound){
			unbindService(mConnection);
			mBound=false;
		}
		//Wifi ad-hoc stopped
		IFManager.stopWifiAdHoc();
		//Broadcast receiver unregistered
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    	super.onStop();
		
	}
	
	/**
	 * Function executed when the button start is pressed
	 * @param view
	 */
	public void startServ(View view){
		//Not already pressed before
		if(!clicked){
			//Wake lock acquired
			wl.acquire();
			clicked=true;
			movingAvg=-1;
			//Resetting all the UI views
			EditText et=(EditText) this.findViewById(R.id.editText1);
			TextView listAddr=(TextView) findViewById(R.id.listOfSender);
			TextView bRate=(TextView) findViewById(R.id.listOfSender);
			//Recovering the file name from the textview
			String name=et.getText().toString();
			if(name.equals("")){
				name=System.currentTimeMillis()+".wav";
			}
			else{
				File f=new File(pathDirectory+"/"+name+".wav");
				boolean ctrl=true;
				int i=1;
				String temporary=name;
				while(ctrl){
					if(f.exists()){
						temporary=name+"("+i+")";
						i++;
						f=new File(pathDirectory+"/"+temporary+".wav");
					}
					else
						ctrl=false;
				}
				name=temporary+".wav";
				
			}
			listAddr.setText("");
			bRate.setText("");
			if(gvs!=null){
			}
			//Checking if the performance statistics have to be saved
			CheckBox cb=(CheckBox) findViewById(R.id.checkBox1);
			mService.mode=cb.isChecked();
			if(mService.mode){
				PrintWriter writer = null;
				try {
					writer = new PrintWriter(new FileOutputStream(Environment.getExternalStorageDirectory() + "/CLAppInfo/cpuStat"));
					writer.print("");
				} catch (FileNotFoundException e) {
				}
				try {
					writer = new PrintWriter(new FileOutputStream(Environment.getExternalStorageDirectory() + "/CLAppInfo/memStat"));
					writer.print("");
				} catch (FileNotFoundException e) {
				}
				try {
					writer = new PrintWriter(new FileOutputStream(Environment.getExternalStorageDirectory() + "/CLAppInfo/timeInterv"));
					writer.print("");
				} catch (FileNotFoundException e) {
				}
				
				writer.close();
			}
			//Starting service
			mService.startMainService(name,pathDirectory);
			Log.i("Debug activity","Button on");
		}
	}
	
	/**
	 * Function executed when the stop button is pressed
	 * @param view
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void stopServ(View view) throws InterruptedException, IOException{
		//Checking if start button is already pressed
		if(clicked){
			//Wake lock released
			wl.release();
			clicked=false;
			//Stopping service
			mService.stopMainService();
			Log.i("Debug activity","Button off");
			TextView panel=(TextView) findViewById(R.id.listOfSender);
			panel.setText(R.string.listText);
			panel = (TextView) findViewById(R.id.bitRate);
			panel.setText(R.string.ratetext);
			Toast finish=Toast.makeText(this.getApplicationContext(), "Done!", Toast.LENGTH_SHORT);
			finish.show();
			resetGraph=true;
		}
	}
	
	/** Defines callbacks for service binding, passed to bindService() 
	 * 
	 *
	 */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    /**
     * Function that copies the file specified from the asset to the external memory
     * @param name
     * @param differentProcessorExecutables
     */
    private void copyFileFromAssets(String name, boolean differentProcessorExecutables) {
		AssetManager assetManager = getAssets();
		InputStream in = null;
		OutputStream out = null;
		try {
			if(differentProcessorExecutables) {
				if(Utils.processorIsARMv7()) {
					in = assetManager.open(name+"-armv7");
				} else {
					in = assetManager.open(name+"-armv6");
				}
			} else {
				in = assetManager.open(name);
			}
			out = new FileOutputStream(BaseSettings.APPDIR+name);
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch (Exception e) {
			Log.e("asset setup", e.getMessage());
		}

		// Changes file permissions
		String command = "chmod 777 "+BaseSettings.APPDIR+name+"\n";
		Utils.rootExec(command);
	}

    /**
     * Function that updates the list of near devices in the wifi adhoc
     * @param data
     * 		HashMap with ip addresses as keys
     */
	protected void stampAddressList(HashMap<InetAddress,ArrayList<Integer>> data){
		String list = "";
		TextView listAddr=(TextView) findViewById(R.id.listOfSender);
		if(data.isEmpty()){
			listAddr.setText("Nobody near yet\n");
		}
		else{
			for(InetAddress keys : data.keySet()){
				list+=keys.getHostAddress()+"\n";
			}
			listAddr.setText(list);
		}
		listAddr.invalidate();
	}

	/**
	 * Function that updates the bit rate view
	 * @param bitRate
	 * 		Bit rate value
	 */
	protected void updateBitRate(int bitRate) {
		TextView bitRateField=(TextView) findViewById(R.id.bitRate);
		DecimalFormat df=new DecimalFormat();
		df.setMaximumFractionDigits(2);
		double rate;
		double average;
		double alpha=0.33;
		String stamp;
		
		if(movingAvg<0){
			average=bitRate;
			movingAvg=bitRate;
		}
		else{
			average=bitRate*alpha+movingAvg*(1-alpha);
			movingAvg=average;
		}
		
		if(average>=1024 && average<1024*1024){
			rate=((double)average)/1024;
			stamp=df.format(rate)+" Kbps/s";
		}
		else if(average>=1024*1024){
			rate=((double)average)/(1024*1024);
			stamp=df.format(rate)+" Mbps/s";
		}
		else
			stamp=df.format((double)average)+" bps/s";
		bitRateField.setText(stamp);
		bitRateField.invalidate();
	}

	/**
	 * Function that updates the RMSE graph
	 */
	protected void updateGraph() {
		
		ArrayList<GraphViewData> gvd = new ArrayList<GraphViewData>();
		float x, y;
		mService.cleaned.lock();
		for(int i=0;i<mService.RMSE.size();i++){
			x=mService.times.get(i).floatValue();
			y=mService.RMSE.get(i).floatValue();
			gvd.add(new GraphViewData((double) x, (double) y));
		}
		mService.RMSE.clear();
		mService.times.clear();
		mService.cleaned.unlock();
	
		if(!gvd.isEmpty()){
			if(gvs==null){
				GraphViewData[] array = new GraphViewData[gvd.size()];
				for(int i=0;i<gvd.size();i++){
					array[i]=gvd.get(i);
				}
				gvs= new GraphViewSeries(array);
				lgv.addSeries(gvs);
			}
			else if(resetGraph){
				GraphViewData[] array = new GraphViewData[gvd.size()];
				for(int i=0;i<gvd.size();i++){
					array[i]=gvd.get(i);
				}
				
				gvs.resetData(array);
				resetGraph=false;
			}
			else{
				for(int i=0;i<gvd.size();i++){
					gvs.appendData(gvd.get(i), true);
				}
			}
		}
	}
	
	/**
	 * Function that saves the performance stats on the external memory
	 * @throws IOException
	 */
	protected void performanceInfo() throws IOException{
		if(mService.mode){
			ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			int[] mpid={pid};
			//Recovering memory info
			android.os.Debug.MemoryInfo[] mi = am.getProcessMemoryInfo(mpid);
			int totalMem = mi[0].getTotalPss();
			OutputStreamWriter oswMEM = new OutputStreamWriter(new FileOutputStream(Environment.getExternalStorageDirectory()+"/CLAppInfo/memStat", true));
			oswMEM.append(Integer.toString(totalMem)+"\n");
			oswMEM.close();
			//Recovering cpu info
			String command = BaseSettings.APPDIR+"busybox top -n 1 | "+BaseSettings.APPDIR+"busybox grep \"^"+pid+"\" | "+BaseSettings.APPDIR+"busybox awk '{print $7}'\n";
			Log.i("PERFORMANCE", command);
			String[] response=Utils.rootExec(command);
			OutputStreamWriter oswCPU = new OutputStreamWriter(new FileOutputStream(Environment.getExternalStorageDirectory()+"/CLAppInfo/cpuStat", true));
			oswCPU.append(response[0]+"\n");
			oswCPU.close();
			//Recovering actual time
			long currentTime=System.currentTimeMillis()-appStartTime;
			OutputStreamWriter oswTIME = new OutputStreamWriter(new FileOutputStream(Environment.getExternalStorageDirectory()+"/CLAppInfo/timeInterv", true));
			oswTIME.append(Long.toString(currentTime)+"\n");
			oswTIME.close();
		}
	}

}
