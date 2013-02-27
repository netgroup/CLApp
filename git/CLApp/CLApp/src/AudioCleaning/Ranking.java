package AudioCleaning;

public class Ranking {
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
