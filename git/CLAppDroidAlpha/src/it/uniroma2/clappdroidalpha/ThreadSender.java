package it.uniroma2.clappdroidalpha;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ThreadSender extends Thread {
	public int i=1;
	
	public Handler mHandler;
	//For the explanation of that function, we remand you to the ListenStream class
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
	
	public ThreadSender() {
	}
	
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

}
