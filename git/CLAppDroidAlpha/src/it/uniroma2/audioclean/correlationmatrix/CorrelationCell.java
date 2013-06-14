package it.uniroma2.audioclean.correlationmatrix;

//Object cell of the correlation Matrix
public class CorrelationCell{
	public double correlation;
	public int delay;
	
	CorrelationCell(){}
	public CorrelationCell(double maxcorr, int offset){
		correlation=maxcorr;
		delay=offset;
	}
}
