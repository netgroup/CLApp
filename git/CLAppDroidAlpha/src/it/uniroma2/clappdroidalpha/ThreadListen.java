package it.uniroma2.clappdroidalpha;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressLint("HandlerLeak")
public class ThreadListen extends HandlerThread{

	public static volatile boolean stop=false;
	private HashMap<InetAddress, ArrayList<byte[]>> arrChunks;
	private HashMap<InetAddress, ArrayList<Integer>> arrOrder;
	private ArrayList<byte[]> internalData;
	private Lock lock;
	private ServiceTest st;
	private String pathName;
	
	public ThreadListen(String name, ServiceTest st, String pathName) {
		super(name);
		this.pathName=pathName;
		arrChunks=new HashMap<InetAddress,ArrayList<byte[]>>();
		arrOrder=new HashMap<InetAddress,ArrayList<Integer>>();
		internalData=new ArrayList<byte[]>();
		stop=false;
		lock=new ReentrantLock();
		this.st=st;
	}
	
	@SuppressLint("HandlerLeak")
	public Handler mHandler=new Handler(){
		public void handleMessage(Message msg) {
			if(msg.what==20){
				lock.lock();
				internalData.add(msg.getData().getByteArray("data"));
				Log.i("Debug Listener","Msg received");
				lock.unlock();
			}
		}
	};
	
	private byte[] mergeInternalData(){
		int size=0, index=0;
		for(int i=0;i<internalData.size();i++){
			size+=internalData.get(i).length;
		}
		byte[] array=new byte[size];
		for(int i=0;i<internalData.size();i++){
			for(int j=0;j<internalData.get(i).length;j++){
				array[index]=internalData.get(i)[j];
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
			Packet pack=null;
			DatagramPacket pkt=new DatagramPacket(new byte[1500],1500);
			sock.setSoTimeout(10000);
			long time=System.nanoTime();
			st.lockServ.lock();
			while(!stop){
				if(System.nanoTime()-time>1000000000){
					toClean=new ArrayList<byte[]>();
					for(InetAddress tmp : arrChunks.keySet()){
						toClean.add(reformPack(arrChunks.get(tmp), arrOrder.get(tmp)));
					}
					lock.lock();
					byte[] internalDataArray=mergeInternalData();
					lock.unlock();
					toClean.add(internalDataArray);
					if(ct!=null)
						ct.join();
					ct=new CleanTask(toClean,pathName,st);
					ct.start();
					Log.i("Debug Listener","Data to clean sent");
					arrChunks.clear();
					arrOrder.clear();
					internalData.clear();
					time=System.nanoTime();
				}
				else{
					try{
						sock.receive(pkt);	
					}catch(SocketTimeoutException e){
						if(stop){
							time=System.nanoTime();
						}
						else
							sock.setSoTimeout(10000);
						continue;
					}
					sock.setSoTimeout(10000);
					if(!mine(pkt.getAddress())){
						pack=Packet.recoverDataDot(pkt.getData());
						if(!arrChunks.containsKey(pkt.getAddress())){
							arrChunks.put(pkt.getAddress(),new ArrayList<byte[]>());
							arrOrder.put(pkt.getAddress(),new ArrayList<Integer>());
						}
						arrChunks.get(pkt.getAddress()).add(pack.getData());
						arrOrder.get(pkt.getAddress()).add(Integer.parseInt(new String(pack.index)));			
					}
				}
			}
			sock.close();
			Log.i("Debug Listener","While(true) exited");
			long timer=System.currentTimeMillis();
			while(System.currentTimeMillis()-timer<5000){
				if(!internalData.isEmpty()){
					toClean=new ArrayList<byte[]>();
					if(!arrChunks.isEmpty()){
						for(InetAddress tmp : arrChunks.keySet()){
							toClean.add(reformPack(arrChunks.get(tmp), arrOrder.get(tmp)));
						}
					}
					lock.lock();
					byte[] internalDataArray=mergeInternalData();
					lock.unlock();
					toClean.add(internalDataArray);
					if(ct!=null)
						ct.join();
					ct=new CleanTask(toClean,pathName,st);
					ct.start();
					arrChunks.clear();
					arrOrder.clear();
					internalData.clear();
					timer=System.currentTimeMillis();
				}
			}
			st.lockServ.unlock();
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

	//mine is a function that similar to getBroadcast already explained, returns if the IP address given
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
