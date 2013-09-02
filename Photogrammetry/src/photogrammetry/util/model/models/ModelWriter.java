package photogrammetry.util.model.models;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for model writers.
 * 
 * @author johannes
 */
public interface ModelWriter {
	
	/**
	 * Writes a model to a file.
	 * 
	 * @param file
	 *            the file to write the model to.
	 * @param model
	 *            the model to write.
	 */
	public void saveToFile(File file, Model model) throws IOException;

	/**
	 * Writes a model to a stream.
	 * 
	 * @param stream
	 *            the stream to write the model to.
	 * @param model
	 *            the model to write.
	 */
	public void saveToStream(OutputStream stream, Model model);

}
