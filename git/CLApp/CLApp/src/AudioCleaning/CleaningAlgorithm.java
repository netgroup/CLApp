package AudioCleaning;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.text.DecimalFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.musicg.wave.Wave;
import com.musicg.graphic.*;
//Object cell of the correlation Matrix
class CorrelationCell{
	double correlation;
	int delay;
	
	CorrelationCell(){}
	
	CorrelationCell(double maxcorr, int offset){
		correlation=maxcorr;
		delay=offset;
	}
}
//Object Correlation Matrix
class CorrelationMatrix{
	CorrelationCell[][] matrix;
	int dimension;
	
	CorrelationMatrix(){
	}
	
	CorrelationMatrix(int i){
		matrix=new CorrelationCell[i][i];
		dimension=i;
	}
	//function to compute maximum row
	public int maxRow(){
		float max=0, sum=0;
		int indice=0;
		
		for(int i=0;i<dimension;i++){
			sum=0;
			for(int j=0;j<dimension;j++){
				sum+=matrix[i][j].correlation;
			}
			if(max<sum){
				max=sum;
				indice=i;
			}
		}
		
		return indice;
	}
	//function to draw the matrix
	public void stamp(){
		DecimalFormat f = new DecimalFormat("###0.00000");
		System.out.print("In the cells: correlation, delay.\n\n");
		for(int i=0; i<dimension;i++){
			System.out.print("| ");
			for(int j=0; j<dimension; j++){
				System.out.print("\t"+f.format(matrix[i][j].correlation)+", "+matrix[i][j].delay+"\t|");
			}
			System.out.println();
		}
		System.out.println("\n");
	}
}
//Object for Rank
class Ranking{
	int pos;
	double sigma;
	
	Ranking(){
		this.pos=0;
		this.sigma= 0.0;
	}
	
	Ranking(int pos, double h){
		this.pos=pos;
		this.sigma=h;
	}
}

public class CleaningAlgorithm{
	static float INF=Float.MAX_VALUE;
	public static final int SAMPLE_RATE = 44100;
	private static final double MAX_16_BIT = Short.MAX_VALUE;     // 32,767
	
