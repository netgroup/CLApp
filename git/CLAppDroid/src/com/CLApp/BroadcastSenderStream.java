package com.CLApp;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;

import com.audioclean.WaveManipulation;
import com.musicg.wave.Wave;
import com.musicg.wave.WaveHeader;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;


@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class BroadcastSenderStream extends AsyncTask<ArrayBlockingQueue<Byte>,Void,Void>{
	
	//private File file;
	private String fileName;
	private WaveHeader wh;
	private boolean done=false;
	//public Intent intent;
	
	/*public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface
	                .getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {}
	    return null;
	}
	*/
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
	
	public void sendChunk(DatagramSocket sock, InetAddress addr, int port, ArrayBlockingQueue<Byte> pipe) throws IOException, InterruptedException{
		DatagramPacket pack;
		//sock.joinGroup(addr);
		
		int size=1000;
		byte[] array;
		//String sizeInWord=Integer.toString(size);
		//String waveHeadS=wh.toString();
		Packet toSend;//=new Packet(waveHeadS.getBytes(),true)
		/*toSend.computeCrc();
		toSend.makePack();
		array=new byte[512];
		*/byte[] temp;//=Packet.terminate(toSend.getOverall())
		/*pack=new DatagramPacket(temp,temp.length,addr,port);
		sock.send(pack);*/
		//Thread.sleep(1000);
		//pack=new DatagramPacket((waveHeadS).getBytes(),(waveHeadS).getBytes().length,addr,port);
		//sock.send(pack);
		//Thread.sleep(1000);
		
		array=new byte[size];
		int indexC=1;
		while(!done){
			for(int i=0;i<size;i++){
				array[i]=pipe.take();
			}
			toSend=new Packet(array,indexC);
			toSend.computeCrc();
			toSend.makePack();
			temp=Packet.terminate(toSend.getOverall());
			pack=new DatagramPacket(temp,temp.length,addr,port);
			//Thread.sleep(100);
			sock.send(pack);
			Log.i("bcast", "packet send");
		}
		/*String term="";
		temp=term.getBytes();
		pack=new DatagramPacket(temp,0,addr,port);
		sock.send(pack);
		*/
	}
	
	public void sending(ArrayBlockingQueue<Byte> pipe) throws IOException, InterruptedException{
		DatagramSocket sock=new DatagramSocket();
		InetAddress addr=InetAddress.getByName(getBroadcast());
		sock.setBroadcast(true);
		int port = 10000;
		sendChunk(sock,addr,port,pipe);
		//sock.leaveGroup(addr);
		sock.close();
	}/*
	public void sending() throws IOException, InterruptedException{
		DatagramSocket sock=new DatagramSocket();
		
		//String address = getBroadcast();
		InetAddress addr=InetAddress.getByName(getBroadcast());
		sock.setBroadcast(true);
		int port = 10000;
		DatagramPacket pack;
		//sock.joinGroup(addr);
		
		long size=file.length(), index=0;
		FileInputStream fs=new FileInputStream(file);
		byte[] array=new byte[512];
		String sizeInWord=Integer.toString((int) size);
		/*if(size%512!=0){
			int multiplier=(int) (size/512);
			int remain=(int) (size-multiplier*512);
			sizeInWord+=" "+remain+".";
		}
		else{
			sizeInWord+=" 0.";
		}
		sizeInWord+=".";
		pack=new DatagramPacket((sizeInWord).getBytes("ASCII"),(sizeInWord).getBytes("ASCII").length,addr,port);
		sock.send(pack);
		ArrayList<ThreadSend> sender=new ArrayList<ThreadSend>();
		sock.setSoTimeout(10000);
		try{
			while(true){
				sock.receive(pack);
				InetAddress active=pack.getAddress();
				ThreadSend th= new ThreadSend(active,file);
				sender.add(th);
			}
		}catch(SocketTimeoutException e){}
		int n=0;
		while(n<sender.size()){
			sender.get(n).start();
			n++;
		}
		n=0;
		while(n<sender.size()){
			sender.get(n).join();
		}
	}*/
	
	public short[][] clusteringTrack(File f){
		Wave wav=new Wave(f.getAbsolutePath());
		WaveHeader head=wav.getWaveHeader();
		wh=head;
		int sampleRate=head.getSampleRate();
		short[] noWin=wav.getSampleAmplitudes();
		int numWin=WaveManipulation.computeNumWindows(noWin, 1.0, sampleRate);
		int winLen=sampleRate/1;
		return WaveManipulation.windowsCreation(noWin, numWin, winLen);
	}

	@Override
	protected Void doInBackground(ArrayBlockingQueue<Byte>... arg0) {
		ArrayBlockingQueue<Byte> pipe=arg0[0];
		try {
			sending(pipe);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void stopIt(){
		done=true;
	}

}
