package Test;

import java.io.File;
import java.util.ArrayList;
import AudioCleaning.*;
import CorrelationMatrix.CorrelationMatrix;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class ErrorGraph {
	
	private static int longest(double[] f, double[] s){
		if(f.length>s.length) return 0;
		else return 1;
	}
	
	private static double[] preparation(double[] d, int length){
		int size=length;
		double[] temp; 			
		temp = new double[size];
		for(int j=0;j<d.length;j++){
			temp[j]=d[j];				
		}
		for(int j=d.length;j<temp.length;j++){
			temp[j]=(double)0.0;
		}
		return temp;				
	}
	
	public static void syncronization(CorrelationMatrix mx, int i, double[] f, double[] s){
		
		int offset;
		float[] sync;
		
		if(i==0){
			offset=mx.matrix[0][1].delay;
			if(offset>=0){
				/*sync=new float[offset]; 
				for(int z=0;z<offset;z++){
					sync[z]=amplitudeReady.get(j)[z];
				}*/
				int k=0;
				//shifting the vector
				for(int z=offset;z<s.length;z++){
					s[k]=s[z];
					k++;
				}
				//int z=0;
				//zeropadding "exceding" float
				for(;k<s.length;k++){
					s[k]=0;
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
				m=s.length-1;
				for(int z=(s.length-1-(offset*(-1)));z>=0;z--){
					s[m]=s[z];
					s[z]=0;
					m--;
				}
				//int z=sync.length-1;
				for(;m>=0;m--){
					s[m]=0;
					//z--;
				}
			}
			
			
		}
		else{
			offset=mx.matrix[1][0].delay;
			
			
			if(offset>=0){
				/*sync=new float[offset]; 
				for(int z=0;z<offset;z++){
					sync[z]=amplitudeReady.get(j)[z];
				}*/
				int k=0;
				//shifting the vector
				for(int z=offset;z<f.length;z++){
					f[k]=f[z];
					k++;
				}
				//int z=0;
				//zeropadding "exceding" float
				for(;k<f.length;k++){
					f[k]=0;
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
				m=f.length-1;
				for(int z=(f.length-1-(offset*(-1)));z>=0;z--){
					f[m]=f[z];
					f[z]=0;
					m--;
				}
				//int z=sync.length-1;
				for(;m>=0;m--){
					f[m]=0;
					//z--;
				}
			}
			
			
		}
	}
	
	public static void selectionSyncronization(double[] f, double[] s, int winLen){
		int offset=winLen;
		/* cross correlation matrix creation */
		CorrelationMatrix matrix=new CorrelationMatrix(2);
		matrix.matrix[0][0]=Syncing.xcross(f, f , offset);
		matrix.matrix[0][1]=Syncing.xcross(f, s, offset);
		matrix.matrix[1][0]=Syncing.xcross(s, f , offset);
		matrix.matrix[1][1]=Syncing.xcross(s, s, offset);
		System.out.print("Creazione matrice di correlazione:\n\n");
		matrix.stamp();

		int ref=matrix.maxRow();
		/* syncing of the tracks refered to the track with maximum correlation*/
		System.out.println("Maximum correlation track� "+(ref+1));
		syncronization(matrix, ref,f, s);
		System.out.println("Tracks synced!\n");
		matrix=null;
	
	}
	
	public static void main(String[] args){
		Wave first, second, render;
		int winLen=1000;
		double[] f, s;
		String name;
		File remove;
		GraphicRender r=new GraphicRender();
		
		if(args.length<3){
			System.err.println("Usage: java -jar errorGraph.jar <Wav 1> <Wav 2> <name file>");
			return;
		}
		
		first=new Wave(args[0]);
		second=new Wave(args[1]);
		name=args[2];
		f=first.getNormalizedAmplitudes();
		s=second.getNormalizedAmplitudes();
		
		if(longest(f,s)==0) s=preparation(s, f.length);
		else f=preparation(f, s.length);
		
		selectionSyncronization(f,s,winLen);
		
		for(int i=0;i<f.length;i++){
			f[i]-=s[i];
		}
		
		WaveManipulation.save("error.wav", f);
		render=new Wave("error.wav");
		r.renderWaveform(render, name+"-error-removed.jpg");
		remove=new File("error.wav");
		remove.delete();
	}

}
