package com.CLApp;
import java.io.IOException;
import java.io.InterruptedIOException;
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
import java.util.Timer;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.musicg.wave.Wave;
import com.musicg.wave.WaveFileManager;
import com.musicg.wave.WaveHeader;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class ServerRec extends Service{
	private static DatagramSocket sock;
	private static WaveHeader wh;
	HashMap<InetAddress,ArrayList<Integer>> chunkVerify;
	HashMap<InetAddress,ArrayList<byte[]>> chunk;
	HashMap<InetAddress,WaveHeader> waveHeaders;
	HashMap<InetAddress, byte[]> totalTrack;
	
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
		Context cntx=this.getApplicationContext();
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
	
	public void onCreate()
	{
		super.onCreate();
		Log.i("server", "Service created");
	}
	
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		Log.i("server", "Service started");
		receiver();		
		return START_STICKY;
	}
	
	public void onDestroy(){
		sock.close(); 
	    //System.out.println("File ricevuto");
	    super.onDestroy();
	}
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean verifyCoerency(InetAddress ia){
		if(!chunkVerify.containsKey(ia)){
			return false;
		}
		int number=chunkVerify.get(ia).size();
		for(int i=1;i<=number;i++){
			if(!chunkVerify.get(ia).contains(i)){
				return false;
			}
		}
		return true;
	}
	
	public byte[] reformPack(ArrayList<byte[]> chunks, ArrayList<Integer> order){
		ArrayList<Byte> recollect = new ArrayList<Byte>();
		for(int i=0;i<order.size();i++){
			int index=order.indexOf(i+1);
			for(int j=0;j<chunks.get(index).length;j++){
				recollect.add(new Byte(chunks.get(index)[j]));
			}
		}
		Byte[] toArray=(Byte[]) recollect.toArray();
		byte[] toReturn=new byte[toArray.length];
		for(int i=0;i<toArray.length;i++){
			toReturn[i]=toArray[i].byteValue();
		}
		return toReturn;
	}
	
	public void server(){
		chunkVerify=new HashMap<InetAddress, ArrayList<Integer>>();
		chunk=new HashMap<InetAddress, ArrayList<byte[]>>();
		waveHeaders=new HashMap<InetAddress,WaveHeader>();
		totalTrack=new HashMap<InetAddress,byte[]>();
		
		String waveHeadS=new String();
		int port = 10000, m=0;
		byte[] buck=new byte[1000];
		byte[] real;
		Packet pkt;
		sock=null;
		try{
			sock=new DatagramSocket(port);
			DatagramPacket pk=new DatagramPacket(buck,buck.length);
			while(true){
				sock.receive(pk);
				sock.setSoTimeout(1000);
				if(pk.getData()!=null){
					//real=Packet.recvTerminated(pk.getData());
					pkt=Packet.recoverData(pk.getData());
					//if(!pkt.testCrc())
						//continue;
					//if(pkt.control){
						/*if(chunkVerify.containsKey(pk.getAddress())){
							chunkVerify.remove(pk.getAddress());
							chunk.remove(pk.getAddress());
							waveHeaders.remove(pk.getAddress());
						}*/
						waveHeadS=new String(pkt.getData());
						wh=WaveHeader.parseString(waveHeadS);
						chunkVerify.put(pk.getAddress(), new ArrayList<Integer>());
						chunk.put(pk.getAddress(), new ArrayList<byte[]>());
						waveHeaders.put(pk.getAddress(), wh);
					//}
					//else{
						chunkVerify.get(pk.getAddress()).add(Integer.parseInt(new String(pkt.index)));
						chunk.get(pk.getAddress()).add(pkt.getData());
					//}
				}
				else{
					if(verifyCoerency(pk.getAddress())){
						byte[] total=reformPack(chunk.get(pk.getAddress()),chunkVerify.get(pk.getAddress()));
						totalTrack.put(pk.getAddress(),total);
						chunk.remove(pk.getAddress());
						chunkVerify.remove(pk.getAddress());
					}
				}
			}
		}catch(InterruptedIOException e){
			sendForSave();
		}catch(IOException e){
			Log.e("Server", "Error in socket bcast");
			//System.err.println("Error in socket broadcast");
			System.exit(1);
		}
	}
	
	public void sendForSave(){
		Wave toSave;
		WaveFileManager wfm;
		Set<InetAddress> ks=totalTrack.keySet();
		InetAddress[] ksa=(InetAddress[]) ks.toArray();
		for(int i=0;i<ksa.length;i++){
			if(waveHeaders.containsKey(ksa[i])){
				toSave=new Wave(waveHeaders.get(ksa[i]),totalTrack.get(ksa[i]));
				wfm=new WaveFileManager(toSave);
				wfm.saveWaveAsFile("receiveFrom"+ksa[i].toString()+".wav");
			}
		}
	}
	
}
