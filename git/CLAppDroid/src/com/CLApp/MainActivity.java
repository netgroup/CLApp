package com.CLApp;

import java.util.concurrent.ArrayBlockingQueue;

import com.example.CLAppDroid.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private Intent intentRec;
	private Intent intBcast;
	private boolean start;
	private ThreadReceive tr;
	private Listening at;
	private String name;
	private ArrayBlockingQueue<Byte> pipeRX;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		pipeRX=new ArrayBlockingQueue<Byte>(1000);
		intentRec=new Intent(this,regService.class);
		start=false;
		at=new Listening(this.getApplicationContext());
		at.execute(pipeRX);
		//intentBcast=new Intent(this,ServerRec.class);
		//Context ctx=this;
		//tr=new ThreadReceive(ctx,intentBcast);
		//tr.start();
		//startService(intentBcast);
	}
	
	
	
	public void clicked(View view){
		View loading=this.findViewById(R.id.progressBar1);
		
		if(start){
			Toast tst;
			String str="sending...";
			tst=Toast.makeText(this, str, 2000);
			tst.show();
			start=false;
			loading.setVisibility(View.GONE);
			stopService(intentRec);
			intBcast=new Intent(this,BroadcastSender.class);
			intBcast.putExtra("fileName", Environment.getExternalStorageDirectory().getPath()+"/"+name+".wav");
			Log.i("reg", "starting broadcasting");
			startService(intBcast);
		}
		else{
			EditText text=(EditText) this.findViewById(R.id.editText1);
			name=text.getText().toString();
			if(name.compareTo("")!=0)
				intentRec.putExtra("fileName", name);
			
			else{
				name="no_name";
				intentRec.putExtra("fileName", "no_name");
			}
				
			start=true;
			loading.setVisibility(View.VISIBLE);
			startService(intentRec);
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onDestroy(){
		at.cancel(true);
		super.onDestroy();
	}

}
