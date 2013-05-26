package it.uniroma2.clappdroidalpha;

//Class used to standardize the packet structure
public class Packet {
	//bytes array of data
	byte[] data;
	//byte[] crc;
	//bytes array containing the index of the chunk
	byte[] index;
	//ensemble of index and data separated by a dot
	byte[] overall;
	//boolean control=false;
	
	//Constructor: it takes the array of data and the chunk number
	Packet(byte[] d, int ind){
		data=d;
		//The index of type int is converted to String
		String str=Integer.toString(ind);
		index=str.getBytes();
	}
	
	//Static function to return an object Packet from the string of byte 
	//received on the net
	public static Packet recoverData(byte[] b){
		//overall=b;
		//Boolean for functions of control
		boolean indexRead=false, dataRead=false;
		byte[] data = null; //crc;
		int index = 0;
		String str=new String();
		String ch;
		//The bytes array is parsed to remove the divisor dot
		for(int i=0;i<b.length;i++){
			ch=new String(b,i,1);
			if(ch.compareTo(".")==0){
				//The index is identified and memorized
				if(!indexRead){
					index=Integer.parseInt(str);
				}
				else if(!dataRead){
					data=str.getBytes();
					i=b.length;
				}
				str=new String();
			}
			else
				str+=ch;
		}
		//crc=str.getBytes();
		//The packet to return is declared and returned
		Packet p=new Packet(data,index);
		//p.crc=crc;
		p.overall=b;
		return p;
	}
	
	public static Packet recoverDataDot(byte[] b){
		//overall=b;
		//Boolean for functions of control
		byte[] bRev=recvTerminated(b);
		boolean indexRead=false, dataRead=false;
		byte[] data = null; //crc;
		int index = 0;
		String str=new String();
		String ch;
		//The bytes array is parsed to remove the divisor dot
		for(int i=0;i<bRev.length;i++){
			ch=new String(bRev,i,1);
			if(ch.compareTo(".")==0){
				//The index is identified and memorized
				if(!indexRead){
					index=Integer.parseInt(str);
				}
				else if(!dataRead){
					data=str.getBytes();
					i=bRev.length;
				}
				str=new String();
			}
			else
				str+=ch;
		}
		//crc=str.getBytes();
		//The packet to return is declared and returned
		Packet p=new Packet(data,index);
		//p.crc=crc;
		p.overall=bRev;
		return p;
	}
	
	/*public Packet(byte[] data, boolean ctrl){
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
	}*/
	
	//Function to setup the overall array with the separator
	public void makePack(){
		byte[] point=".".getBytes();
		int j=0;
		overall=new byte[index.length+data.length+1*point.length];
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
		/*
		i=j=index.length+data.length+point.length;
		for(;i<(j+point.length);i++){
			overall[i]=point[i-j];
		}
		i=index.length+data.length+2*point.length;
		for(;i<(j+crc.length);i++){
			overall[i]=crc[i-j];
		}*/
	}
	/*
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
	*/
	
	//Return the index of the object packet 
	public byte[] getIndex(){
		return index;
	}
	
	//return the data of the object packet
	public byte[] getData(){
		return data;
	}
	/*
	public byte[] getCrc(){
		return crc;
	}
	*/
	
	//return the overall bytes array
	public byte[] getOverall(){
		return overall;
	}
	
	//Function to set up the string of byte with a dot to be recognized
	//by the server parser
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
	
	//Function to recover the data from the string of bytes terminated by the dot
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
