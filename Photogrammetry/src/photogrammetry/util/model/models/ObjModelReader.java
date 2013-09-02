package photogrammetry.util.model.models;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import photogrammetry.util.model.Feature;
import photogrammetry.util.model.Point3d;

public class ObjModelReader {

	public static final ObjModelReader inst = new ObjModelReader();

	private ObjModelReader() {
	}

	public Model readModel(File file) throws IOException {
		Model r = new Model();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("v")) {
					String[] parts = line.split(" ");
					r.addPoint(
							new Feature(),
							new Point3d(Double.valueOf(parts[1]), Double.valueOf(parts[2]), Double
									.valueOf(parts[3])));
				}
			}
		} finally {
			reader.close();
		}
		return r;
	}

}
