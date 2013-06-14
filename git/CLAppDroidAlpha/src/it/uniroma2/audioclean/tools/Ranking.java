package it.uniroma2.audioclean.tools;

public class Ranking {
	public int pos;
	public double sigma;
	
	public Ranking(){
		this.pos=0;
		this.sigma= 0.0;
	}
	Ranking(int pos, double h){
		this.pos=pos;
		this.sigma=h;
	}
}
