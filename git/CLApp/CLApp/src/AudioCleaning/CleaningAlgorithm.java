package AudioCleaning;

import java.io.IOException;
import java.util.ArrayList;
import java.text.DecimalFormat;
import com.musicg.wave.Wave;
import com.musicg.graphic.*;

public class CleaningAlgorithm{
	static float INF=Float.MAX_VALUE;
	public static String name;				//name for the saves
	static boolean WINDOW=false;	//boolean for the choice between WindowedAlgorithm or simple Algorithm
	public static short[] amplitude;		//amplitudes in short values
	public static ArrayList<float[]> normalizedAmplitudes; //amplitude in float values
	public static ArrayList<float[]> amplitudeReady;		 //amplitude in float values with same lenght
	static float[][] hToPlotW;		//matrix with the h value (windows mode)
	//Function for the windows creation
	public static void windowsCreation(ArrayList<ArrayList<float[]>> windowed, int windows , int winLen, int lastWind){
				
		// windows creation
		while(amplitudeReady.size()>0){
			windowed.add(new ArrayList<float[]>());			
			int lasting=amplitudeReady.get(0).length;
			for(int j=0;j<windows;j++){				
				if(lasting>=winLen){
					windowed.get(windowed.size()-1).add(new float[winLen]);
					lasting-=winLen;
				}
				else{
					windowed.get(windowed.size()-1).add(new float[lasting]);
					lastWind=lasting;
					lasting=0;
				}					
				for(int i=0;i<windowed.get(windowed.size()-1).get(windowed.get(windowed.size()-1).size()-1).length;i++){
					windowed.get(windowed.size()-1).get(j)[i]=amplitudeReady.get(0)[winLen*j+i];
				}				
			}
			windowed.get(windowed.size()-1).trimToSize();
			amplitudeReady.remove(0);
		}
		amplitudeReady=null;
		windowed.trimToSize();
	}
	
	//Normalization, windows version: different covariance for any windows
	public static void normalizationWindows(ArrayList<ArrayList<float[]>> windowed, int windows, int ref){
		float[] c1=new float[windows];
		float[] c2=new float[windows];
		float[][] sigma=new float[windowed.size()-1][windows];
		double[][] factors= new double[windowed.size()][windows];
		hToPlotW=new float[windowed.size()][windows];
		// Xref
		if(ref!=0){
			System.out.println("Covariance "+ref+"vs0: ");
			for(int i=0;i<windows;i++){
				c2[i]=Statistical.covariance(windowed.get(ref).get(i),windowed.get(0).get(i));
				factors[0][i]=1.0;
				System.out.println("Windows "+i+": covariance= "+c2[i]+". h= "+factors[0][i]);
			}
			System.out.println();
		}
		else{
			System.out.println("Covariance "+ref+"vs1: ");
			for(int i=0;i<windows;i++){
				c2[i]=Statistical.covariance(windowed.get(ref).get(i),windowed.get(1).get(i));
				factors[1][i]=1.0;
				System.out.println("Windows "+i+": covariance= "+c2[i]+". h= "+factors[1][i]);
			}
			System.out.println();
		}
		/* computing the h factor referred to the xref track covariance */ 
		int test=windowed.size();
		for(int i=0; i < test; i++){
			if(ref!=i){
				System.out.println("Covariance "+ref+"vs"+i+": ");
				for(int j=0;j<windows;j++){
					c1[j]=Statistical.covariance(windowed.get(ref).get(j),windowed.get(i).get(j));
					factors[i][j]=c2[j]/c1[j];
					System.out.println("Windows "+j+": covariance= "+c1[j]*factors[i][j]+". h= "+factors[i][j]);
				}
				System.out.println();
			}
		}
		for(int i=0;i<windowed.size();i++){
			if(i!=ref){
				for(int j=0;j<windowed.get(i).size();j++){
					for(int z=0;z<windowed.get(i).get(j).length;z++){
						windowed.get(i).get(j)[z]*=factors[i][j];
					}
					if(i<ref)
						sigma[i][j]=Statistical.variance(windowed.get(i).get(j));
					else
						sigma[i-1][j]=Statistical.variance(windowed.get(i).get(j));
				}
			}
		}
		/* computing the xref h value referred to the track with the lowest variance */
		int k[]=new int[windowed.get(0).size()];
		for(int i=0;i<k.length;i++){
			k[i]=SortingTools.min(sigma, i);
			if(k[i]>=ref){
				k[i]+=1;
				System.out.println("Min temp windows "+i+": "+(k[i])+"; sigma^2 = "+sigma[k[i]-1][i]);
			}
			else 
				System.out.println("Min temp windows "+i+": "+(k[i])+"; sigma^2 = "+sigma[k[i]][i]);
		}
		System.out.println();
		for(int i=0;i<windows;i++){
			System.out.println("Windows "+i+" -> Covariance "+k[i]+"vs"+ref+": ");
			c1[i]=Statistical.covariance(windowed.get(k[i]).get(i),windowed.get(ref).get(i));
			factors[ref][i]=c2[i]/c1[i];
			System.out.println("covariance= "+c1[i]*factors[ref][i]+", h= "+factors[ref][i]);
		}
		System.out.println();
		for(int i=0;i<windowed.get(ref).size();i++){
			for(int j=0;j<windowed.get(ref).get(i).length;j++){
				windowed.get(ref).get(i)[j]*=factors[ref][i];
			}
		}
		for(int i=0;i<factors.length; i++){
			for(int j=0;j<factors[i].length;j++){
				hToPlotW[i][j]=(float) factors[i][j];
			}
		}
	}
	
