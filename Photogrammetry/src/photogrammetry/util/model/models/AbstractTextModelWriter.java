package photogrammetry.util.model.models;

import java.io.OutputStream;
import java.io.PrintWriter;

public abstract class AbstractTextModelWriter extends AbstractModelWriter {

	@Override
	public void saveToStream(OutputStream stream, Model model) {
		PrintWriter pw = new PrintWriter(stream);
		try {
			writeModel(pw, model);
		} finally {
			pw.close();
		}
	}

	/**
	 * Write a model.
	 * 
	 * @param writer
	 *            the PrintWriter to use for writing.
	 * @param model
	 *            the model to write.
	 */
	protected abstract void writeModel(PrintWriter writer, Model model);

}
