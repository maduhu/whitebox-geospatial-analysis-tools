package photogrammetry.util.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import photogrammetry.util.MatrixUtils;

public class Image implements SceneView {

    private final File path;
    private final Map<Feature, Point2d> imageLocations = new HashMap<>();

    /**
     * <p>
     * Construct a new image using a given path.
     * </p>
     * <p>
     * The image need not yet exist.
     * </p>
     *
     * @param path
     */
    public Image(File path) throws IOException {
        this.path = path;
        loadFeatures();
    }

    /**
     * Get the path where this image is stored.
     *
     * @return the path of the image
     */
    public File getPath() {
        return path;
    }

    /**
     * Sets the location of a feature in this image.
     *
     * @param f the feature whose location to set
     * @param location the new location of the feature
     */
    public void setLocation(Feature f, HasCoordinates2d location) {
        if (imageLocations.containsKey(f)) {
            imageLocations.get(f).set(location);
        } else {
            imageLocations.put(f, new Point2d(location));
        }
    }

    /**
     * Removes a feature from this image.
     *
     * @param f
     */
    public void removeFeature(Feature f) {
        imageLocations.remove(f);
    }

    /**
     * Determine the feature nearest to a given point (in image coordiantes).
     *
     * @param pt the point
     * @return the nearest feature. If this image has no features, null will be
     * returned.
     */
    public Feature getNearestFeature(HasCoordinates2d pt) {
        double minDistance = Double.MAX_VALUE;
        Feature minFeature = null;
        for (Feature f : getFeatures()) {
            double dist = getLocationInView(f).distanceTo(pt);
            if (dist < minDistance) {
                minDistance = dist;
                minFeature = f;
            }
        }
        return minFeature;
    }

    @Override
    public Set<Feature> getFeatures() {
        return imageLocations.keySet();
    }

    @Override
    public HasCoordinates2d getLocationInView(Feature f) {
        return imageLocations.get(f);
    }

    @Override
    public Set<Feature> getCommonFeatures(SceneView other) {
        Set<Feature> commonFeatures = new LinkedHashSet<>();
        Collection<Feature> features2 = other.getFeatures();
        Set<Feature> features = getFeatures();
        for (Feature f : features2) {
            if (features.contains(f)) {
                commonFeatures.add(f);
            }
        }
        return commonFeatures;
    }

    /**
     * Looks for a file named path + ".features" and if it exists, tries to load
     * features from the file.
     *
     * @throws IOException if the file is corrupted
     */
    public void loadFeatures() throws IOException {
        File f = new File(path.getAbsolutePath() + ".features");
        if (f.exists()) {
            DataInputStream dis = new DataInputStream(new FileInputStream(f));
            try {
                if (dis.available() < 4) {
                    return;
                }
                int count = dis.readInt();
                imageLocations.clear();
                for (int i = 0; i < count; i++) {
                    imageLocations.put(new Feature(dis.readLong()), new Point2d(dis.readDouble(),
                            dis.readDouble()));
                }
            } finally {
                dis.close();
            }
        } else {
            String absolutePath = path.getAbsolutePath();
            f = new File(absolutePath.substring(0, absolutePath.length() - 3) + "txt");
            if (f.exists()) {
                double[] values = MatrixUtils.loadMatrix(f, " ");
                for (int i = 0; i < values.length; i += 3) {
                    imageLocations.put(new Feature((long) values[i]), new Point2d(values[i + 1],
                            values[i + 2]));
                }
            }
        }
    }

    /**
     * Saves all features to a file named path + ".features"
     *
     * @throws IOException
     */
    public void saveFeatures() throws IOException {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(path.getAbsolutePath()
                + ".features"));
        try {
            dos.writeInt(imageLocations.size());
            for (Map.Entry<Feature, Point2d> entry : imageLocations.entrySet()) {
                dos.writeLong(entry.getKey().id);
                dos.writeDouble(entry.getValue().x);
                dos.writeDouble(entry.getValue().y);
            }
        } finally {
            dos.close();
        }
    }
}