	static String name;				//name for the saves
	static boolean WINDOW=false;	//boolean for the choice between WindowedAlgorithm or simple Algorithm
	static boolean stamp=true;	//boolean to activate some messages
	static int MAXDELAY=1350000;	//max delay for the cross correlation
	static short[] amplitude;		//amplitudes in short values
	static ArrayList<float[]> normalizedAmplitudes; //amplitude in float values
	static ArrayList<float[]> amplitudeReady;		 //amplitude in float values with same lenght
	static float[][] hToPlot;		//matrix with the h value (windows mode)
	
	
	/* converting array from short to double */
	public static void getNormalizedAmplitudes(int waveHeader) {
		boolean signed=true; 
		// usually 8bit is unsigned
		if (waveHeader==8){
			signed=false;
		}
		int numSamples = amplitude.length;
		int maxAmplitude = 1 << (waveHeader - 1);
		
		if (!signed){	// one more bit for unsigned value
			maxAmplitude<<=1;
		}
		/* normalizedAmplitudes: tracks type float */
		normalizedAmplitudes.add(new float[numSamples]);
		for (int i = 0; i < numSamples; i++) {
			normalizedAmplitudes.get(normalizedAmplitudes.size()-1)[i] = (float) amplitude[i] / maxAmplitude;
		}
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
	
	/* cross correlation to find the delay between the tracks, float version */
	public static CorrelationCell xcross(float[] fs, float[] fs2, int offset){
		double r=0, maxr=Double.NEGATIVE_INFINITY;
		int delay=0, delayS=0, bar=(MAXDELAY*2)/100;
		int rangeDelay=MAXDELAY<fs.length ? MAXDELAY : fs.length;
		
		/* Uncomment to activate stamp of the array values
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
		*/
		for(delay=-rangeDelay;delay<=rangeDelay;delay+=offset){
			//uncomment it to draw a progress bar
			//if(delay%bar==0 && stamp)
			//	System.out.print(">");
			r=crosscorrelation(fs,fs2,delay,offset);
			if(maxr<=r){
				maxr=r;
				delayS=delay;
			}
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
	
	/* function to extends all the traces to maximum lenght */
	private static void preparation(int longest){
		int size=normalizedAmplitudes.get(longest).length;
		float[] temp; 
		while(normalizedAmplitudes.size()>0){			
			temp= new float[size];
			for(int j=0;j<normalizedAmplitudes.get(0).length;j++){
				temp[j]=(normalizedAmplitudes.get(0)[j]);				
			}
			for(int j=normalizedAmplitudes.get(0).length;j<temp.length;j++){
				temp[j]=(float)0.0;
			}
			normalizedAmplitudes.remove(0);
			amplitudeReady.add(temp);
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
					for(int z=offset;z<amplitudeReady.get(j).length;z++){
						amplitudeReady.get(j)[k]=amplitudeReady.get(j)[z];
						k++;
					}
					//int z=0;
					//zeropadding "exceding" float
					for(;k<amplitudeReady.get(j).length;k++){
						amplitudeReady.get(j)[k]=0;
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
					m=amplitudeReady.get(j).length-1;
					for(int z=(amplitudeReady.get(j).length-1-(offset*(-1)));z>=0;z--){
						amplitudeReady.get(j)[m]=amplitudeReady.get(j)[z];
						amplitudeReady.get(j)[z]=0;
						m--;
					}
					//int z=sync.length-1;
					for(;m>=0;m--){
						amplitudeReady.get(j)[m]=0;
						//z--;
					}
				}
			}
		}
	}
	//Function to compute the covariance, float array version
	public static float covariance(float[] fs, float[] fs2){
		int n=fs.length;
		double avgA=0, avgB=0;
		float covariance=0;
		double x,z;
				
		for(int i=0; i<fs.length;i++){
			avgA+=fs[i];
			avgB+=fs2[i];
		}
		avgA=avgA/n;
		avgB=avgB/n;
		
		for(int i=0;i<n;i++){
			x=fs[i]-avgA;
			z=fs2[i]-avgB;
			covariance+=(x*z)/n;
		}
		return covariance;		
	}
	// Covariance, ArrayList<float[]> version
	public static float covariance(ArrayList<float[]> fs1, ArrayList<float[]> fs2){
		int n=0;
		double avgA=0, avgB=0;
		float covariance=0;
		double x,z;
				
		for(int i=0; i<fs1.size();i++){
			for(int j=0;j<fs1.get(i).length;j++){
				avgA+=fs1.get(i)[j];
				avgB+=fs2.get(i)[j];
				n++;
			}
		}
		avgA=avgA/n;
		avgB=avgB/n;
		
		for(int j=0;j<fs1.size();j++){
			for(int i=0;i<fs1.get(j).length;i++){
				x=fs1.get(j)[i]-avgA;
				z=fs2.get(j)[i]-avgB;
				covariance+=(x*z)/n;
			}
		}
		return covariance;		
	}
	
	// Function to compute variance value, float array version
	public static float variance(float[] a){
		int n=0;
		float sum1=0;
		float sum2=0;
		float mean=0;
		float variance;
		
		for(int i=0;i<a.length;i++){
			n++;
			sum1+=a[i];
		}
		mean= sum1/n;
		
		for(int i=0;i<a.length;i++){
			sum2+=(a[i]-mean)*(a[i]-mean);
		}
		variance=sum2/(n);
		return variance;
	}
	//Variance, matrix of float version
	public static float variance(float[][] a, int j){
		int n=0;
		float sum1=0;
		float sum2=0;
		float mean=0;
		float variance;
		
		for(int i=0;i<a[j].length;i++){
			n++;
			sum1+=a[j][i];
		}
		mean= sum1/n;
		
		for(int i=0;i<a[j].length;i++){
			sum2+=(a[j][i]-mean)*(a[j][i]-mean);
		}
		variance=sum2/(n-1);
		return variance;
	}
	// Variance, ArrayList<float[]> version
	public static float variance(ArrayList<float[]> a){
		int n=0;
		float sum1=0;
		float sum2=0;
		float mean=0;
		float variance;
		
		for(int j=0;j<a.size();j++){
			for(int i=0;i<a.get(j).length;i++){
				n++;
				sum1+=a.get(j)[i];
			}
		}
		mean= sum1/n;
		
		for(int j=0;j<a.size();j++){
			for(int i=0;i<a.get(j).length;i++){
				sum2+=(a.get(j)[i]-mean)*(a.get(j)[i]-mean);
			}
		}
		variance=sum2/(n-1);
		return variance;
	}
	//Computing the minimum value in a float array
	static int min(float[] valori){
		int k=0;
		float min=INF;
		for(int i=0; i<valori.length;i++){
			if(min>valori[i]){
				min=valori[i];
				k=i;
			}
		}
		return k;
	}
	//compute minimum value in a column of a float matrix
	private static int min(float[][] factors, int j){
		int k=0;
		float min=INF;
		for(int i=0; i<factors.length;i++){
			if(min>factors[i][j]){
				min=factors[i][j];
				k=i;
			}
		}
		return k;
	}
	//compute minimum value like the previous function, with doubles matrix
	@SuppressWarnings("unused")
	private static int min(double[][] factors, int j){
		int k=0;
		double min=INF;
		for(int i=0; i<factors.length;i++){
			if(min>factors[i][j]){
				min=factors[i][j];
				k=i;
			}
		}
		return k;
	}
	//Computes minimum trace with a Ranking Array
	@SuppressWarnings("unused")
	private static int minTrack(Ranking[] factors){
		int min=0;
		double value=INF;
		for(int i=1;i<factors.length;i++){
			if(value>factors[i].sigma){
				value=factors[i].sigma;
				min=factors[i].pos;
			}
		}
		return min;
	}
	//computes minimum trace with a 2-D Ranking Array
	@SuppressWarnings("unused")
	private static int minTrack(Ranking[][] factors, int j){
		int min=0;
		double  value=INF;
		for(int i=1;i<factors.length;i++){
			if(value>factors[i][j].sigma){
				value=factors[i][j].sigma;
				min=factors[i][j].pos;
			}
		}
		return min;
	}

	/* sorting tracks */
	private static void trackSort(Ranking[] factors) {
		Ranking temp;
		int j=0;
		while(j<factors.length-2){
			for(int i=0,z=1;i<(factors.length-1-j) && z<(factors.length-j);i++,z++){
				if(factors[i].sigma>factors[z].sigma){
					temp=factors[i];
					factors[i]=factors[z];
					factors[z]=temp;
				}			
			}
			j++;
		}
		
	}
	
	/* sorting tracks with windows */
	private static void trackSort(Ranking[][] factors, int k) {
		Ranking temp;
		int j=0;
		while(j<=factors.length-2){
			for(int i=0,z=1;i<(factors.length-1-j) && z<(factors.length-j);i++,z++){
				if(factors[i][k].sigma>factors[z][k].sigma){
					temp=factors[i][k];
					factors[i][k]=factors[z][k];
					factors[z][k]=temp;
				}			
			}
			j++;
		}
		
	}
	//Zeropadding track for the maximum lenght
	public static void zeroPadding(){
		int longest=longest(normalizedAmplitudes);
		preparation(longest);
		normalizedAmplitudes=null;
		amplitudeReady.trimToSize();
	}
	//Function to sync the traces
	public static int selectionSyncronization(int winLen){
		int offset=winLen;
		System.out.println("Longest track: "+amplitudeReady.get(0).length);
		/* cross correlation matrix creation */
		CorrelationMatrix matrix=new CorrelationMatrix(amplitudeReady.size());
		for(int i=0;i<amplitudeReady.size();i++){
			for(int j=0;j<amplitudeReady.size();j++){
				if(stamp)
					System.out.println("Tracks cross correlation:"+i+", "+j);
				matrix.matrix[i][j]=xcross(amplitudeReady.get(i),amplitudeReady.get(j), offset);
			}
		}
		System.out.print("Creating correlation matrix...\n\n");
		matrix.stamp();

		int ref=matrix.maxRow();
		/* syncing of the tracks refered to the track with maximum correlation*/
		System.out.println("Maximum correlation track� "+(ref+1));
		syncronization(matrix, ref);
		System.out.println("Tracks synced!\n");
		matrix=null;
		return ref;
	}
	//Tracks normalization
	public static void normalization(int ref){
		float c1=0, c2;
		//covariance between the first and the second track
		if(ref!=0)
			c2=covariance(amplitudeReady.get(ref),amplitudeReady.get(0));
		else
			c2=covariance(amplitudeReady.get(ref),amplitudeReady.get(1));
		float[] sigma=new float[amplitudeReady.size()-1];
		System.out.println("Covariance 0vs1: "+c2);
		
		float[] factors= new float[amplitudeReady.size()];
		
		//second track ranking initialization
		factors[1]=(float)1.0;
		System.out.println("h: "+factors[1]+"\n");
		
		//computing the other tracks h factor
		for(int i=0;i<amplitudeReady.size();i++){
			if(ref!=i){
				c1=covariance(amplitudeReady.get(ref),amplitudeReady.get(i));
				System.out.println("Covariance "+ref+"vs"+(i)+": "+c1);
				factors[i]=c2/c1;
				System.out.println("h: "+factors[i]+"\n");
			}
		}
		
		for(int i=0;i<amplitudeReady.size();i++){
			if(ref!=i){
				for(int j=0;j<amplitudeReady.get(i).length;j++){
					amplitudeReady.get(i)[j]*=factors[i];
				}
				if(i<ref)
					sigma[i]=variance(amplitudeReady.get(i));
				else
					sigma[i-1]=variance(amplitudeReady.get(i));
			}
		}
		
		//computing first track ranking with the track with lowest variance
		int k=min(sigma);
		if(k>=ref)
			k+=1;
		System.out.println("Min Track Temp: "+(k)+"\n");
		c1=covariance(amplitudeReady.get(k),amplitudeReady.get(ref));
		System.out.println("Covariance "+k+"vs"+ref+": "+c1);
		factors[ref]=c2/c1;
		System.out.println("h: "+factors[ref]+"\n");
		
		//for(int i=0;i<k;i++){
		for(int j=0;j<amplitudeReady.get(ref).length;j++){
			amplitudeReady.get(ref)[j]*=factors[ref];
		}
		//}
	}
	//Populates the Rankings Array
	public static Ranking[] ranking(){
		Ranking[] sigma=new Ranking[amplitudeReady.size()];
		
		for(int i=0;i<sigma.length;i++){
			sigma[i]=new Ranking();
			sigma[i].sigma=variance(amplitudeReady.get(i));
			sigma[i].pos=i;
		}
		trackSort(sigma);
		System.out.println("Ranking");
		for(int i=0; i<sigma.length; i++){
			System.out.println("Position "+i+"-> sigma^2: "+sigma[i].sigma+"; track: "+(sigma[i].pos+1));
		}
		return sigma;
	}
	//Computes the poweravg array
	public static float[] poweravg(Ranking[] factors){
		float temp[]=new float[amplitudeReady.get(0).length];
		float poweravg[]=new float[amplitudeReady.size()];
		
		int h=0;
		while(h<amplitudeReady.size()){
			int j=factors[h].pos;
			for(int i=0;i<amplitudeReady.get(j).length;i++){
				temp[i]+=(amplitudeReady.get(j)[i]);
				temp[i]/=(h+1);
			}
			poweravg[h]=variance(temp);
			for(int i=0;i<amplitudeReady.get(j).length;i++){
				temp[i]*=(h+1);
			}
			h++;
		}	
		return poweravg;
	}
	//Combining function for the final track
	public static float[] combining(Ranking[] factors, int k){
		int h=0;
		float[] temp=new float[amplitudeReady.get(0).length];
		
		System.out.println("Best track combining "+(k)+" tracks");
		
		int j=0;
		while(h<=k){
			j=factors[h].pos;
			for(int i=0;i<amplitudeReady.get(j).length;i++){
				temp[i]+=(amplitudeReady.get(j)[i]);
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
		
		for(int i=0;i<amplitudeReady.size();i++){
			for(int j=0;j<amplitudeReady.get(i).length;j++){
				amplitudeReady.get(i)[j]=(((k+1)*amplitudeReady.get(i)[j])-finalTrack[j])/(k+1);
			}
			save(i+"-errorNoWind.wav",convertFloatsToDoubles(amplitudeReady.get(i)));
			render=new Wave(i+"-errorNoWind.wav");
			r.renderWaveform(render, name+(i)+"errorNoWind-file.jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
		}
	}

	//Algorithm without windows
	public static void Algorithm(int ref){
		
		Ranking[] ranked;
		
		normalization(ref);
		// Sorting tracks
		ranked=ranking();
		
		// poweravg for all the tracks combination
		float poweravg[]=poweravg(ranked);
		
		System.out.println("Poweravg:\n");
		for(int i=0;i<poweravg.length;i++){
			System.out.print("Combining tracks ");
			for(int j=0;j<=i;j++){
				System.out.print(" "+(ranked[j].pos)+", ");
			}
			System.out.println(": "+poweravg[i]);
		}
		// computing lowest poweravg
		int k=min(poweravg);
		
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
		
		errorTrack(temp, k);
		
		Wave renders;
		GraphicRender r=new GraphicRender();
		
		save(name+"-nonwindowed.wav", convertFloatsToDoubles(temp));
		
		renders=new Wave(name+"-nonwindowed.wav");
		r.renderWaveform(renders, name+"-nonwindowed.jpg");
	
		System.out.println("Conversion completed!");
		return;
	}
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
		hToPlot=new float[windowed.size()][windows];
		
		// Xref
		if(ref!=0){
			System.out.println("Covariance "+ref+"vs0: ");
			for(int i=0;i<windows;i++){
				c2[i]=covariance(windowed.get(ref).get(i),windowed.get(0).get(i));
				factors[0][i]=1.0;
				System.out.println("Windows "+i+": covariance= "+c2[i]+". h= "+factors[0][i]);
			}
			System.out.println();
		}
		else{
			System.out.println("Covariance "+ref+"vs1: ");
			for(int i=0;i<windows;i++){
				c2[i]=covariance(windowed.get(ref).get(i),windowed.get(1).get(i));
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
					c1[j]=covariance(windowed.get(ref).get(j),windowed.get(i).get(j));
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
						sigma[i][j]=variance(windowed.get(i).get(j));
					else
						sigma[i-1][j]=variance(windowed.get(i).get(j));
				}
			}
		}
		
		/* computing the xref h value referred to the track with the lowest variance */
		int k[]=new int[windowed.get(0).size()];
		for(int i=0;i<k.length;i++){
			k[i]=min(sigma, i);
			if(i>=ref)
				k[i]+=1;
			System.out.println("Min temp windows "+i+": "+(k[i])+"; sigma^2 = "+sigma[k[i]-1][i]);
		}
		System.out.println();
		
		for(int i=0;i<windows;i++){
			System.out.println("Windows "+i+" -> Covariance "+k[i]+"vs"+ref+": ");
			c1[i]=covariance(windowed.get(k[i]).get(i),windowed.get(ref).get(i));
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
				hToPlot[i][j]=(float) factors[i][j];
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
			c2=covariance(windowed.get(0),windowed.get(1));
			factors[1]=1.0;
			System.out.println("covariance= "+c2+". h= "+factors[1]);
		//}
		System.out.println();
		
		/* computing the h factor referred to the xref track covariance */ 
		int test=windowed.size();
		for(int i=2; i < test; i++){
			System.out.println("Covariance 0vs"+i+": ");
			//for(int j=0;j<windows;j++){
				c1=covariance(windowed.get(0),windowed.get(i));
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
			sigma[i-1]=variance(windowed.get(i));
		}
		
		/* computing the xref h value referred to the track with the lowest variance */
		int k=0;
		//for(int i=0;i<k.length;i++){
			k=min(sigma);
			k+=1;
			System.out.println("Min : "+(k)+"; sigma^2 = "+sigma[k-1]);
		//}
		System.out.println();
		
		//for(int i=0;i<windows;i++){
			System.out.println("Covariance "+k+"vs0: ");
			c1=covariance(windowed.get(k),windowed.get(0));
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
				sigma[i][j].sigma=variance(windowed.get(i).get(j));
				sigma[i][j].pos=i;
			}
		}
		
		for(int i=0;i<sigma[0].length;i++){
			trackSort(sigma,i);
		}
		
		int windows=sigma[0].length;
		System.out.println("Ranking by windows\n");
		for(int j=0;j<windows;j++){
			System.out.println("Windows "+j);
			for(int i=0; i<sigma.length; i++){
				System.out.println("Position "+i+"-> sigma^2: "+sigma[i][j].sigma+"; track: "+(sigma[i][j].pos+1));
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
				poweravg[h][l]=variance(temp,l);
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
			for(int z=0;z<=m[j];z++){
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
	public static void errorTrackWindow(ArrayList<ArrayList<float[]>> windowed, float[][] finalTrack, int[] m){
		Wave render;
		GraphicRender r=new GraphicRender();
		//File remove;
		int windows=windowed.get(0).size();
		int winLen=windowed.get(0).get(1).length;
		int lastWin=windowed.get(0).get(windows-1).length;
		
		for(int i=0;i<windowed.size();i++){
			for(int j=0;j<windowed.get(i).size();j++){
				for(int z=0;z<windowed.get(i).get(j).length;z++){
					windowed.get(i).get(j)[z]=(((m[j]+1)*windowed.get(i).get(j)[z])-finalTrack[j][z])/(m[j]+1);
				}
			}
			save(i+"-errorWind.wav",convertFloatsToDoubles(fromWindowedToNormal(windowed.get(i),windows,winLen,lastWin)));
			render=new Wave(i+"-errorWind.wav");
			
			r.renderWaveform(render, name+(i)+"error-file-window"+windows+".jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
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
	public static void windowedAlgorithm(int ref){
		int windows=30;
		Ranking[][] ranked;
		ArrayList<ArrayList<float[]>> windowed;
		
		/* windows length */
		int winLen=(amplitudeReady.get(0).length/windows), lastWind=0;
		System.out.println("Windows activated.\n#windows: "+windows+"; window length: "+winLen);
		
		windowed=new ArrayList<ArrayList<float[]>>();
		windowsCreation(windowed, windows, winLen, lastWind);
		
		hToPlot=new float[windowed.size()][windows];
		normalizationWindows(windowed, windows,ref);
		
		/* sorting tracks */
		ranked=rankingWindows(windowed);		
				
		float poweravg[][]=poweravgWindows(windowed,ranked);
		
		/* computing best combine by windows */
		int[] m=new int[windows];
		for(int i=0;i<windows;i++){
			m[i]=min(poweravg, i);
		}
		for(int i=0;i<m.length;i++){
			for(int j=windowed.size()-1;j>m[i];j--){
				hToPlot[ranked[j][i].pos][i]=(float) 0.0;
			}
		}
		PlotH.Plotting(hToPlot);
		
		float[][] temp=combiningWindows(windowed, m, ranked);
		//windowed=null;
		
		errorTrackWindow(windowed,temp, m);
		
		/* converting tracks merging the windows */
		float[] finalTrack=fromWindowedToNormal(temp,windows,winLen,lastWind);
		
		save(name+"-windowed("+windows+").wav", convertFloatsToDoubles(finalTrack));
		System.out.println("Conversion completed!");
		return;
	}
	
	public static void main(String[] args) throws IOException{
		
		Wave input;
		int waveHead, index;
		normalizedAmplitudes=new ArrayList<float[]>();
		amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		if(args.length<5){
			System.err.println("Usage: java -jar cleaningAlgorithm.jar <head file output name> <windows enable (true or false)> <offset crosscorrelation> <path audiofile (min 2)>");
			return;
		}
		name=args[0];
		WINDOW=Boolean.parseBoolean(args[1]);
		offset=Integer.parseInt(args[2]);
		System.out.println("Tracks: "+(args.length-3));
		
		
		GraphicRender r=new GraphicRender();

		// creating the arrays to contain the tracks 
		for(int i=3;i<args.length;i++){
			input=new  Wave(args[i]);
			r.renderWaveform(input, args[i]+".jpg");
			waveHead=input.getWaveHeader().getBitsPerSample();
			amplitude=input.getSampleAmplitudes();
			getNormalizedAmplitudes(waveHead);
			
		}
		normalizedAmplitudes.trimToSize();
		zeroPadding();
		index=selectionSyncronization(offset);
				
		// windowed algorithm
		if(WINDOW){
			windowedAlgorithm(index);
		}
		else{
			System.out.println("Windows deactivated.\n");
			Algorithm(index);			
		}
	}
	
	public static double[] convertFloatsToDoubles(float[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    double[] output = new double[input.length];
	    for (int i = 0; i < input.length; i++)
	    {
	        output[i] = input[i];
	    }
	    return output;
	}
	
	
	/**
     * Save the double array as a sound file (using .wav or .au format).
     * Fonte: http://introcs.cs.princeton.edu/java/stdlib/StdAudio.java.html
     */
    public static void save(String filename, double[] input) {

        // assumes 44,100 samples per second
        // use 16-bit audio, mono, signed PCM, little Endian
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        byte[] data = new byte[2 * input.length];
        for (int i = 0; i < input.length; i++) {
            int temp = (short) (input[i] * MAX_16_BIT);
            data[2*i + 0] = (byte) temp;
            data[2*i + 1] = (byte) (temp >> 8);
        }

        // now save the file
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            AudioInputStream ais = new AudioInputStream(bais, format, input.length);
            if (filename.endsWith(".wav") || filename.endsWith(".WAV")) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
            }
            else if (filename.endsWith(".au") || filename.endsWith(".AU")) {
                AudioSystem.write(ais, AudioFileFormat.Type.AU, new File(filename));
            }
            else {
                throw new RuntimeException("File format not supported: " + filename);
            }
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }
	

}