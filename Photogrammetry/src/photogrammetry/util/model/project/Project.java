package photogrammetry.util.model.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import photogrammetry.util.model.Feature;

public class Project implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2907660464256409408L;

	private Map<Feature, Set<Feature>> topology = new HashMap<Feature, Set<Feature>>();
	private final List<File> files = new ArrayList<File>();
	private final File path;

	public Project(File path) {
		this.path = path;
	}

	public static Project load(File path) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
		try {
			Object o = ois.readObject();
			if (o instanceof Project) {
				Project result = (Project) o;
				if (result.topology == null)
					result.topology = new HashMap<Feature, Set<Feature>>();
				return (Project) o;
			}
			return null;
		} catch (IOException e) {
			throw new IOException("Corrupted file", e);
		} catch (ClassNotFoundException e) {
			throw new IOException("Corrupted file", e);
		} finally {
			ois.close();
		}
	}

	public void save() throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
		try {
			oos.writeObject(this);
		} finally {
			oos.close();
		}
	}

	public List<File> files() {
		return files;
	}

	public synchronized Set<Feature> connectedFeatures(Feature f) {
		Set<Feature> features = topology.get(f);
		if (features == null) {
			features = new HashSet<Feature>();
			topology.put(f, features);
		}
		return features;
	}

	/**
	 * <p>
	 * Toggle the connection between two features.
	 * </p>
	 * <p>
	 * <code>toggleConnection</code> adds a connection between two features if there is none, and
	 * removes it, if there is one. It then tries to save the project file.
	 * </p>
	 * 
	 * @param a first feature
	 * @param b second feature
	 */
	public void toggleConnection(Feature a, Feature b) {
		Set<Feature> features = connectedFeatures(a);
		if (!features.remove(b))
			features.add(b);
		features = connectedFeatures(b);
		if (!features.remove(a)) {
			features.add(a);
		}
		try {
			save();
		} catch (IOException e) {
		}
	}

}
