package photogrammetry.util.model.models;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Abstract base class for model writers. Provides default implementation of saveToFile.
 * 
 * @author johannes
 */
public abstract class AbstractModelWriter implements ModelWriter {

	@Override
	public void saveToFile(File file, Model model) throws IOException {
		BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(file));
		try {
			saveToStream(bo, model);
		} finally {
			bo.close();
		}
	}

}