	//Normalization, windows version: same covariance for any windows
	public static int normalizationWindows2(ArrayList<ArrayList<float[]>> windowed, int windows){
		float c1;
		float c2;
		float[] sigma=new float[windowed.size()-1];
		double[] factors= new double[windowed.size()];
		
		// Xref
		System.out.println("Covariance 0vs1: ");
		//for(int i=0;i<windows;i++){
			c2=Statistical.covariance(windowed.get(0),windowed.get(1));
			factors[1]=1.0;
			System.out.println("covariance= "+c2+". h= "+factors[1]);
		//}
		System.out.println();
		/* computing the h factor referred to the xref track covariance */ 
		int test=windowed.size();
		for(int i=2; i < test; i++){
			System.out.println("Covariance 0vs"+i+": ");
			//for(int j=0;j<windows;j++){
				c1=Statistical.covariance(windowed.get(0),windowed.get(i));
				factors[i]=c2/c1;
				System.out.println("covariance= "+c1*factors[i]+". h= "+factors[i]);
			//}
			System.out.println();
		}
		for(int i=1;i<windowed.size();i++){
			for(int j=0;j<windowed.get(i).size();j++){
				for(int z=0;z<windowed.get(i).get(j).length;z++){
					windowed.get(i).get(j)[z]*=factors[i];
				}
			}
			sigma[i-1]=Statistical.variance(windowed.get(i));
		}
		/* computing the xref h value referred to the track with the lowest variance */
		int k=0;
		//for(int i=0;i<k.length;i++){
			k=SortingTools.min(sigma);
			k+=1;
			System.out.println("Min : "+(k)+"; sigma^2 = "+sigma[k-1]);
		//}
		System.out.println();
		//for(int i=0;i<windows;i++){
			System.out.println("Covariance "+k+"vs0: ");
			c1=Statistical.covariance(windowed.get(k),windowed.get(0));
			factors[0]=c2/c1;
			System.out.println("covariance= "+c2*factors[0]+", h= "+factors[0]);
		//}
		System.out.println();
		for(int i=0;i<windowed.get(0).size();i++){
			for(int j=0;j<windowed.get(0).get(i).length;j++){
				windowed.get(0).get(i)[j]*=factors[0];
			}
		}
		return k;
	}
	
	//Compute the Ranking matrix
	public static Ranking[][] rankingWindows(ArrayList<ArrayList<float[]>> windowed){
		Ranking[][] sigma=new Ranking[windowed.size()][windowed.get(0).size()];
		
		for(int i=0;i<windowed.size();i++){
			for(int j=0; j<windowed.get(0).size(); j++){
				sigma[i][j]=new Ranking();
				sigma[i][j].sigma=Statistical.variance(windowed.get(i).get(j));
				sigma[i][j].pos=i;
			}
		}
		for(int i=0;i<sigma[0].length;i++){
			SortingTools.trackSort(sigma,i);
		}
		int windows=sigma[0].length;
		System.out.println("Ranking by windows\n");
		for(int j=0;j<windows;j++){
			System.out.println("Windows "+j);
			for(int i=0; i<sigma.length; i++){
				System.out.println("Position "+i+"-> sigma^2: "+sigma[i][j].sigma+"; track: "+(sigma[i][j].pos));
			}
			System.out.println();
		}
		return sigma;
	}
	
