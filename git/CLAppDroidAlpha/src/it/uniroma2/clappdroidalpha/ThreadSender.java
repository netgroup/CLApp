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

/**
 * Thread for the data sending
 * @author Daniele De Angelis
 *
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class ThreadSender extends Thread {
	
	public volatile static boolean exitLoop=false;
	
	private MainService st; //Service who called that thread
	private int index; //Actual index of the packet
	private final int fragmentSize=1200; //Maximum size of the packet
	private int bitSumma; //variable to save the bit sent
	
	/**
	 * Constructor
	 * @param st
	 * 		Service who calls the thread
	 */
	public ThreadSender(MainService st) {
		this.st=st;
		exitLoop=false;
		index=1;
		bitSumma=0;
	}
	
	@Override
	public void run(){
		byte[] send;
		try{
			ArrayList<byte[]> fragments;
			//Creating the exit socket
			DatagramSocket sock=new DatagramSocket();
			sock.setBroadcast(true);
			//Setting the broadcast destination
			InetAddress addr=InetAddress.getByName(getBroadcast());
			Packet envelope;
			long time=System.currentTimeMillis();
			while(!exitLoop){
				//if toSend isn't empty
				if(!st.toSend.isEmpty()){
					st.send.lock();
					//Picks the data from the service and inserts it into an array
					send=reform(st.toSend);
					st.toSend.clear();
					st.send.unlock();
				}
				//If toSend is empty there isn't any data to send
				else{
					continue;
				}
				//If a second is elapsed the bits sent value updates the bit rate
				if(System.currentTimeMillis()-time>1000){
					st.syncBitRate.lock();
					st.bitRate=bitSumma;
					bitSumma=0;
					st.syncBitRate.unlock();
					time=System.currentTimeMillis();
				}
				bitSumma+=send.length*8;
				//Data in send array are divided to be sent on the wifi adhoc net
				fragments=fragment(send);
				for(int i=0;i<fragments.size();i++){	
					//Each packet is sent with is own index
					envelope=new Packet(fragments.get(i),index);
					envelope.makePack();
					byte[] overall=envelope.getOverall();
					byte[] trmd=Packet.terminate(overall);
					DatagramPacket pkt=new DatagramPacket(trmd,trmd.length,addr,10000);
					sock.send(pkt);
					Log.i("Debug Sender","Packet sent "+System.nanoTime());
					index++;
				}
				send=new byte[fragmentSize];
				fragments.clear();
			}
    	} catch (Exception e){
    		e.printStackTrace();
    	}
	}
	
	/**
	 * Function that returns the broadcast ip of the actual net
	 * @return
	 * 		Broadcast ip
	 * @throws SocketException
	 */
	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private static String getBroadcast() throws SocketException {
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

	/**
	 * Reform an array list of array of bytes into an unique array
	 * @param data
	 * 		Data
	 * @return
	 * 		Unique array of byte
	 */
	private byte[] reform(ArrayList<byte[]> data){
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
	
	/**
	 * Divides an array of bytes into smaller array to make easier the sending phase
	 * @param data
	 * 		Data
	 * @return
	 * 		An array list of bytes arrays
	 */
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
