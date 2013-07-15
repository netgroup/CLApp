package it.uniroma2.audioclean;

import it.uniroma2.audioclean.correlationmatrix.CorrelationCell;
import it.uniroma2.audioclean.correlationmatrix.CorrelationMatrix;

import java.util.ArrayList;

public class Syncing {

	public static boolean stamp=true;	//boolean to activate some messages
	static float INF=Float.MAX_VALUE;
	static int MAXDELAY=CleaningAlgorithm.SAMPLE_RATE*2;	//max delay for the cross correlation
	
	/**
	 * Function that do the shifting between the tracks and computes their correlation
	 * @param fs
	 * 		Track 1
	 * @param fs2
	 * 		Track 2
	 * @param offset
	 * 		Offset between one shifting and another
	 * @return
	 * 		An object CorrelationCell containing the best shifting value and his correlation value
	 */
	private static CorrelationCell xcross(float[] fs, float[] fs2, int offset){
		double r=0, maxr=Double.NEGATIVE_INFINITY;
		int delay=0, delayS=0;
		int rangeDelay=MAXDELAY<fs.length ? MAXDELAY : fs.length;
		
		//Loop that do the shiftings
		for(delay=-rangeDelay;delay<=rangeDelay;delay+=offset){
			r=crosscorrelation(fs,fs2,delay,offset);
			if(maxr<=r){
				maxr=r;
				delayS=delay;
			}
			if(stamp)
				System.out.println("delay:"+delay+" \tr:"+r);
		}
		if(stamp)
			System.out.println("\n");
		return new CorrelationCell(maxr,delayS);
	}

	/**
	 * Correlation between two tracks
	 * @param fs
	 * 		Track 1
	 * @param fs2
	 * 		Track 2
	 * @param delay
	 * 		Delay value between the tracks
	 * @param offset
	 * 		Offset of the shiftings
	 * @return
	 * 		The correlation value
	 */
	private static double crosscorrelation(float[] fs, float[] fs2, int delay, int offset){
		double sumAB=0, sumA=0, sumB=0, avgA=0, avgB=0, denom=0, r;
		int count=0;
		int j=0;
		/* avg */
		for(int z=0; z<fs.length; z+=offset){
			j=z;
	        count++;
	        avgA+=fs[z]+1;
	        avgB+=fs2[j]+1;
		}
		j=0;
		avgA=avgA/(count);
		avgB=avgB/(count);
		
		/* do autocorrelation */
		for(int i=0; i<fs.length; i+=offset){
			j=i;
	        sumA+=(fs[i]+1-avgA)*(fs[i]+1-avgA);
	        sumB+=(fs2[j]+1-avgB)*(fs2[j]+1-avgB);
		}
		denom=Math.sqrt(sumA*sumB);
		j=0;
		for(int i=0;i<fs.length;i+=offset){
			j=i+delay;
			/* Possible alternative
			 * if(j<0||j>=fs.length){
				sumAB+=(fs[i]+1-avgA)*(1-avgB);
			}
			else{
				sumAB+=(fs[i]+1-avgA)*(fs2[j]+1-avgB);
			}*/
			if (j < 0 || j >= fs.length)
				continue;
	        else{
	    		sumAB+=(fs[i]+1-avgA)*(fs2[j]+1-avgB);
	        }
		}
		r=sumAB/denom;
		return r;
	}

	/**
	 * Function to zeropad the tracks in CleaningAlgorithm
	 */
	public static void zeroPadding(){
		int longest=longest(CleaningAlgorithm.normalizedAmplitudes);
		preparation(longest);
		CleaningAlgorithm.normalizedAmplitudes=null;
		CleaningAlgorithm.amplitudeReady.trimToSize();
	}

	/**
	 * function to extends all the traces to maximum lenght
	 * @param longest
	 * 		Longest track size
	 */
	private static void preparation(int longest){
		int size=CleaningAlgorithm.normalizedAmplitudes.get(longest).length;
		float[] temp; 
		while(CleaningAlgorithm.normalizedAmplitudes.size()>0){			
			temp= new float[size];
			for(int j=0;j<CleaningAlgorithm.normalizedAmplitudes.get(0).length;j++){
				temp[j]=(CleaningAlgorithm.normalizedAmplitudes.get(0)[j]);				
			}
			for(int j=CleaningAlgorithm.normalizedAmplitudes.get(0).length;j<temp.length;j++){
				temp[j]=(float)0.0;
			}
			CleaningAlgorithm.normalizedAmplitudes.remove(0);
			CleaningAlgorithm.amplitudeReady.add(temp);
		}
	}