	//Computing the poweravg matrix
	public static float[][] poweravgWindows(ArrayList<ArrayList<float[]>> windowed, Ranking[][] rank){
		int windows=windowed.get(0).size(), winLen=windowed.get(0).get(0).length;
		float poweravg[][]=new float[windowed.size()][windows];
		float temp[][]=new float[windows][winLen]; /* TODO change it to double */
		/* computing poweravg for all the windows with any combination */
		int h=0;
		while(h<windowed.size()){
			for(int l=0;l<windows;l++){
				int j=rank[h][l].pos;
				for(int m=0;m<windowed.get(j).get(l).length;m++){
					temp[l][m]+=(windowed.get(j).get(l)[m]);
					temp[l][m]/=(h+1);
				}
			}
			for(int l=0;l<windows;l++)
				poweravg[h][l]=Statistical.variance(temp,l);
			for(int l=0;l<windows;l++){
				int j=rank[h][l].pos;
				for(int m=0;m<windowed.get(j).get(l).length;m++){
					temp[l][m]*=(h+1);
				}
			}
			h++;
		}
		return poweravg;
	}
	//Combining window for the final track
	public static float[][] combiningWindows(ArrayList<ArrayList<float[]>> windowed, int[] m, Ranking[][] ranked){
		int j=0, h=0;
		float[][] temp=new float[windowed.get(0).size()][windowed.get(0).get(0).length];
		
		System.out.println("Best combine per windows:");
		for(int i=0; i<m.length;i++){
			System.out.print("Window "+i+", ");
			for(int z=0;z<=m[i];z++){
				 System.out.print("track "+ranked[z][i].pos+" + ");
			}
			System.out.println();
		}
		while(j<m.length){
			h=0;
			int l=0;
			while(h<=m[j]){
				//System.out.println("Window: "+(j+1)+"; Numero di tracce combinate: "+m[j]);
				l=ranked[h][j].pos;
				for(int i=0;i<windowed.get(l).get(j).length;i++){
					temp[j][i]+=(windowed.get(l).get(j)[i]);
				}
				h++;
			}
			for(int i=0;i<temp[j].length;i++){
				temp[j][i]/=(h);
			}
			j++;
		}
		return temp;
	}
	
