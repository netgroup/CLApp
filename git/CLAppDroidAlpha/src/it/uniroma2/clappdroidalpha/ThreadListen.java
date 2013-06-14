package it.uniroma2.clappdroidalpha;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

@SuppressLint("HandlerLeak")
public class ThreadListen extends Thread{
	private final static int TIMEOUT=10000;
	private final static int BUFFERED_SIZE=100000;
	private final static int TIME_TO_UPDATE=1000000000;

	public static volatile boolean stop=false;
	private HashMap<InetAddress, ArrayList<byte[]>> arrChunks;
	private HashMap<InetAddress, ArrayList<Integer>> arrOrder;
	private MainService st;
	private String pathName;
	public int payloadSize;
	
	public ThreadListen(MainService st, String pathName) {
		this.pathName=pathName;
		payloadSize=0;
		arrChunks=new HashMap<InetAddress,ArrayList<byte[]>>();
		arrOrder=new HashMap<InetAddress,ArrayList<Integer>>();
		new ArrayList<byte[]>();
		stop=false;
		this.st=st;
	}
	/*
	@SuppressLint("HandlerLeak")
	public Handler mHandler=new Handler(){

		public void handleMessage(Message msg) {
			int index;
			if(msg.what==20){
				lock.lock();
				bSamples=msg.arg1;
				internalData.add(msg.getData().getByteArray("data"));
				byte[] recv=internalData.get(internalData.size()-1);
				Log.i("Debug Listener","Msg received");
				
				payloadSize += recv.length;
				if (bSamples == 16)
				{
					for (int i=0; i<recv.length/2; i++)
					{ // 16bit sample size
						short curSample = AudioRecorder.getShort(recv[i*2], recv[i*2+1]);
						if (curSample > cAmplitude)
						{ // Check amplitude
							cAmplitude = curSample;
						}
					}
				}
				else	
				{ // 8bit sample size
					for (int i=0; i<recv.length; i++)
					{
						if (recv[i] > cAmplitude)
						{ // Check amplitude
							cAmplitude = recv[i];
						}
					}
				}
				super.handleMessage(msg);
				lock.unlock();
			}
		}
	};
	*/
	private byte[] mergeInternalData(ArrayList<byte[]> data){
		int size=0, index=0;
		for(int i=0;i<data.size();i++){
			size+=data.get(i).length;
		}
		byte[] array=new byte[size];
		for(int i=0;i<data.size();i++){
			for(int j=0;j<data.get(i).length;j++){
				array[index]=data.get(i)[j];
				index++;
			}
		}
		return array;
	}
	
