package it.uniroma2.clappdroidalpha;


import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import it.uniroma2.clappdroidalpha.ServiceTest.LocalBinder;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import it.uniroma2.mobilecollaborationplatform.*;

public class MainActivity extends Activity {
	
	ServiceTest mService=null;
	boolean mBound = false;
	private boolean clicked=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Copies required executables to the application directory
		copyFileFromAssets("iptables", false);
		copyFileFromAssets("wifiloader", false);
		copyFileFromAssets("iwconfig", false);
		copyFileFromAssets("ifconfig", true);
		copyFileFromAssets("ip", false);
		copyFileFromAssets("busybox", false);
		
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		IFManager.startWifiAdHoc();
		Intent serv=new Intent(this,ServiceTest.class);
		bindService(serv,mConnection,Context.BIND_AUTO_CREATE);
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
    	super.onStop();
		
	}
	
	@Override
	protected void onPause(){
		if(mBound){
			unbindService(mConnection);
			mBound=false;
		}
		IFManager.stopWifiAdHoc();
    	super.onPause();
		
	}
	
	public void startServ(View view){
		if(!clicked){
			clicked=true;
			EditText et=(EditText) this.findViewById(R.id.editText1);
			String name=et.getText().toString();
			if(name.equals("")){
				name="no_name.wav";
			}
			else{
				name=name+".wav";
			}
			mService.startServiceTest(name);
			Log.i("Debug activity","Button on");
			View progress=this.findViewById(R.id.progressBar1);
			progress.setVisibility(View.VISIBLE);
		}
	}
	
	public void stopServ(View view) throws InterruptedException{
		if(clicked){
			clicked=false;
			mService.stopServiceTest();
			Log.i("Debug activity","Button off");
			View progress=this.findViewById(R.id.progressBar1);
			progress.setVisibility(View.INVISIBLE);
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

}
