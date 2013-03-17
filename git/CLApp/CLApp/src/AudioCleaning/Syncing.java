package AudioCleaning;

import java.io.File;
import java.util.ArrayList;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

import CorrelationMatrix.CorrelationCell;
import CorrelationMatrix.CorrelationMatrix;

public class Syncing {

	public static boolean stamp=true;	//boolean to activate some messages
	static int MAXDELAY=1350000;	//max delay for the cross correlation
	/* cross correlation to find the delay between the tracks, float version */
	public static CorrelationCell xcross(float[] fs, float[] fs2, int offset){
		double r=0, maxr=Double.NEGATIVE_INFINITY;
		int delay=0, delayS=0, bar=(MAXDELAY*2)/100;
		int rangeDelay=MAXDELAY<fs.length ? MAXDELAY : fs.length;
		
		if(stamp){
			System.out.println("fs");
			for(int i=0;i<fs.length;i++){
				System.out.println(fs[i]+1);
			}
			System.out.println("\n");
		
			System.out.println("fs2");
			for(int i=0;i<fs.length;i++){
				System.out.println(fs2[i]+1);
			}
			System.out.println("\n");
		}
		for(delay=-rangeDelay;delay<=rangeDelay;delay+=offset){
			//uncomment it to draw a progress bar
			//if(delay%bar==0 && stamp)
				//System.out.print(">");
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

	/* cross correlation to find the delay between the tracks, double version */
	public static CorrelationCell xcross(double[] fs, double[] fs2, int offset) {
		double r, maxr=Double.NEGATIVE_INFINITY;
		int delay=0, delayS=0, bar=(MAXDELAY*2)/100;
		int rangeDelay=MAXDELAY<fs.length ? MAXDELAY : fs.length;
		
		for(delay=-rangeDelay;delay<=rangeDelay;delay+=offset){
			if(delay%bar==0 && stamp)
				System.out.print(">");
			r=crosscorrelation(fs,fs2,delay,offset);
			if(maxr<r){
				maxr=r;
				delayS=delay;
			}
		}
		if(stamp)
			System.out.println("\n");
		return new CorrelationCell(maxr,delayS);
	}

	//function to compute the correlation in double values
	public static double crosscorrelation(double[] fs, double[] fs2, int delay, int offset){
		double sumAB=0, sumA=0, sumB=0, avgA=0, avgB=0, denom=0, r;
		int count=0;
		int j=0;
		/* avg */
		for(int z=0; z<fs.length; z+=offset){
			j=z;
	        count++;
	        avgA+=fs[z];
	        avgB+=fs2[j];
		}
		j=0;
		avgA=avgA/(count);
		avgB=avgB/(count);
		for(int i=0; i<fs.length; i+=offset){
			j=i;
	        sumA+=(fs[i]-avgA)*(fs[i]-avgA);
	        sumB+=(fs2[j]-avgB)*(fs2[j]-avgB);
		}
		denom=Math.sqrt(sumA*sumB);
		j=0;
		for(int i=0;i<fs.length;i+=offset){
			j=i+delay;
			if (j < 0 || j >= fs.length)
				continue;
	        else{   	
	    		sumAB+=(fs[i]-avgA)*(fs2[j]-avgB);
	        }
		}
		r=sumAB/denom;
		return r;
	}

	public static double crosscorrelation(float[] fs, float[] fs2, int delay, int offset){
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

	//Zeropadding track for the maximum lenght
	public static void zeroPadding(){
		int longest=longest(CleaningAlgorithm.normalizedAmplitudes);
		preparation(longest);
		CleaningAlgorithm.normalizedAmplitudes=null;
		CleaningAlgorithm.amplitudeReady.trimToSize();
	}

	/* function to extends all the traces to maximum lenght */
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

	/* Function to find the longest track */
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

	//Function to sync the traces
	public static void syncronization(CorrelationMatrix mx, int i){
		int offset;
		float[] sync;
		
		for(int j=0;j<mx.dimension;j++){
			if(j==i){
				continue;
			}
			else{
				offset=mx.matrix[i][j].delay;
				if(offset>=0){
					/*sync=new float[offset]; 
					for(int z=0;z<offset;z++){
						sync[z]=amplitudeReady.get(j)[z];
					}*/
					int k=0;
					//shifting the vector
					for(int z=offset;z<CleaningAlgorithm.amplitudeReady.get(j).length;z++){
						CleaningAlgorithm.amplitudeReady.get(j)[k]=CleaningAlgorithm.amplitudeReady.get(j)[z];
						k++;
					}
					//int z=0;
					//zeropadding "exceding" float
					for(;k<CleaningAlgorithm.amplitudeReady.get(j).length;k++){
						CleaningAlgorithm.amplitudeReady.get(j)[k]=0;
						//z++;
					}
				}
				else{
					//same actions done before
					sync=new float[offset*(-1)];
					int m=sync.length-1;
					/*for(int z=(amplitudeReady.get(j).length)-1;z>(amplitudeReady.get(j).length-offset);z--){
						sync[m]=amplitudeReady.get(j)[z];
						m--;
					}*/
					m=CleaningAlgorithm.amplitudeReady.get(j).length-1;
					for(int z=(CleaningAlgorithm.amplitudeReady.get(j).length-1-(offset*(-1)));z>=0;z--){
						CleaningAlgorithm.amplitudeReady.get(j)[m]=CleaningAlgorithm.amplitudeReady.get(j)[z];
						CleaningAlgorithm.amplitudeReady.get(j)[z]=0;
						m--;
					}
					//int z=sync.length-1;
					for(;m>=0;m--){
						CleaningAlgorithm.amplitudeReady.get(j)[m]=0;
						//z--;
					}
				}
			}
		}
	}

	//Function to sync the traces
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
		for(int i=0;i<CleaningAlgorithm.amplitudeReady.size();i++){
			Syncing.stampSynced(CleaningAlgorithm.amplitudeReady.get(i), "synced"+i+".jpg");
		}
		matrix=null;
		return ref;
	}

	static float INF=Float.MAX_VALUE;
	
	public static void stampSynced(float[] t, String name){
		Wave render;
		GraphicRender r=new GraphicRender();
		File f;
		
		WaveManipulation.save("temp.wav", WaveManipulation.convertFloatsToDoubles(t));
		render=new Wave("temp.wav");
		r.renderWaveform(render, name);
		f=new File("temp.wav");
		f.delete();
	}

}