	public void run(){
		DatagramSocket sock;
		ArrayList<byte[]> toClean;
		CleanTask ct = null;
		try {
			sock = new DatagramSocket(10000);
			sock.setBroadcast(true);
			Packet pack=null;
			DatagramPacket pkt=new DatagramPacket(new byte[1500],1500);
			sock.setSoTimeout(TIMEOUT);
			sock.setReceiveBufferSize(BUFFERED_SIZE);
			long time=System.nanoTime();
			//st.lockServ.lock();
			while(!stop){
				if(System.nanoTime()-time>TIME_TO_UPDATE){
					//Broadcast part
					Intent broadcast=new Intent();
					broadcast.setAction("android.intent.action.MAIN");
					Bundle envelope=new Bundle();
					envelope.putSerializable("Addresses", arrOrder);
					broadcast.putExtra("Envelope", envelope);
					st.syncByteRate.lock();
					envelope.putInt("ByteRate", st.byteRate);
					st.byteRate=0;
					st.syncByteRate.unlock();
					//broadcast.putExtra("message", "funziona");
					//broadcast.setAction(Intent.);
					MainActivity.current.sendBroadcast(broadcast);
					Log.d("Debug Listener","Broadcast sent");
					
					//Cleaning part
					toClean=new ArrayList<byte[]>();
					for(InetAddress tmp : arrChunks.keySet()){
						toClean.add(reformPack(arrChunks.get(tmp), arrOrder.get(tmp)));
					}
					arrChunks.clear();
					arrOrder.clear();
					st.record.lock();
					if(!st.dataRecorded.isEmpty()){
						byte[] internalDataArray=mergeInternalData(st.dataRecorded);
						st.dataRecorded.clear();
						toClean.add(internalDataArray);
					}
					st.record.unlock();
					if(ct!=null)
						ct.join();
					ct=new CleanTask(toClean,pathName,st);
					ct.start();
					Log.d("Debug Listener","Data to clean sent");
					//internalData.clear();
					time=System.nanoTime();
				}
				else{
					try{
						sock.receive(pkt);	
					}catch(SocketTimeoutException e){
						if(!stop){
							sock.setSoTimeout(TIMEOUT);
						}
						continue;
					}
					sock.setSoTimeout(TIMEOUT);
					if(!mine(pkt.getAddress())){
						pack=Packet.recoverDataDot(pkt.getData());
						if(!arrChunks.containsKey(pkt.getAddress())){
							arrChunks.put(pkt.getAddress(),new ArrayList<byte[]>());
							arrOrder.put(pkt.getAddress(),new ArrayList<Integer>());
						}
						arrChunks.get(pkt.getAddress()).add(pack.getData());
						arrOrder.get(pkt.getAddress()).add(Integer.parseInt(new String(pack.index)));			
						Log.i("Debug Listener","Packet UDP received");
					}
					
				}
			}
			sock.close();
			Log.i("Debug Listener","While(true) exited");
			long timer=System.currentTimeMillis();
			while(System.currentTimeMillis()-timer<5000){
				if(!st.dataRecorded.isEmpty()){
					toClean=new ArrayList<byte[]>();
					if(!arrChunks.isEmpty()){
						for(InetAddress tmp : arrChunks.keySet()){
							toClean.add(reformPack(arrChunks.get(tmp), arrOrder.get(tmp)));
						}
					}
					arrChunks.clear();
					arrOrder.clear();
					st.record.lock();
					if(!st.dataRecorded.isEmpty()){
						byte[] internalDataArray=mergeInternalData(st.dataRecorded);
						st.dataRecorded.clear();
						toClean.add(internalDataArray);
					}
					st.record.unlock();
					
					if(ct!=null)
						ct.join();
					ct=new CleanTask(toClean,pathName,st);
					ct.start();
					//internalData.clear();
					timer=System.currentTimeMillis();
				}
			}
			//st.lockServ.unlock();
		}catch(Exception e){
			e.printStackTrace();
		}
			
		
	}

	//That function returns the size-2 array with the lowest and highest index
	private int[] minNMaxNumChunk(ArrayList<Integer> hm){
		int min=Integer.MAX_VALUE, max=Integer.MIN_VALUE;
		int[] ret=new int[2];
		for(int i=0;i<hm.size();i++){
			if(min>hm.get(i)){
				min=hm.get(i);
			}
			if(max<hm.get(i)){
				max=hm.get(i);
			}
		}
		ret[0]=min;
		ret[1]=max;
		return ret;
	}

	//That function takes the Hashmap already declared and reforms the data sequence,
	//zeropadding the gaps and reordering the arrivals
	private byte[] reformPack(ArrayList<byte[]> chunks, ArrayList<Integer> order){
		//recollect has to contain the byte sequences ordered
		ArrayList<Byte> recollect = new ArrayList<Byte>();
		//That array contains two items:
		//0: contains the lowest index of the sequence
		//1: contains the highest index of the sequence
		int[] minNmax=minNMaxNumChunk(order);
		int index, size=chunks.get(0).length;
		//The number from lowest and highest index are controlled
		for(int i=minNmax[0]-1;i<minNmax[1];i++){
			//if index is -1, the keys isn't contained, so the gap is zeropadded
			if((index=order.indexOf(i+1))==-1){
				for(int j=0;j<size;j++){
					//Zeropadding
					recollect.add(new Byte((byte) 0));
				}
			}
			//found the key in the hashmap the respective bytes sequence in the other hashmap
			//is added to the final arraylist
			else{
				for(int j=0;j<chunks.get(index).length;j++){
					recollect.add(new Byte(chunks.get(index-minNmax[0])[j]));
				}
			}
		}
		Byte[] toArray=(Byte[]) recollect.toArray();
		byte[] toReturn=new byte[toArray.length];
		//The arraylist in Byte is converted in an array of bytes
		for(int i=0;i<toArray.length;i++){
			toReturn[i]=toArray[i].byteValue();
		}
		return toReturn;
	}

	//mine is a function that, similar to getBroadcast already explained, returns if the IP address given
	//is our address, to permit to our algorithm to exclude our data from the broadcast received
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private static boolean mine(InetAddress addr) throws SocketException {
	    System.setProperty("java.net.preferIPv4Stack", "true");
	    
	    for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
	        NetworkInterface ni = niEnum.nextElement();
	        if (!ni.isLoopback()) {
	            for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
	            	if(interfaceAddress.getAddress().equals(addr))
	            		return true;
	            }
	        }
	    }
	    return false;
	}

}
