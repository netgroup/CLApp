package com.CLApp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class ListeningStream extends Thread {
	//Socket in listen
	private DatagramSocket sock;
	//Pipe to receive the data recorded
	ArrayBlockingQueue<Byte> pipeIN;
	//Hashmap with ip address as keys and sequence of chunk as objects
	HashMap<InetAddress,ArrayList<Integer>> chunkVerify;
	//Hashmap with ip address as keys and the real bytes sequence in the order of arrive as objects
	HashMap<InetAddress,ArrayList<byte[]>> chunk;
	//ArrayList to contain all the bytes sequence to send to the Cleaning Algorithm
	ArrayList<byte[]> totalTrack;
	//Boolean used to stop the while(true) sequence
	private static volatile boolean done;
	//Name header file TODO maybe also output file
	String fileName;
	
	//Constructor. It takes as attribute the pipe to receive data
	ListeningStream(ArrayBlockingQueue<Byte> ar, String name){
		pipeIN=ar;
		done=false;
		fileName=name;
	}
	
	//Function to get the broadcast address from our actual connection
	public static String getBroadcast() throws SocketException {
	    System.setProperty("java.net.preferIPv4Stack", "true");
	    //All the network interfaces are took and stored in an Enumeration for the "for" loop
	    for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
	        NetworkInterface ni = niEnum.nextElement();
	        //One by one the interfaces are controlled
	        //If it isn't lo interface the code go on. Other ways it return to the for statement
	        if (!ni.isLoopback()) {
	        	//For the selected interface the function takes all the address associated
	            for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
	            	//If the getBroadcast function returns null, we are talking about a IPv6 address, so we return to the for
	            	if(interfaceAddress.getBroadcast()!=null)
	            		//Instead, if the getBroadcast returns a proper address, that is the address that this function returns
	            		return interfaceAddress.getBroadcast().toString().substring(1);
	            }
	        }
	    }
	    return null;
	}
	
	//mine is a function that similar to getBroadcast already explained, returns if the IP address given
	//is our address, to permit to our algorithm to exclude our data from the broadcast received
	public static boolean mine(InetAddress addr) throws SocketException {
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
	
	//Run function of the thread. It only call the server function
	public void run() {
		try {
			server();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//That function takes the Hashmap already declared and reforms the data sequence,
	//zeropadding the gaps and reordering the arrivals
	public byte[] reformPack(ArrayList<byte[]> chunks, ArrayList<Integer> order){
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
	
	//Primary function of the thread
	public void server() throws InterruptedException{
		//Initialization of the hashmaps
		CleanTask ct=new CleanTask(null,null);
		chunkVerify=new HashMap<InetAddress, ArrayList<Integer>>();
		chunk=new HashMap<InetAddress, ArrayList<byte[]>>();
		totalTrack=new ArrayList<byte[]>();
		InetAddress[] keys = null;
		
		//Socket in listen to the port 10000
		int port=10000;
		//Array used in the DatagramPacket
		byte[] buck=new byte[1500];
		byte[] real;
		Byte[] internal;
		byte[] internalConv;
		Packet pkt;
		sock=null;
		try{
			//socket initialized
			sock=new DatagramSocket(port);
			sock.setReuseAddress(true);
			//DatagramPacket initialized
			DatagramPacket pk=new DatagramPacket(buck,buck.length);
			//Time in nanoseconds memorized in time.
			//this is used to decide when the sequences could be sent to the cleaning Algorithm
			long time=System.nanoTime();
			//That while can be stopped with the use of the function stopIt
			while(!this.isInterrupted()){
				//True if from the memorization of time a second has elapsed
				if(System.nanoTime()-time>100000000){
					//chunk.keySet().toArray(keys);
					//all the bytes sequence from the different IPs are reordered and finalized
					for(InetAddress tmp : chunk.keySet()){
						totalTrack.add(reformPack(chunk.get(tmp),chunkVerify.get(tmp)));
					}
					//The datas in the internal pipe are took to be added with the other in the arraylist 
					internal=pipeIN.toArray(new Byte[pipeIN.size()]);
					pipeIN.clear();
					internalConv=new byte[internal.length];
					for(int i=0;i<internalConv.length;i++){
						internalConv[i]=internal[i].byteValue();
					}
					totalTrack.add(internalConv);
					//TODO insert a call to the AsyncTask of the cleaning algorithm
					if(ct.isAlive()){
						ct.join();
					}
					ct=new CleanTask(totalTrack,fileName);
					ct.run();
					chunkVerify=new HashMap<InetAddress, ArrayList<Integer>>();
					chunk=new HashMap<InetAddress, ArrayList<byte[]>>();
					time=System.nanoTime();
					totalTrack=new ArrayList<byte[]>();
				}
				else{
					//a new packet is received from the socket in listen
					sock.receive(pk);
					//if the packet is mine (from my own address) it is tossed
					if(!mine(pk.getAddress())){
						//The dot is removed from the end of the string of byte end a packet is created
						real=Packet.recvTerminated(pk.getData());
						pkt=Packet.recoverData(real);
						//If the sender IP isn't already known, it is inserted in the Hashmaps as new key
						if(!chunkVerify.containsKey(pk.getAddress())){
							chunkVerify.put(pk.getAddress(), new ArrayList<Integer>());
							chunk.put(pk.getAddress(), new ArrayList<byte[]>());
						}
						chunkVerify.get(pk.getAddress()).add(Integer.parseInt(new String(pkt.index)));
						chunk.get(pk.getAddress()).add(pkt.getData());
					}
					
				}
				//At the end of the loop, if there are others data in the Hashmaps and/or in the pipe
				//those are sent to the cleaning algorithm
			}
			System.out.println("while(true) listen stopped");
			if(!chunk.isEmpty()||!pipeIN.isEmpty()){
				//TODO insert a way to determinate if there aren't new packet in the socket but there are in the pipe
				if(!chunk.isEmpty()){
					keys=(InetAddress[]) chunk.keySet().toArray();
					for(int i=0;i<keys.length;i++){
						totalTrack.add(reformPack(chunk.get(keys[i]),chunkVerify.get(keys[i])));
					}
				}
				if(!pipeIN.isEmpty()){
					internal=pipeIN.toArray(new Byte[pipeIN.size()]);
					pipeIN.clear();
					internalConv=new byte[internal.length];
					for(int i=0;i<internalConv.length;i++){
						internalConv[i]=internal[i].byteValue();
					}
					totalTrack.add(internalConv);
				}
				//TODO insert a call to the AsyncTask of the cleaning algorithm
				if(ct.isAlive()){
					ct.join();
				}
				ct=new CleanTask(totalTrack,fileName);
				ct.run();
				ct.join();
				chunkVerify=new HashMap<InetAddress, ArrayList<Integer>>();
				chunk=new HashMap<InetAddress, ArrayList<byte[]>>();
			}
			//TODO insert a way to signal to the audiocleaning algorithm to save the file
		}catch(InterruptedIOException e){
			Log.e("Server", "Error in socket bcast");
			System.exit(1);
		}catch(SocketException e){
			Log.e("Server", "Error in socket bcast");
			System.exit(1);
		}catch(IOException e){
			Log.e("Server", "Error in socket bcast");
			System.exit(1);
		}
	}
	
	//Function to stop the while(true) loop
	public static void stopIt(){
		done=true;
	}

}