	//Computes the error, one window at time
	public static void errorTrackWindow(ArrayList<float[][]> tracks, float[][] finalTrack, int[] bestCombo){
		Wave render;
		GraphicRender r=new GraphicRender();
		//File remove;
		int windows=tracks.get(0).length;
		int winLen=tracks.get(0)[0].length;
		int lastWin=tracks.get(0)[windows-1].length;
		float[] temp=new float[windows*(winLen-1)+lastWin];
		int multiplier=0;
		
		for(int i=0;i<tracks.size();i++){
			for(int j=0;j<windows;j++){
				for(int z=0;z<tracks.get(i)[j].length;z++){
					temp[multiplier+z]=(((bestCombo[j]+1)*tracks.get(i)[j][z])-finalTrack[j][z])/(bestCombo[j]+1);
				}
				multiplier+=winLen;
			}
			WaveManipulation.save(i+"-errorWind.wav",WaveManipulation.convertFloatsToDoubles(temp));
			render=new Wave(i+"-errorWind.wav");
			
			r.renderWaveform(render, name+(i)+"error-file-window"+windows+".jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
			multiplier=0;
			temp=new float[windows*(winLen-1)+lastWin];
		}
	}
	
	//Converts a windowed track into a normal track
	public static float[] fromWindowedToNormal(float[][] temp, int windows, int winLen, int lastWind){
		float[] finalTrack=new float[(winLen*(windows-1))+lastWind];
		int o=0;
		
		for(int i=0;i<windows;i++){
			if(i==windows-1){
				for(int g=0;g<lastWind;g++){
					finalTrack[o]=temp[i][g];
					o++;
				}
			}
			else{
				for(int g=0;g<winLen;g++){
					finalTrack[o]=temp[i][g];
					o++;
				}
			}	
		}
		return finalTrack;
	}
	
	//Like the previous function, ArrayList<float[]> version
	public static float[] fromWindowedToNormal(ArrayList<float[]> temp, int windows, int winLen, int lastWind){
		float[] finalTrack=new float[(winLen*(windows-1))+lastWind];
		int o=0;
		
		for(int i=0;i<windows;i++){
			if(i==windows-1){
				for(int g=0;g<lastWind;g++){
					finalTrack[o]=temp.get(i)[g];
					o++;
				}
			}
			else{
				for(int g=0;g<winLen;g++){
					finalTrack[o]=temp.get(i)[g];
					o++;
				}
			}	
		}
		return finalTrack;
	}
	
	//Windowed Algorithm version
	public static void windowedAlgorithm(int ref) throws IOException{
		int windows=AlgorithmWindows.computeNumWindows(1);
		Ranking[][] ranked;
		ArrayList<ArrayList<float[]>> windowed;
		
		/* windows length */
		int winLen=(amplitudeReady.get(0).length/windows), lastWind=0;
		System.out.println("Windows activated.\n#windows: "+windows+"; window length: "+winLen);
		windowed=new ArrayList<ArrayList<float[]>>();
		windowsCreation(windowed, windows, winLen, lastWind);
		hToPlotW=new float[windowed.size()][windows];
		normalizationWindows(windowed, windows,ref);
		/* sorting tracks */
		ranked=rankingWindows(windowed);		
		float poweravg[][]=poweravgWindows(windowed,ranked);
		/* computing best combine by windows */
		int[] m=new int[windows];
		for(int i=0;i<windows;i++){
			m[i]=SortingTools.min(poweravg, i);
		}
		for(int i=0;i<m.length;i++){
			for(int j=windowed.size()-1;j>m[i];j--){
				hToPlotW[ranked[j][i].pos][i]=(float) 0.0;
			}
		}
		//PlotH.Plotting(hToPlotW);
		float[] data;
		for(int i=0;i<windowed.size();i++){
			//System.out.print("RMSE Track "+i+" by windows: {");
			DataFile.fileCreate(hToPlotW[i], "hplot"+i);
			//System.out.println("}");
		}
		float[][] temp=combiningWindows(windowed, m, ranked);
		WaveManipulation.amplitudeNormalization(temp);
		//windowed=null;
		for(int i=0;i<windowed.size();i++){
			//System.out.print("RMSE Track "+i+" by windows: {");
			data=new float[windowed.get(0).size()];
			for(int j=0;j<windowed.get(i).size();j++){
				data[j]=(float) Statistical.normalizedRMSE(temp[j], windowed.get(i).get(j));
			}
			DataFile.fileCreate(data, "dataRMSE"+i);
			//System.out.println("}");
		}
		float[] finalTrack=fromWindowedToNormal(temp,windows,winLen,lastWind);
		float[] RMSETot=new float[windowed.size()];
		for(int i=0;i<windowed.size();i++){
//			RMSETot[i]=(float) Statistical.RMSETotW(temp, windowed.get(i));
		}
		//ATTENTION: errorTrackWindows could modify windowed values!!
//		errorTrackWindow(windowed,temp, m);
		/* converting tracks merging the windows */
		WaveManipulation.save(name+"-windowed("+windows+").wav", WaveManipulation.convertFloatsToDoubles(finalTrack));
		System.out.println("Conversion completed!");
		return;
	}
	
	public static void main(String[] args) throws IOException{
		Wave input;
		int waveHead, index;
		normalizedAmplitudes=new ArrayList<float[]>();
		amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		// Input verification
		if(args.length<5){
			System.err.println("Usage: java -jar cleaningAlgorithm.jar <head file output name> <windows enable (true or false)> <offset crosscorrelation> <path audiofile (min 2)>");
			return;
		}
		name=args[0]; 							// Reading file name
		WINDOW=Boolean.parseBoolean(args[1]);	// Reading windows choice
		offset=Integer.parseInt(args[2]);		// Reading cross correlation offset 
		Syncing.stamp=false;
		System.out.println("Tracks: "+(args.length-3));
		GraphicRender r=new GraphicRender();
		// creating the arrays to contain the tracks 
		for(int i=3;i<args.length;i++){
			input=new  Wave(args[i]);
			r.renderWaveform(input, args[i]+".jpg");
			waveHead=input.getWaveHeader().getBitsPerSample();
			amplitude=input.getSampleAmplitudes();
			WaveManipulation.getNormalizedAmplitudes(waveHead);
			
		}
		normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		index=Syncing.selectionSyncronization(offset);	
		// windowed algorithm
		if(WINDOW){
			AlgorithmWindows.algorithm(index);
			//windowedAlgorithm(index);
		}
		else{ // basic algorithm
			System.out.println("Windows deactivated.\n");
			Algorithm.algorithm(index);			
		}
	}
}