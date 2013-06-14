package it.uniroma2.clappdroidalpha;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class ThreadSender extends Thread {
	private MainService st;
	public volatile static boolean exitLoop=false;
	private int index;
	private final int fragmentSize=1000;
	private int byteSumma;
	//public Handler mHandler;
	//For the explanation of that function, we remand you to the ListenStream class
	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static String getBroadcast() throws SocketException {
	   System.setProperty("java.net.preferIPv4Stack", "true");
	   for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
	      NetworkInterface ni = niEnum.nextElement();
	      if (!ni.isLoopback()) {
	          for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
	         	if(interfaceAddress.getBroadcast()!=null)
	         		return interfaceAddress.getBroadcast().toString().substring(1);
	          }
	      }
	   }
	   return null;
	}
	
	public ThreadSender(MainService st) {
		this.st=st;
		exitLoop=false;
		index=1;
		byteSumma=0;
	}
	
	private boolean Xor(boolean x, boolean y){
		return ( ( x || y ) && ! ( x && y ) );
	}
	
	@Override
	public void run(){
		byte[] send;
		try{
			ArrayList<byte[]> fragments;
			DatagramSocket sock=new DatagramSocket();
			sock.setBroadcast(true);
			InetAddress addr=InetAddress.getByName(getBroadcast());
			Packet envelope;
			long time=System.currentTimeMillis();
			while(!exitLoop){//Xor(exitLoop,st.toSend.isEmpty()) || !(exitLoop || st.toSend.isEmpty())){
				
	
				if(!st.toSend.isEmpty()){
					st.send.lock();
					send=reform(st.toSend);
					st.toSend.clear();
					st.send.unlock();
				}
				else{
					//sleep(1);
					continue;
				}
				if(System.currentTimeMillis()-time>1000){
					st.syncByteRate.lock();
					st.byteRate=byteSumma;
					byteSumma=0;
					st.syncByteRate.unlock();
					time=System.currentTimeMillis();
				}
				byteSumma+=send.length;
				fragments=fragment(send);
				for(int i=0;i<fragments.size();i++){	
					envelope=new Packet(fragments.get(i),index);
					envelope.makePack();
					byte[] overall=envelope.getOverall();
					DatagramPacket pkt=new DatagramPacket(overall,overall.length,addr,10000);
					sock.send(pkt);
					Log.i("Debug Sender","Packet sent "+System.nanoTime());
					index++;
				}
				send=new byte[1000];
				fragments.clear();
			}
    	} catch (Exception e){
    		e.printStackTrace();
    	}
	}
	
	/*
	@SuppressLint("HandlerLeak")
	public void run(){
		Looper.prepare();
		mHandler = new Handler() {
		    public void handleMessage(Message msg) {
		        try{
		        	byte[] send;
		        	DatagramSocket sock=new DatagramSocket();
		        	InetAddress addr=InetAddress.getByName(getBroadcast());
		        	Packet envelope;
		        	if(msg.what==22){
		        		send=msg.getData().getByteArray("data");
		        		envelope=new Packet(send,i);
		        		envelope.makePack();
		        		byte[] overall=envelope.getOverall();
		        		DatagramPacket pkt=new DatagramPacket(overall,overall.length,addr,10000);
		        		sock.send(pkt);
		        		//msg=null;
		        		Log.i(" Debug Sender","Packet sent");
		        		i++;
		        		
		        		super.handleMessage(msg);
		        	}
		        }catch(Exception e){
		        	e.printStackTrace();
		        }
		    }
		};
		Looper.loop();
	}
	*/
	
	
	/*
	public void killServ(){
		long time=System.nanoTime();
		while(true){
			if(System.nanoTime()-time>1000000000){
				if(!this.mHandler.hasMessages(22)){
					break;
				}
				time=System.nanoTime();
			}
		}
		this.mHandler.getLooper().quit();
	}
	*/
	
	public byte[] reform(ArrayList<byte[]> data){
		byte[] track;
		int size=0, j=0;
		for(int i=0;i<data.size();i++){
			size+=data.get(i).length;
		}
		track=new byte[size];
		for(int i=0;i<data.size();i++){
			for(int t=0;t<data.get(i).length;t++){
				track[j]=data.get(i)[t];
				j++;
			}
		}
		return track;
	}
	
	private ArrayList<byte[]> fragment(byte[] data){
		byte[] array;
		ArrayList<byte[]> fragments=new ArrayList<byte[]>();
		int numberOfFragment, last=0, index=0;
		if(data.length%fragmentSize!=0){
			numberOfFragment=(data.length/fragmentSize)+1;
			last=data.length%fragmentSize;
		}
		else{
			numberOfFragment=data.length/fragmentSize;
			last=fragmentSize;
		}
		for(int i=0;i<numberOfFragment;i++){
			if(i<numberOfFragment-1)
				array=new byte[fragmentSize];
			else
				array=new byte[last];
			for(int j=0;j<array.length;j++){
				array[j]=data[index];
				index++;
			}
			fragments.add(array.clone());
		}
		return fragments;
	}
}
