package photogrammetry.util.model.models;

import java.io.PrintWriter;

import photogrammetry.util.model.Point3d;

/**
 * Writes models to .obj (wavefront) files.
 * 
 * @author johannes
 */
public class ObjModelWriter extends AbstractTextModelWriter {

	public static final ObjModelWriter inst = new ObjModelWriter();
	
	private ObjModelWriter() {
	}
	
	@Override
	protected void writeModel(PrintWriter writer, Model model) {
		for (Point3d p : model.getPoints()) {
			writer.println("v " + p.toString(" ", false));
		}
	}
	
}
