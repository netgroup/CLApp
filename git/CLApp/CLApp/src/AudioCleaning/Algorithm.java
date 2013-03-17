package AudioCleaning;

import java.io.File;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class Algorithm {

	static float[] hToPlot;

	//Tracks normalization
	public static void normalization(int ref){
		float c1=0, c2;
		hToPlot=new float[CleaningAlgorithm.amplitudeReady.size()];
		
		//covariance between the first and the second track
		if(ref!=0)
			c2=Statistical.covariance(CleaningAlgorithm.amplitudeReady.get(ref),CleaningAlgorithm.amplitudeReady.get(0));
		else
			c2=Statistical.covariance(CleaningAlgorithm.amplitudeReady.get(ref),CleaningAlgorithm.amplitudeReady.get(1));
		float[] sigma=new float[CleaningAlgorithm.amplitudeReady.size()-1];
		System.out.println("Covariance 0vs1: "+c2);
		float[] factors= new float[CleaningAlgorithm.amplitudeReady.size()];
		//second track ranking initialization
		factors[1]=(float)1.0;
		System.out.println("h: "+factors[1]+"\n");
		//computing the other tracks h factor
		for(int i=0;i<CleaningAlgorithm.amplitudeReady.size();i++){
			if(ref!=i){
				c1=Statistical.covariance(CleaningAlgorithm.amplitudeReady.get(ref),CleaningAlgorithm.amplitudeReady.get(i));
				System.out.println("Covariance "+ref+"vs"+(i)+": "+c1);
				factors[i]=c2/c1;
				System.out.println("h: "+factors[i]+"\n");
			}
		}
		for(int i=0;i<CleaningAlgorithm.amplitudeReady.size();i++){
			if(ref!=i){
				for(int j=0;j<CleaningAlgorithm.amplitudeReady.get(i).length;j++){
					CleaningAlgorithm.amplitudeReady.get(i)[j]*=factors[i];
				}
				if(i<ref)
					sigma[i]=Statistical.variance(CleaningAlgorithm.amplitudeReady.get(i));
				else
					sigma[i-1]=Statistical.variance(CleaningAlgorithm.amplitudeReady.get(i));
			}
		}
		//computing first track ranking with the track with lowest variance
		int k=SortingTools.min(sigma);
		if(k>=ref)
			k+=1;
		System.out.println("Min Track Temp: "+(k)+"\n");
		c1=Statistical.covariance(CleaningAlgorithm.amplitudeReady.get(k),CleaningAlgorithm.amplitudeReady.get(ref));
		System.out.println("Covariance "+k+"vs"+ref+": "+c1);
		factors[ref]=c2/c1;
		System.out.println("h: "+factors[ref]+"\n");
		//for(int i=0;i<k;i++){
		for(int j=0;j<CleaningAlgorithm.amplitudeReady.get(ref).length;j++){
			CleaningAlgorithm.amplitudeReady.get(ref)[j]*=factors[ref];
		}
		hToPlot=factors;
		//}
	}

	//Populates the Rankings Array
	public static Ranking[] ranking(){
		Ranking[] sigma=new Ranking[CleaningAlgorithm.amplitudeReady.size()];
		
		for(int i=0;i<sigma.length;i++){
			sigma[i]=new Ranking();
			sigma[i].sigma=Statistical.variance(CleaningAlgorithm.amplitudeReady.get(i));
			sigma[i].pos=i;
		}
		SortingTools.trackSort(sigma);
		System.out.println("Ranking");
		for(int i=0; i<sigma.length; i++){
			System.out.println("Position "+i+"-> sigma^2: "+sigma[i].sigma+"; track: "+(sigma[i].pos));
		}
		return sigma;
	}

	//Computes the poweravg array
	public static float[] poweravg(Ranking[] factors){
		float temp[]=new float[CleaningAlgorithm.amplitudeReady.get(0).length];
		float poweravg[]=new float[CleaningAlgorithm.amplitudeReady.size()];
		
		int h=0;
		while(h<CleaningAlgorithm.amplitudeReady.size()){
			int j=factors[h].pos;
			for(int i=0;i<CleaningAlgorithm.amplitudeReady.get(j).length;i++){
				temp[i]+=(CleaningAlgorithm.amplitudeReady.get(j)[i]);
				temp[i]/=(h+1);
			}
			poweravg[h]=Statistical.variance(temp);
			for(int i=0;i<CleaningAlgorithm.amplitudeReady.get(j).length;i++){
				temp[i]*=(h+1);
			}
			h++;
		}	
		return poweravg;
	}

	//Combining function for the final track
	public static float[] combining(Ranking[] factors, int k){
		int h=0;
		float[] temp=new float[CleaningAlgorithm.amplitudeReady.get(0).length];
		System.out.println("Best track combining "+(k)+" tracks");
		int j=0;
		while(h<=k){
			j=factors[h].pos;
			for(int i=0;i<CleaningAlgorithm.amplitudeReady.get(j).length;i++){
				temp[i]+=(CleaningAlgorithm.amplitudeReady.get(j)[i]);
			}
			h++;
		}
		for(int i=0;i<temp.length;i++){
			temp[i]/=(h);
		}
		return temp;
	}

	//Function to compute the error removed
	public static void errorTrack(float[] finalTrack, int k){
		Wave render;
		GraphicRender r=new GraphicRender();
		File remove;
	
		for(int i=0;i<CleaningAlgorithm.amplitudeReady.size();i++){
			for(int j=0;j<CleaningAlgorithm.amplitudeReady.get(i).length;j++){
				CleaningAlgorithm.amplitudeReady.get(i)[j]=(((k+1)*CleaningAlgorithm.amplitudeReady.get(i)[j])-finalTrack[j])/(k+1);
			}
			WaveManipulation.save(i+"-errorNoWind.wav",WaveManipulation.convertFloatsToDoubles(CleaningAlgorithm.amplitudeReady.get(i)));
			render=new Wave(i+"-errorNoWind.wav");
			r.renderWaveform(render, CleaningAlgorithm.name+(i)+"errorNoWind-file.jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
		}
	}

	//Algorithm without windows
	public static void algorithm(int ref, int secondRef){
		Ranking[] ranked;
		
		normalization(ref);
		// Sorting tracks
		ranked=ranking();
		// poweravg for all the tracks combination
		float poweravg[]=poweravg(ranked);
		System.out.println("Poweravg:");
		for(int i=0;i<poweravg.length;i++){
			System.out.print("Combining tracks ");
			for(int j=0;j<=i;j++){
				System.out.print(" "+(ranked[j].pos)+", ");
			}
			System.out.println(": "+poweravg[i]);
		}
		// computing lowest poweravg
		int k=SortingTools.min(poweravg);
		System.out.print("Combination with lowest poweravg: ");
		for(int i=0; i<=k; i++){
			System.out.print("track "+(ranked[i].pos));
			if(i<k){
				System.out.print(" + ");
			}
		}
		System.out.println();
		//TODO
		//save("test.wav", convertFloatsToDoubles(amplitudeReady.get(0)));
		float[] temp=combining(ranked, k);
		for(int i=0;i<CleaningAlgorithm.amplitudeReady.size();i++){
			System.out.println("RMSE Track "+i+": "+Statistical.RMSE(temp, CleaningAlgorithm.amplitudeReady.get(i)));
		}
		errorTrack(temp, k);
		Wave renders;
		GraphicRender r=new GraphicRender();
		WaveManipulation.save(CleaningAlgorithm.name+"-nonwindowed.wav", WaveManipulation.convertFloatsToDoubles(temp));
		renders=new Wave(CleaningAlgorithm.name+"-nonwindowed.wav");
		r.renderWaveform(renders, CleaningAlgorithm.name+"-nonwindowed.jpg");
		System.out.println("Conversion completed!");
		return;
	}

}
