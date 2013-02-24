package AudioCleaning;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class RenderSingleTrack {
	
	public static void main(String[] args){
		Wave a;
		GraphicRender r=new GraphicRender();
				
		if(args.length!=1){
			System.err.println("Usage: java -jar renderize.jar <path file .wav>");
			return;
		}
		
		a=new Wave(args[0]);
		r.renderWaveform(a, args[0]+"-render.jpg");
	}

}
