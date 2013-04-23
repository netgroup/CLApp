package com.CLApp;

import java.util.zip.CRC32;

public class Packet {
	byte[] data;
	byte[] crc;
	byte[] index;
	byte[] overall;
	boolean control=false;
	
	Packet(byte[] d, int ind){
		data=d;
		String str=Integer.toString(ind);
		index=str.getBytes();
	}
	
	public static Packet recoverData(byte[] b){
		//overall=b;
		boolean indexRead=false, dataRead=false;
		byte[] data = null, crc;
		int index = 0;
		String str=new String();
		String ch;
		for(int i=0;i<b.length;i++){
			ch=new String(b,i,1);
			if(ch.compareTo(".")==0){
				if(!indexRead){
					index=Integer.parseInt(str);
				}
				else if(!dataRead){
					data=str.getBytes();
				}
				str=new String();
			}
			else
				str+=ch;
		}
		crc=str.getBytes();
		Packet p=new Packet(data,index);
		p.crc=crc;
		p.overall=b;
		if(p.index.equals((Integer.toString(index)).getBytes())){
			p.control=true;
		}
		return p;
	}
	
	public Packet(byte[] data, boolean ctrl){
		this.data=data;
		control=ctrl;
		if(ctrl){
			int ind=0;
			String str=Integer.toString(ind);
			index=str.getBytes();
		}
	}
	
	public void computeCrc(){
		CRC32 com=new CRC32();
		com.update(data);
		long numCrc=com.getValue();
		String strCrc=Long.toString(numCrc);
		crc=strCrc.getBytes();
	}
	
	public void makePack(){
		byte[] point=".".getBytes();
		int j=0;
		overall=new byte[index.length+data.length+crc.length+2*point.length];
		int i=0;
		for(i=0;i<index.length;i++){
			overall[i]=index[i];
		}
		i=j=index.length;
		for(;i<(j+point.length);i++){
			overall[i]=point[i-j];
		}
		i=j=index.length+point.length;
		for(;i<(data.length+j);i++){
			overall[i]=data[i-j];
		}
		i=j=index.length+data.length+point.length;
		for(;i<(j+point.length);i++){
			overall[i]=point[i-j];
		}
		i=index.length+data.length+2*point.length;
		for(;i<(j+crc.length);i++){
			overall[i]=crc[i-j];
		}
	}
	
	public boolean testCrc(){
		CRC32 newCrc=new CRC32();
		long oldCrcValue;
		long newCrcValue;
		String parseCrc=new String();
		//String charact=new String();
		//for(int i=0;i<crc.length;i++){
			parseCrc=new String(crc);
		//}
		oldCrcValue=Long.parseLong(parseCrc);
		newCrc.update(data);
		newCrcValue=newCrc.getValue();
		if(oldCrcValue==newCrcValue)
			return true;
		else
			return false;
	}
	
	public byte[] getIndex(){
		return index;
	}
	
	public byte[] getData(){
		return data;
	}
	
	public byte[] getCrc(){
		return crc;
	}
	
	public byte[] getOverall(){
		return overall;
	}
	
	public static byte[] terminate(byte[] data){
		byte[] point=".".getBytes();
		byte[] term=new byte[data.length+point.length];
		for(int i=0;i<term.length;i++){
			if(i<data.length){
				term[i]=data[i];
			}
			else{
				term[i]=point[i-data.length];
			}
		}
		return term;
	}
	
	public static byte[] recvTerminated(byte[] data){
		byte[] term = null;
		boolean ctrl=false;
		for(int i=data.length-1;i>=0;i--){
			if(".".compareTo(new String(data,i,1))==0 && !ctrl){
				term=new byte[i];
				ctrl=true;
			}
			else if(ctrl){
				term[i]=data[i];
			}
		}
		return term;
	}

}
