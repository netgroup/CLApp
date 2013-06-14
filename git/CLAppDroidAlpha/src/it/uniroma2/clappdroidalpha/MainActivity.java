package it.uniroma2.clappdroidalpha;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import it.uniroma2.clappdroidalpha.MainService.LocalBinder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import it.uniroma2.mobilecollaborationplatform.*;

public class MainActivity extends Activity {
	
	MainService mService=null;
	boolean mBound = false;
	private boolean clicked=false;
	private WakeLock wl;
	private String pathDirectory;
	public LocalBroadcastManager lbm;
	public static MainActivity current;
	private UpdateUiReceiver receiver;
	private InetAddress ipAddress;
	
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
		
	}
	
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
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onStart(){
		super.onStart();
		current=this;
		IFManager.startWifiAdHoc();
		try {
			ipAddress=Utils.localAddressNotLo();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Intent serv=new Intent(this,MainService.class);
		bindService(serv,mConnection,Context.BIND_AUTO_CREATE);
		PowerManager pm=(PowerManager) this.getSystemService(Context.POWER_SERVICE);
		wl=pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "ClappDroid");
		File f = new File(Environment.getExternalStorageDirectory() + "/WavFiles");
		if(!f.exists()) {
			f.mkdir();
		}
		pathDirectory=f.getAbsolutePath();
			
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,new IntentFilter("prova_broadcast_sender"));
		
		TextView ipAddr=(TextView) findViewById(R.id.ipAddr);
		ipAddr.setText(ipAddress.getHostAddress());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onStop(){
		if(mBound){
			unbindService(mConnection);
			mBound=false;
		}
		IFManager.stopWifiAdHoc();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		//IFManager.disableFirewall();
    	super.onStop();
		
	}
	/*
	@Override
	protected void onPause(){
		if(mBound){
			unbindService(mConnection);
			mBound=false;
		}
		IFManager.stopWifiAdHoc();
    	super.onPause();
		
	}
	*/
	public void startServ(View view){
		if(!clicked){
			wl.acquire();
			clicked=true;
			EditText et=(EditText) this.findViewById(R.id.editText1);
			TextView listAddr=(TextView) findViewById(R.id.listOfSender);
			TextView bRate=(TextView) findViewById(R.id.listOfSender);
			//View progressBar=findViewById(R.id.progressBar1);
			View panel=findViewById(R.id.List);
			//progressBar.setVisibility(View.VISIBLE);
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
			panel.setVisibility(View.VISIBLE);	
			mService.startServiceTest(this,name,pathDirectory);
			Log.i("Debug activity","Button on");
			//View progress=this.findViewById(R.id.progressBar1);
			//progress.setVisibility(View.VISIBLE);
		}
	}
	
	public void stopServ(View view) throws InterruptedException, IOException{
		if(clicked){
			wl.release();
			clicked=false;
			mService.stopServiceTest();
			Log.i("Debug activity","Button off");
			//View progress=this.findViewById(R.id.progressBar1);
			View panel=findViewById(R.id.List);
			//progress.setVisibility(View.INVISIBLE);
			panel.setVisibility(View.INVISIBLE);			
			Toast finish=Toast.makeText(this.getApplicationContext(), "Done!", Toast.LENGTH_SHORT);
			finish.show();
		}
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
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

	protected void updateByteRate(int byteRate) {
		TextView byteRateField=(TextView) findViewById(R.id.byteRate);
		DecimalFormat df=new DecimalFormat();
		df.setMaximumFractionDigits(2);
		double rate;
		String stamp;
		if(byteRate>=1024 && byteRate<1024*1024){
			rate=((double)byteRate)/1024;
			stamp=df.format(rate)+" KiB/s";
		}
		else if(byteRate>=1024*1024){
			rate=((double)byteRate)/(1024*1024);
			stamp=df.format(rate)+" MiB/s";
		}
		else
			stamp=df.format((double)byteRate)+" B/s";
		byteRateField.setText(stamp);
		byteRateField.invalidate();
	}

}
