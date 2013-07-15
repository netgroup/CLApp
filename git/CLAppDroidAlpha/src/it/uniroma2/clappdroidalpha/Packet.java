package it.uniroma2.clappdroidalpha;

/**
 * Class of standard packet structure
 * @author Daniele De Angelis
 *
 */
public class Packet {
	//bytes array of data
	private byte[] data;
	//bytes array containing the index of the chunk
	private byte[] index;
	//ensemble of index and data separated by a dot
	private byte[] overall;
	//boolean control=false;
	/**
	 * Constructor
	 * @param d
	 * 		Data
	 * @param ind
	 * 		Index
	 */
	Packet(byte[] d, int ind){
		data=d;
		//The index of type int is converted to String
		String str=Integer.toString(ind);
		index=str.getBytes();
	}
	
	/**
	 * Function to setup the overall array with the separator
	 */
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
		
	}
	
	/**
	 * Returns the packet index
	 * @return
	 * 		Index
	 */
	public byte[] getIndex(){
		return index;
	}
	
	/**
	 * Returns the packet array of data
	 * @return
	 * 		Data
	 */
	public byte[] getData(){
		return data;
	}
	
	/**
	 * Returns the overall array with index and data concatenated 
	 * @return
	 * 		Overall array
	 */
	public byte[] getOverall(){
		return overall;
	}

	/**
	 * Removes the final dot from an array of byte received from the net
	 * and returns a Packet with the data extracted
	 * @param b
	 * 		Array received from the net
	 * @return
	 * 		Packet filled with data extracted
	 */
	public static Packet recoverDataDot(byte[] b){
		//Here the final dot is removed
		byte[] bRev=recvTerminated(b);
		boolean indexRead=false, dataRead=false;
		byte[] data = null;
		int index = 0;
		String str=new String();
		String ch;
		
		//The bytes array is parsed to remove the divisor dot
		for(int i=0;i<bRev.length;i++){
			ch=new String(bRev,i,1);
			if(ch.compareTo(".")==0){
				//The index is identified and memorized
				if(!indexRead){
					try{
						index=Integer.parseInt(str);
						indexRead=true;
					}
					catch(NumberFormatException e){
						return null;
					}
				}
				//Data are memorized
				else if(!dataRead){
					data=str.getBytes();
					i=bRev.length;
					dataRead=true;
				}
				str=new String();
			}
			else
				str+=ch;
		}
		//The packet to return is declared and returned
		Packet p=new Packet(data,index);
		p.overall=bRev;
		return p;
	}

	/**
	 * Function to set up the string of byte with a dot on tail to be recognized
	 * by the parser
	 * @param data
	 * 		Bytes array
	 * @return
	 * 		Bytes array with final dot
	 */
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

	/**
	 * Function that parses a received array and removes the final dot inserted
	 * to understand when the data are finished
	 * @param data
	 * 		Array as received from the net
	 * @return
	 * 		An array without the final dot
	 * 		
	 */
	private static byte[] recvTerminated(byte[] data){
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
