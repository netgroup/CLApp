package AudioCleaning;

import java.util.ArrayList;

public class Statistical {

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
	
	public static double meanpwr(float f[]){
		double result=0;
		int i;
		for(i=0;i<f.length;i++){
			result+=f[i];
		}
		return result/i;
	}
	
	public static double normalizedRMSE(float[] f1, float[] f2){
		double poweravgF1=meanpwr(f1);
		double poweravgF2=meanpwr(f2);
		
		for(int i=0;i<f2.length;i++){
			f2[i]*=(poweravgF1/poweravgF2);
		}
		return RMSE(f1,f2);
	}
	
	public static double RMSE(float[] f1, float[] f2){
		double result=0, sum=0;
		int i=0, j=0;
		
		for(i=0,j=0;i<f1.length && j<f2.length;i++,j++){
			sum=f1[i]-f2[j];
			result+=Math.pow(sum, 2);
		}
		result/=i<j ? i : j;
		result=Math.sqrt(result);
		return result;
	}
	
	public static double RMSE(float[] f1, float[] f2, float[] h, int index){
		double result=0, sum=0;
		int i=0, j=0;
		
		for(i=0,j=0;i<f1.length && j<f2.length;i++,j++){
			sum=(f1[i]/h[index])-f2[j];
			result+=Math.pow(sum, 2);
		}
		result/=i<j ? i : j;
		result=Math.sqrt(result);
		return result;
	}
	
	public static double RMSE(float[] f1, float[] f2, float[][] h, int indexT, int indexW){
		double result=0, sum=0;
		int i=0, j=0;
		
		for(i=0,j=0;i<f1.length && j<f2.length;i++,j++){
			sum=(f1[i]/h[indexT][indexW])-f2[j];
			result+=Math.pow(sum, 2);
		}
		result/=i<j ? i : j;
		result=Math.sqrt(result);
		return result;
	}
}