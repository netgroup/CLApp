package com.CLApp;

import java.util.ArrayList;
import com.example.CLAppDroid.R;
import com.musicg.wave.WaveHeader;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	//Intent for the recording service
	private Intent intentRec;
	//Boolean used for the status of the button
	private boolean start;
	//String for the name of the file
	private String name;
	//Set of all the bytes array for the final track
	public ArrayList<byte[]> finalTrack;
	//Wave header for the save
	public WaveHeader waveHead;
	
	//On create we create the activity and we initialize the intent for the 
	//recording service and the boolean for the button status
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		intentRec=new Intent(this,regService.class);
		start=false;
		//intentBcast=new Intent(this,ServerRec.class);
		//Context ctx=this;
		//tr=new ThreadReceive(ctx,intentBcast);
		//tr.start();
		//startService(intentBcast);
	}
	
	//Function to manage the use of the button on the Context
	public void clicked(View view){
		View loading=this.findViewById(R.id.progressBar1);
		//if the button is already clicked, a Toast with a message is set
		//while the service is stopped and the progress bar is hidden
		//TODO insert the actions for the file save
		if(start){
			Toast tst;
			String str="sending...";
			tst=Toast.makeText(this, str, Toast.LENGTH_LONG);
			tst.show();
			start=false;
			loading.setVisibility(View.GONE);
			stopService(intentRec);
			/*intBcast=new Intent(this,BroadcastSender.class);
			intBcast.putExtra("fileName", Environment.getExternalStorageDirectory().getPath()+"/"+name+".wav");
			Log.i("reg", "starting broadcasting");
			startService(intBcast);*/
		}
		//The button isn't clicked. The text in the textfield is taken.
		//finalTrack is initialized. The boolean "start" is set true,
		//the progress bar is set visible and the recording service is started
		else{
			EditText text=(EditText) this.findViewById(R.id.editText1);
			name=text.getText().toString();
			finalTrack=new ArrayList<byte[]>();
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
	
	//The function add an array of byte to the ArrayList of bytes array of the final track
	public void addToFinalTrack(byte[] piece){
		finalTrack.add(piece);
	}
	
	//The wave header of the file is set in "waveHead"
	public void setWaveHeader(WaveHeader wh){
		waveHead=wh;
	}
	
	//The function is used to reassemble the bytes arrays for the final track save 
	public byte[] reformFinalTrack(){
		byte[] track;
		int size=0, j=0;
		for(int i=0;i<finalTrack.size();i++){
			size+=finalTrack.get(i).length;
		}
		track=new byte[size];
		for(int i=0;i<finalTrack.size();i++){
			for(int t=0;t<finalTrack.get(i).length;t++){
				track[j]=finalTrack.get(i)[t];
				j++;
			}
		}
		return track;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onDestroy(){
		super.onDestroy();
	}
	
	//TODO maybe is usefull to set the file saving in the onCancel function
	//to be able to save all the data also in case of app cancellation
}