	/**
	 * Function to find the longest track size
	 * @param normalizedAmplitudes2
	 * 		Set of tracks
	 * @return
	 * 		Longest size
	 */
	private static int longest(ArrayList<float[]> normalizedAmplitudes2){
		int longest=0;
		int size=0;
		for(int i=0;i<normalizedAmplitudes2.size();i++){
			if(size<normalizedAmplitudes2.get(i).length){
				longest=i;
				size=normalizedAmplitudes2.get(i).length;
			}
		}
		return longest;
	}

	/**
	 * Function that with the correlation matrix synchronizes all the tracks
	 * @param mx
	 * 		Correlation matrix
	 * @param i
	 * 		Reference track index
	 */
	private static void syncronization(CorrelationMatrix mx, int i){
		int offset;
		float[] sync;
		//Loop over the entire row, picking one track at time
		for(int j=0;j<mx.dimension;j++){
			//If the track itself, do nothing
			if(j==i){
				continue;
			}
			else{
				//If the track is early, the code delays it
				offset=mx.matrix[i][j].delay;
				if(offset>=0){
					int k=0;
					//shifting the vector
					for(int z=offset;z<CleaningAlgorithm.amplitudeReady.get(j).length;z++){
						CleaningAlgorithm.amplitudeReady.get(j)[k]=CleaningAlgorithm.amplitudeReady.get(j)[z];
						k++;
					}
					//zeropadding "exceding" float
					for(;k<CleaningAlgorithm.amplitudeReady.get(j).length;k++){
						CleaningAlgorithm.amplitudeReady.get(j)[k]=0;
					}
				}
				//The track is delayed
				else{
					//same actions done before
					sync=new float[offset*(-1)];
					int m=sync.length-1;
					m=CleaningAlgorithm.amplitudeReady.get(j).length-1;
					for(int z=(CleaningAlgorithm.amplitudeReady.get(j).length-1-(offset*(-1)));z>=0;z--){
						CleaningAlgorithm.amplitudeReady.get(j)[m]=CleaningAlgorithm.amplitudeReady.get(j)[z];
						CleaningAlgorithm.amplitudeReady.get(j)[z]=0;
						m--;
					}
					for(;m>=0;m--){
						CleaningAlgorithm.amplitudeReady.get(j)[m]=0;
					}
				}
			}
		}
	}

	/**
	 * Function that manage all the synchronization process
	 * @param winLen
	 * 		Offset for the cross correlation
	 * @return
	 * 		An array with only two elements: the index of the first best track and the index of the second
	 */
	public static int[] selectionSyncronization(int winLen){
		int offset=winLen;
		System.out.println("Longest track: "+CleaningAlgorithm.amplitudeReady.get(0).length);
		/* cross correlation matrix creation */
		CorrelationMatrix matrix=new CorrelationMatrix(CleaningAlgorithm.amplitudeReady.size());
		for(int i=0;i<CleaningAlgorithm.amplitudeReady.size();i++){
			for(int j=0;j<CleaningAlgorithm.amplitudeReady.size();j++){
				System.out.println("Tracks cross correlation:"+i+", "+j);
				matrix.matrix[i][j]=Syncing.xcross(CleaningAlgorithm.amplitudeReady.get(i),CleaningAlgorithm.amplitudeReady.get(j), offset);
			}
		}
		System.out.print("Creating correlation matrix...\n\n");
		matrix.stamp();
	
		int[] ref=new int[2];
		ref[0]=matrix.maxRow();
		ref[1]=matrix.secondMaxRow();
		/* syncing of the tracks refered to the track with maximum correlation*/
		System.out.println("Maximum correlation track "+(ref[0])+", second: "+ref[1]);
		Syncing.syncronization(matrix, ref[0]);
		System.out.println("Tracks synced!\n");
		matrix=null;
		return ref;
	}
	
}
