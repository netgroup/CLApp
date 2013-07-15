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

/**
 * Thread that manages the receiving phase
 * @author Daniele De Angelis
 *
 */
@SuppressLint("HandlerLeak")
public class ThreadListen extends Thread{
	private final static int TIMEOUT=1000; //Socket timeout on listen
	private final static int BUFFERED_SIZE=100000; //Buffer in receive
	private final static int TIME_TO_UPDATE=1000; //Time to elapse before to clean the data

	public static volatile boolean stop=false; //Boolean that manages the while loop
	private HashMap<InetAddress, ArrayList<byte[]>> arrChunks; //Hash map for the data
	private HashMap<InetAddress, ArrayList<Integer>> arrOrder; //Hash map for the order of each chunks of data 
	private MainService st; //Service who called that thread
	private CleanTask ct; //Thread for the cleaning phase
	private String pathName; //File path
	
	/**
	 * Constructor
	 * @param st
	 * 		Calling service
	 * @param pathName
	 * 		File path
	 */
	public ThreadListen(MainService st, String pathName) {
		this.pathName=pathName;
		arrChunks=new HashMap<InetAddress,ArrayList<byte[]>>();
		arrOrder=new HashMap<InetAddress,ArrayList<Integer>>();
		stop=false;
		this.st=st;
	}
	
	public void run(){
		DatagramSocket sock;
		ArrayList<byte[]> toClean;
		ct = null;
		try {
			//Sending socket creation
			sock = new DatagramSocket(10000);
			sock.setBroadcast(true);
			Packet pack=null;
			//Setting a default size for the received chunks
			DatagramPacket pkt=new DatagramPacket(new byte[1500],1500);
			//Setting timeout and buffer size of the socket
			sock.setSoTimeout(TIMEOUT);
			sock.setReceiveBufferSize(BUFFERED_SIZE);
			long time=System.currentTimeMillis();
			//st.lockServ.lock();
			while(!stop){
				//If a second is elapsed
				if(System.currentTimeMillis()-time>TIME_TO_UPDATE){
					//Sending at the broadcast receiver the data to update the UI
					Intent broadcast=new Intent();
					broadcast.setAction("android.intent.action.MAIN");
					Bundle envelope=new Bundle();
					envelope.putSerializable("Addresses", arrOrder);
					broadcast.putExtra("Envelope", envelope);
					st.syncBitRate.lock();
					envelope.putInt("BitRate", st.bitRate);
					st.bitRate=0;
					st.syncBitRate.unlock();
					MainActivity.current.sendBroadcast(broadcast);
					Log.d("Debug Listener","Broadcast sent");
					
					//Sending the data to clean at the CleanTask
					toClean=new ArrayList<byte[]>();
					//Gets received data from their data structures
					for(InetAddress tmp : arrChunks.keySet()){
						if(!arrChunks.isEmpty() || !arrOrder.isEmpty())
							toClean.add(reformPack(arrChunks.get(tmp), arrOrder.get(tmp)));
					}
					arrChunks=new HashMap<InetAddress,ArrayList<byte[]>>();
					arrOrder=new HashMap<InetAddress,ArrayList<Integer>>();
					st.record.lock();
					//Gets the data recorded by the device where the app is running
					if(!st.dataRecorded.isEmpty()){
						byte[] internalDataArray=mergeInternalData(st.dataRecorded);
						st.dataRecorded.clear();
						toClean.add(internalDataArray);
					}
					st.record.unlock();
					//Sends all the data to the cleaning thread
					if(!toClean.isEmpty()){
						if(ct!=null)
							ct.join();
						ct=new CleanTask(toClean,pathName,st);
						ct.start();
						Log.d("Debug Listener","Data to clean sent");
					//internalData.clear();
					}
					time=System.currentTimeMillis();
				}
				else{
					try{
						//Receives data from the net
						sock.receive(pkt);	
					}catch(SocketTimeoutException e){
						//If during listening the socket elapsed a second
						//the function retuns to the while
						if(!stop){
							sock.setSoTimeout(TIMEOUT);
						}
						continue;
					}
					sock.setSoTimeout(TIMEOUT);
					//If the packet isn't from the device own address
					if(!mine(pkt.getAddress())){
						pack=Packet.recoverDataDot(pkt.getData());
						if(pack==null){
							continue;
						}
						//If the address from where the packet came is unknown
						//it's added to the structure as a key
						if(!arrChunks.containsKey(pkt.getAddress())){
							arrChunks.put(pkt.getAddress(),new ArrayList<byte[]>());
							arrOrder.put(pkt.getAddress(),new ArrayList<Integer>());
						}
						arrChunks.get(pkt.getAddress()).add(pack.getData());
						arrOrder.get(pkt.getAddress()).add(Integer.parseInt(new String(pack.getIndex())));			
						Log.i("Debug Listener","Packet UDP received");
					}
					
				}
			}
			//When the function exits from the loop its work is continued 
			//until all the data are processed
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
			
			st.cleaned.lock();
			st.record.lock();
			st.dataRecorded=null;
			st.toSend=null;
			//st.RMSE=null;
			//st.times=null;
			st.record.unlock();
			st.cleaned.unlock();
		}catch(Exception e){
			e.printStackTrace();
		}
			
		
	}

	
	/**
	 * Merges data from multiple bytes arrays into a single one
	 * @param data
	 * 		Set of bytes arrays
	 * @return
	 * 		A bytes array
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
	
    /**
     * That function returns the size-2 array with the lowest and highest index
     * in the set of indexes received
     * @param hm
     * 		Set of indexes
     * @return
     * 		An array of size equals to 2 with min and max indexes
     */
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

	/**
	 *That function takes the Hashmap already declared and reforms the data sequence,
	 *zeropadding the gaps and reordering the arrivals
	 *
	 * @param chunks
	 * 		Set of chunks received
	 * @param order
	 * 		Their index in arrival order
	 * @return
	 * 		An array containing the complete data sequence ordered
	 */
	private byte[] reformPack(ArrayList<byte[]> chunks, ArrayList<Integer> order){
		//recollect has to contain the byte sequences ordered
		ArrayList<Byte> recollect = new ArrayList<Byte>();
		if(chunks.isEmpty())
			return null;
		//That array contains two items:
		//0: contains the lowest index of the sequence
		//1: contains the highest index of the sequence
		int[] minNmax=minNMaxNumChunk(order);
		int index, size=chunks.get(0).length;
		//The number from lowest and highest index are controlled
		for(int i=minNmax[0]-1;i<minNmax[1];i++){
			//if index is -1, the keys isn't contained, so the gap is zeropadded
			if((index=order.indexOf(i+1))==-1 || chunks.get(index)==null){
				for(int j=0;j<size;j++){
					//Zeropadding
					recollect.add(new Byte((byte) 0));
				}
			}
			//found the key in the hashmap the respective bytes sequence in the other hashmap
			//is added to the final arraylist
			else{
				for(int j=0;j<chunks.get(index).length;j++){
					recollect.add(new Byte(chunks.get(index)[j]));
				}
			}
		}
		byte[] toReturn=new byte[recollect.size()];
		//The arraylist in Byte is converted in an array of bytes
		for(int i=0;i<recollect.size();i++){
			toReturn[i]=recollect.get(i).byteValue();
		}
		return toReturn;
	}

	/**
	 * Function that compares an ip address with that actually used
	 * @param addr
	 * 		Ip in input
	 * @return
	 * 		True if the same, otherwise false 
	 * @throws SocketException
	 */
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
