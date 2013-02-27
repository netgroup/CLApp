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

}
