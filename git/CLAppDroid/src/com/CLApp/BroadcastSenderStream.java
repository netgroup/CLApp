package com.CLApp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;


@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class BroadcastSenderStream extends Thread{
	
	private ArrayBlockingQueue<Byte> pipe;
	private boolean done=false;
	
	//Constructor. "pipe" attribute is used to pass the pipe to receive data to send
	BroadcastSenderStream(ArrayBlockingQueue<Byte> pipe){
		this.pipe=pipe;
		done=false;
	}
	
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
	
	//This function is the primary function. It acts like server
	//and send chunks received from the pipe
	public void sendChunk(DatagramSocket sock, InetAddress addr, int port) throws IOException, InterruptedException{
		DatagramPacket pack;
		
		//The size of the chunks is set to 1000 (Note: it is the size of only the data part)
		int size=1000;
		byte[] array;
		Packet toSend;//=new Packet(waveHeadS.getBytes(),true)
		byte[] temp;//=Packet.terminate(toSend.getOverall())
		
		array=new byte[size];
		int indexC=1;
		//While loop stopped only when stopIt is called
		while(!done){
			for(int i=0;i<size;i++){
				array[i]=pipe.take();
			}
			toSend=new Packet(array,indexC);
			toSend.makePack();
			temp=Packet.terminate(toSend.getOverall());
			pack=new DatagramPacket(temp,temp.length,addr,port);
			sock.send(pack);
			Log.i("bcast", "packet send");
		}
	}
	
	//Sending setup all the elements for the sending and then call the sendChunks function to
	//really send data
	public void sending() throws IOException, InterruptedException{
		DatagramSocket sock=new DatagramSocket();
		InetAddress addr=InetAddress.getByName(getBroadcast());
		sock.setBroadcast(true);
		int port = 10000;
		sendChunk(sock,addr,port);
		sock.close();
	}
	
	//run function of the thread. It only calls the sending function
	public void run() {
		try {
			sending();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//Function called to stop the while(true) loop
	public void stopIt(){
		done=true;
	}

}
