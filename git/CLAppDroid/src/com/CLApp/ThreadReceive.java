package com.CLApp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

import com.musicg.wave.Wave;
import com.musicg.wave.WaveFileManager;
import com.musicg.wave.WaveHeader;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class ThreadReceive extends Thread {
	Context contxt;
	Intent intent;
	private static DatagramSocket sock;
	private static WaveHeader wh;
	
	ThreadReceive(Context ctx,Intent i){
		contxt=ctx;
		intent=i;
	}
	public void run(){
		receiver();
	}
	
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
	
	public void chunksReceiver(DatagramSocket sock,InetAddress addr,int port, int len) throws IOException{
		DatagramPacket pack=null;
		byte[] extract=new byte[512];
		byte[] data=new byte[512];
		byte[] full=new byte[len];
		int proc=0;
		pack=new DatagramPacket(data,data.length,addr,port);
		try{
			Log.i("Server","waiting for packets");
			//System.out.println("In attesa di pacchetti");
			while (proc<len) {
				sock.receive(pack);
				extract=pack.getData();
				for(int i=0;i<extract.length && (i+proc)<len;i++){
					full[i+proc]=extract[i];
				}
				Log.i("Server","packet received");
				//System.out.println("Pacchetto ricevuto");
				sock.setSoTimeout(10000);
				proc+=512;
			}
		}catch(SocketTimeoutException e){
			Log.e("Server", "time runned out");
			//System.out.println("tempo scaduto");
			return;
		}finally{
			Wave wav=new Wave(wh,full);
			WaveFileManager wfm=new WaveFileManager(wav);
			wfm.saveWaveAsFile("regReceived.wav");
		}
		Context cntx=contxt;
		String str=new String("File received");
		int dur=Toast.LENGTH_SHORT;
		Toast ts=Toast.makeText(cntx, str, dur);
		ts.show();
	}
	
	public void receiver(){
		sock=null;
		byte[] receive=new byte[512];
		byte[] extract=new byte[512];
		String index=new String();
		String sizeInWord=new String();
		String waveHeadS=new String();
		int m=0;
		int size=0;
		InetAddress addr=null;
		int port = 10000;
		try{
			sock=new DatagramSocket(port);
			//addr=InetAddress.getByName(getBroadcast());
			DatagramPacket pk=new DatagramPacket(receive,receive.length);
			while(true){
				sock.receive(pk);
				extract=pk.getData();
			
				m=0;
				while(index.compareTo(".")!=0){
					if(m<extract.length)
						index=new String(extract,m,1);
					else{
						Log.e("Server", "String out of bound (#pkt)");
						//System.out.println("String out of bound");
					}	
					if(index.compareTo(".")!=0){
						sizeInWord+=index;
						m++;
					}
				}
				sizeInWord.trim();
				size=Integer.parseInt(sizeInWord);
		
				sock.receive(pk);
				index=new String();
				m=0;
				while(index.compareTo(".")!=0){
					if(m<extract.length)
						index=new String(extract,m,1);
					else{
						Log.e("Server", "String out of bound (wh)");
						//System.out.println("String out of bound");
					}	
					if(index.compareTo(".")!=0){
						waveHeadS+=index;
						m++;
					}
				}
				wh=WaveHeader.parseString(waveHeadS);
				chunksReceiver(sock,addr,port,size);
			}
		}catch(IOException e){
			Log.e("Server", "Error in socket bcast");
			//System.err.println("Error in socket broadcast");
			System.exit(1);
		}
	}
	
		
}
