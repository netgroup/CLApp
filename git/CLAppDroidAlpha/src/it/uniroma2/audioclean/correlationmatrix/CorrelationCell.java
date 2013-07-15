package it.uniroma2.audioclean.correlationmatrix;

/**
 * Object cell for correlation matrix
 * @author Daniele De Angelis
 *
 */
public class CorrelationCell{
	public double correlation;
	public int delay;
	/**
	 * Constructor
	 */
	CorrelationCell(){}
	/**
	 * Constructor
	 * @param maxcorr
	 * 		correlation value
	 * @param offset
	 * 		offset for the shifting
	 */
	public CorrelationCell(double maxcorr, int offset){
		correlation=maxcorr;
		delay=offset;
	}
}
