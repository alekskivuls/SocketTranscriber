import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.Transform;

public class Transcriber {
	Configuration configuration;
	StreamSpeechRecognizer recognizer;
	InputStream stream;

	public Transcriber() throws IOException {
		configuration = new Configuration();
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
		configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
		recognizer = new StreamSpeechRecognizer(configuration);
	}

	/**
	 * Transcribes audio from a specified file, writes result to file, then
	 * returns resulting filepath. Uses adaptive speaker profiles to adjust data
	 * for better analysis
	 * 
	 * @param filepath The audio path to analyze
	 * @return The filepath to the resulting text transcribed from the audio
	 * @throws Exception
	 */
	public String readFile(String filePath) throws Exception {
		stream = new FileInputStream(new File(filePath));
		// Skip first 44 bytes of header of wav files
		stream.skip(44);
		
		// Stats class is used to collect speaker-specific data
		Stats stats = recognizer.createStats(1);
		recognizer.startRecognition(stream);
		SpeechResult result;
		while ((result = recognizer.getResult()) != null) {
			stats.collect(result);
		}
		recognizer.stopRecognition();

		// Transform represents the speech profile
		Transform transform = stats.createTransform();
		recognizer.setTransform(transform);

		// Decode again with updated transform
		stream = new FileInputStream(new File(filePath));
		stream.skip(44);
		recognizer.startRecognition(stream);
		StringBuilder sb = new StringBuilder();
		while ((result = recognizer.getResult()) != null) {
			String hypothesis = result.getHypothesis();
			System.out.format("Hypothesis: %s\n", hypothesis);
			sb.append(hypothesis);
			sb.append(' ');
		}
		recognizer.stopRecognition();
		
		File temp = File.createTempFile("transcript" + System.currentTimeMillis(), ".txt");
		FileWriter out = new FileWriter(temp);
		out.write(sb.toString());
		out.close();
		
		return temp.getAbsolutePath();
	}
}