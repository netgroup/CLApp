package it.uniroma2.clappdroidalpha;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.util.Log;

public class AudioRecorder 
{
	private final static int[] sampleRates = {44100, 22050, 11025, 8000};
	
	public static AudioRecorder getInstance(Boolean recordingCompressed)
	{
		AudioRecorder result = null;
		
		if(recordingCompressed)
		{
			result = new AudioRecorder(	false, 
											AudioSource.MIC, 
											sampleRates[3], 
											AudioFormat.CHANNEL_IN_MONO,
											AudioFormat.ENCODING_PCM_16BIT);
		}
		else
		{
			int i=0;
			do
			{
				result = new AudioRecorder(	true, 
												AudioSource.MIC, 
												sampleRates[i], 
												AudioFormat.CHANNEL_IN_MONO,
												AudioFormat.ENCODING_PCM_16BIT);
				
			} while((++i<sampleRates.length) & !(result.getState() == AudioRecorder.State.INITIALIZING));
		}
		return result;
	}
	
	/**
	* INITIALIZING : recorder is initializing;
	* READY : recorder has been initialized, recorder not yet started
	* RECORDING : recording
	* ERROR : reconstruction needed
	* STOPPED: reset needed
	*/
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED};
	
	public static final boolean RECORDING_UNCOMPRESSED = true;
	public static final boolean RECORDING_COMPRESSED = false;
	
	// The interval in which the recorded samples are output to the file
	// Used only in uncompressed mode
	@SuppressWarnings("unused")
	private static final int TIMER_INTERVAL = 120;
	
	// Toggles uncompressed recording on/off; RECORDING_UNCOMPRESSED / RECORDING_COMPRESSED
	private boolean         rUncompressed;
	
	// Recorder used for uncompressed recording
	private AudioRecord     audioRecorder = null;
	
	// Recorder used for compressed recording
	private MediaRecorder   mediaRecorder = null;
	
	// Stores current amplitude (only in uncompressed mode)
	private int             cAmplitude= 0;
	
	// Output file path
	private String          filePath = null;
	
	// Recorder state; see State
	private State          	state;
	
	// File writer (only in uncompressed mode)
	public RandomAccessFile headerFile;
	//private ThreadSender ts;
	//private ThreadListen tl;
	private MainService st;
	//TODO For debug
	private RandomAccessFile testFile;
	
	// Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
	private short                    nChannels;
	private int                      sRate;
	private short                    bSamples;
	private int                      bufferSize;
	private int                      aSource;
	private int                      aFormat;
	
	// Number of frames written to file on each output(only in uncompressed mode)
	private int                      framePeriod;
	
	// Buffer for output(only in uncompressed mode)
	private byte[]                   buffer;
	
	// Number of bytes written to file after header(only in uncompressed mode)
	// after stop() is called, this size is written to the header/data chunk in the wave file
	private int                      payloadSize;
	
	/**
	*
	* Returns the state of the recorder in a RehearsalAudioRecord.State typed object.
	* Useful, as no exceptions are thrown.
	*
	* @return recorder state
	*/
	public State getState()
	{
		return state;
	}
	
	/*
	*
	* Method used for recording.
	*
	*/
	private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener()
	{
		public void onPeriodicNotification(AudioRecord recorder)
		{
			
			audioRecorder.read(buffer, 0, buffer.length); // Fill buffer
			// File output kept to check the difference between the 
			//filtered and the unfiltered data
			try {
				testFile.write(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			//Sending data to clean
			st.record.lock();
			if(st.dataRecorded!=null)
				st.dataRecorded.add(buffer.clone());
			st.record.unlock();
			
			//Sending data to send
			st.send.lock();
			if(st.toSend!=null)
				st.toSend.add(buffer.clone());
			st.send.unlock();
			
			payloadSize += buffer.length;
			if (bSamples == 16)
			{
				for (int i=0; i<buffer.length/2; i++)
				{ // 16bit sample size
					short curSample = getShort(buffer[i*2], buffer[i*2+1]);
					if (curSample > cAmplitude)
					{ // Check amplitude
						cAmplitude = curSample;
					}
				}
			}
			else	
			{ // 8bit sample size
				for (int i=0; i<buffer.length; i++)
				{
					if (buffer[i] > cAmplitude)
					{ // Check amplitude
						cAmplitude = buffer[i];
					}
				}
			}
		}
	
		public void onMarkerReached(AudioRecord recorder)
		{
			// NOT USED
		}
	};
	
	/** 
	 * 
	 * 
	 * Default constructor
	 * 
	 * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
	 * In case of errors, no exception is thrown, but the state is set to ERROR
	 * 
	 */ 
	public AudioRecorder(boolean uncompressed, int audioSource, int sampleRate, int channelConfig, int audioFormat)
	{
		try
		{
			rUncompressed = uncompressed;
			if (rUncompressed)
			{ // RECORDING_UNCOMPRESSED
				if (audioFormat == AudioFormat.ENCODING_PCM_16BIT)
				{
					bSamples = 16;
				}
				else
				{
					bSamples = 8;
				}
				
				if (channelConfig == AudioFormat.CHANNEL_IN_MONO)
				{
					nChannels = 1;
				}
				else
				{
					nChannels = 2;
				}
				
				aSource = audioSource;
				sRate   = sampleRate;
				aFormat = audioFormat;

				bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
				framePeriod = bufferSize / ( 2 * bSamples * nChannels / 8 );
				
				audioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);

				if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
					throw new Exception("AudioRecord initialization failed");
				audioRecorder.setRecordPositionUpdateListener(updateListener);
				audioRecorder.setPositionNotificationPeriod(framePeriod);
			} else
			{ // RECORDING_COMPRESSED
				mediaRecorder = new MediaRecorder();
				mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);				
			}
			cAmplitude = 0;
			filePath = null;
			state = State.INITIALIZING;
		} catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(AudioRecorder.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(AudioRecorder.class.getName(), "Unknown error occured while initializing recording");
			}
			state = State.ERROR;
		}
	}
	
	/**
	 * Sets output file path, call directly after construction/reset.
	 *  
	 * @param output file path
	 * 
	 */
	public void setOutputFile(String argPath)
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				filePath = argPath;
				if (!rUncompressed)
				{
					mediaRecorder.setOutputFile(filePath);					
				}
			}
		}
		catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(AudioRecorder.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(AudioRecorder.class.getName(), "Unknown error occured while setting output path");
			}
			state = State.ERROR;
		}
	}
	
	/**
	 * 
	 * Returns the largest amplitude sampled since the last call to this method.
	 * 
	 * @return returns the largest amplitude since the last call, or 0 when not in recording state. 
	 * 
	 */
	public int getMaxAmplitude()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				int result = cAmplitude;
				cAmplitude = 0;
				return result;
			}
			else
			{
				try
				{
					return mediaRecorder.getMaxAmplitude();
				}
				catch (IllegalStateException e)
				{
					return 0;
				}
			}
		}
		else
		{
			return 0;
		}
	}
	

	/**
	 * 
	* Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
	* the recorder is set to the ERROR state, which makes a reconstruction necessary.
	* In case uncompressed recording is toggled, the header of the wave file is written.
	* In case of an exception, the state is changed to ERROR
	* 	 
	*/
	public void prepare()
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				if (rUncompressed)
				{
					if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (filePath != null))
					{
						// write file header

						headerFile = new RandomAccessFile(filePath, "rw");
						testFile=new RandomAccessFile(Environment.getExternalStorageDirectory()+"/temporary.wav", "rw");
						
						headerFile.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
						headerFile.writeBytes("RIFF");
						headerFile.writeInt(0); // Final file size not known yet, write 0 
						headerFile.writeBytes("WAVE");
						headerFile.writeBytes("fmt ");
						headerFile.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
						headerFile.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
						headerFile.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
						headerFile.writeInt(Integer.reverseBytes(sRate)); // Sample rate
						headerFile.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
						headerFile.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
						headerFile.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
						headerFile.writeBytes("data");
						headerFile.writeInt(0); // Data chunk size not known yet, write 0
						
						testFile.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
						testFile.writeBytes("RIFF");
						testFile.writeInt(0); // Final file size not known yet, write 0 
						testFile.writeBytes("WAVE");
						testFile.writeBytes("fmt ");
						testFile.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
						testFile.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
						testFile.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
						testFile.writeInt(Integer.reverseBytes(sRate)); // Sample rate
						testFile.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
						testFile.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
						testFile.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
						testFile.writeBytes("data");
						testFile.writeInt(0); // Data chunk size not known yet, write 0
						
						buffer = new byte[framePeriod*bSamples/8*nChannels];
						state = State.READY;
					}
					else
					{
						Log.e(AudioRecorder.class.getName(), "prepare() method called on uninitialized recorder");
						state = State.ERROR;
					}
				}
				else
				{
					mediaRecorder.prepare();
					state = State.READY;
				}
			}
			else
			{
				Log.e(AudioRecorder.class.getName(), "prepare() method called on illegal state");
				release();
				state = State.ERROR;
			}
		}
		catch(Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(AudioRecorder.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(AudioRecorder.class.getName(), "Unknown error occured in prepare()");
			}
			state = State.ERROR;
		}
	}
	
	/**
	 * 
	 * 
	 *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
	 *  
	 */
	public void release()
	{
		if (state == State.RECORDING)
		{
			stop();
		}
		else
		{
			if ((state == State.READY) & (rUncompressed))
			{
				try
				{
					headerFile.close(); // Remove prepared file
					testFile.close();
				}
				catch (IOException e)
				{
					Log.e(AudioRecorder.class.getName(), "I/O exception occured while closing output file");
				}
				(new File(filePath)).delete();
			}
		}
		
		if (rUncompressed)
		{
			if (audioRecorder != null)
			{
				audioRecorder.release();
			}
		}
		else
		{
			if (mediaRecorder != null)
			{
				mediaRecorder.release();
			}
		}
	}
	
	/**
	 * 
	 * 
	 * Resets the recorder to the INITIALIZING state, as if it was just created.
	 * In case the class was in RECORDING state, the recording is stopped.
	 * In case of exceptions the class is set to the ERROR state.
	 * 
	 */
	public void reset()
	{
		try
		{
			if (state != State.ERROR)
			{
				release();
				filePath = null; // Reset file path
				cAmplitude = 0; // Reset amplitude
				if (rUncompressed)
				{
					audioRecorder = new AudioRecord(aSource, sRate, nChannels+1, aFormat, bufferSize);
				}
				else
				{
					mediaRecorder = new MediaRecorder();
					mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				}
				state = State.INITIALIZING;
			}
		}
		catch (Exception e)
		{
			Log.e(AudioRecorder.class.getName(), e.getMessage());
			state = State.ERROR;
		}
	}
	
	/**
	 * 
	 * 
	 * Starts the recording, and sets the state to RECORDING.
	 * Call after prepare().
	 * 
	 */
	public void start(MainService st)
	{
		this.st=st;
		if (state == State.READY)
		{
			if (rUncompressed)
			{
				payloadSize = 0;
				audioRecorder.startRecording();
				audioRecorder.read(buffer, 0, buffer.length);
			}
			else
			{
				mediaRecorder.start();
			}
			state = State.RECORDING;
		}
		else
		{
			Log.e(AudioRecorder.class.getName(), "start() called on illegal state");
			state = State.ERROR;
		}
	}
	
	/**
	 * 
	 * 
	 *  Stops the recording, and sets the state to STOPPED.
	 * In case of further usage, a reset is needed.
	 * Also finalizes the wave file in case of uncompressed recording.
	 * 
	 */
	public RandomAccessFile stop()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				audioRecorder.stop();
				try
				{
					
					headerFile.seek(4); // Write size to RIFF header
					headerFile.writeInt(Integer.reverseBytes(36+payloadSize));
				
					headerFile.seek(40); // Write size to Subchunk2Size field
					headerFile.writeInt(Integer.reverseBytes(payloadSize));
					
					testFile.seek(4); // Write size to RIFF header
					testFile.writeInt(Integer.reverseBytes(36+payloadSize));
				
					testFile.seek(40); // Write size to Subchunk2Size field
					testFile.writeInt(Integer.reverseBytes(payloadSize));
					//headerFile.close();
				}
				catch(IOException e)
				{
					Log.e(AudioRecorder.class.getName(), "I/O exception occured while closing output file");
					state = State.ERROR;
				}
			}
			else
			{
				mediaRecorder.stop();
			}
			state = State.STOPPED;
		}
		else
		{
			Log.e(AudioRecorder.class.getName(), "stop() called on illegal state");
			state = State.ERROR;
		}
		return headerFile;
	}
	
	/* 
	 * 
	 * Converts a byte[2] to a short, in LITTLE_ENDIAN format
	 * 
	 */
	public static short getShort(byte argB1, byte argB2)
	{
		return (short)(argB1 | (argB2 << 8));
	}
	
	public static byte[] getBytes(short s) {
        return new byte[]{(byte)(s & 0x00FF),(byte)((s & 0xFF00)>>8)};
    }
}
