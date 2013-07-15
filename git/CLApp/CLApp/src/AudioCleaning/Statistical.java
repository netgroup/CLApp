package AudioCleaning;

/**
 * Class containing statistical functions
 * @author Daniele De Angelis
 *
 */
public class Statistical {

	/**
	 * Compute covariance between two floats array
	 * @param fs
	 * 		First array
	 * @param fs2
	 * 		Second array
	 * @return
	 * 		Covariance value
	 */
	public static float covariance(float[] fs, float[] fs2){
		int n=fs.length;
		double avgA=0, avgB=0;
		float covariance=0;
		double x,z;
		
		//average
		for(int i=0; i<fs.length;i++){
			avgA+=fs[i];
			avgB+=fs2[i];
		}
		avgA=avgA/n;
		avgB=avgB/n;
		//applying covariance function
		for(int i=0;i<n;i++){
			x=fs[i]-avgA;
			z=fs2[i]-avgB;
			covariance+=(x*z)/n;
		}
		return covariance;		
	}


	/**
	 * Function that computes the variance value (array of floats version)
	 * @param a
	 * 		Data
	 * @return
	 * 		Variance value
	 */
	public static float variance(float[] a){
		int n=0;
		float sum1=0;
		float sum2=0;
		float mean=0;
		float variance;
		
		//average
		for(int i=0;i<a.length;i++){
			n++;
			sum1+=a[i];
		}
		mean= sum1/n;
		
		//Variance function
		for(int i=0;i<a.length;i++){
			sum2+=(a[i]-mean)*(a[i]-mean);
		}
		variance=sum2/(n);
		return variance;
	}

	/**
	 * Function that computes the variance value (bidimensional array of floats version)
	 * @param a
	 * 		Bidimensional array of floats
	 * @param j
	 * 		Index of data to compute
	 * @return
	 * 		Variance value
	 */
	public static float variance(float[][] a, int j){
		int n=0;
		float sum1=0;
		float sum2=0;
		float mean=0;
		float variance;
		
		//average
		for(int i=0;i<a[j].length;i++){
			n++;
			sum1+=a[j][i];
		}
		mean= sum1/n;
		
		//variance function
		for(int i=0;i<a[j].length;i++){
			sum2+=(a[j][i]-mean)*(a[j][i]-mean);
		}
		variance=sum2/(n-1);
		return variance;
	}
	
	
	/**
	 * Function that computes the RMSE between two track windows
	 * denormalizing it from the h value coefficient of the cleaning algorithm
	 * @param f1
	 * 		Track 1
	 * @param f2
	 * 		Track 2
	 * @return
	 * 		RMSE value
	 */
	public static double normalizedRMSE(float[] f1, float[] f2){
		double poweravgF1=avg_mod(f1);
		double poweravgF2=avg_mod(f2);
		float[] f2temp=new float[f2.length];
		
		for(int i=0;i<f2.length;i++){
			f2temp[i]=f2[i];
			f2temp[i]*=(poweravgF1/poweravgF2);
		}
		return RMSE(f1,f2temp);
	}
	
	/**
	 * Function that computes the RMSE between two track windowed
	 * denormalizing it from the h value coefficient of the cleaning algorithm
	 * @param f1
	 * 		Track 1 windowed
	 * @param f2
	 * 		Track 2 windowed
	 * @return
	 * 		RMSE value
	 */
	public static double normalizedRMSETotW(float[][] f1, float[][] f2){
		double poweravgF1;
		double poweravgF2;
		float[][] f2temp=new float[f2.length][];
		
		for(int i=0;i<f2.length;i++){
			poweravgF1=avg_mod(f1[i]);
			poweravgF2=avg_mod(f2[i]);
			f2temp[i]=new float[f2[i].length];
			for(int j=0;j<f2[i].length;j++){
				f2temp[i][j]=f2[i][j];
				f2temp[i][j]*=(poweravgF1/poweravgF2);
			}
		}
		return RMSETotW(f1,f2temp);
	}
	
	/**
	 * Computes RMS of a track
	 * @param f
	 * 		Track
	 * @return
	 * 		RMS value
	 */
	protected static double RMS(float[] f){
		double result=0;
		int i=0;
		
		for(i=0;i<f.length;i++){
			result+=Math.pow(f[i], 2);
		}
		result/=i;
		result=Math.sqrt(result);
		return result;
	}
	
	/**
	 * Computes the absolute average
	 * @param f
	 * 		Data
	 * @return
	 * 		Absolute average
	 */
	 protected static double avg_mod(float[] f){
		double result=0;
		
		for(int i=0;i<f.length;i++){
			result+=Math.abs(f[i]);
		}
		return result/((double) f.length);
	}
	
	/**
	 * Computes RMSE between two array of floats
	 * @param f1
	 * 		Array 1
	 * @param f2
	 * 		Array 2
	 * @return
	 * 		RMSE value
	 */
	private static double RMSE(float[] f1, float[] f2){
		double result=0, sum=0;
		int i=0, j=0;
		
		for(i=0,j=0;i<f1.length && j<f2.length;i++,j++){
			sum=f1[i]-f2[j];
			if(!Double.isNaN(sum))
				result+=Math.pow(sum, 2);
		}
		if(!Double.isNaN(result)){
			result/=i<j ? i : j;
			result=Math.sqrt(result);
		}
		
		return result;
	}
	
	/**
	 * Computes RMSE between two bidimensional array of floats
	 * @param f1
	 * 		Array 1
	 * @param f2
	 * 		Array 2
	 * @return
	 * 		RMSE value
	 */
	private static double RMSETotW(float[][] f1, float[][] f2){
		double result=0, sum=0;
		int i=0, j=0, z=0, s=0, tot=0;
		
		for(z=0,s=0;z<f1.length && s<f2.length;z++,s++){
			for(i=0,j=0;i<f1[z].length && j<f2[s].length;i++,j++){
				sum=f1[z][i]-f2[s][j];
				tot++;
				if(!Double.isNaN(sum))
					result+=Math.pow(sum, 2);
			}
		}
		if(!Double.isNaN(result)){
			result/=tot;
			result=Math.sqrt(result);
		}
		return result;
	}
}
