package photogrammetry.util.model.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import photogrammetry.util.model.Feature;
import photogrammetry.util.model.HasCoordinates3d;
import photogrammetry.util.model.Point3d;

public class Model {

    private Map<Feature, Point3d> points;
    private Point3d center = null;
    private Double maxDistance = null;

    /**
     * Construct a new model.
     */
    public Model() {
        points = new HashMap<>();
    }

    /**
     * Add a point to this model.
     *
     * @param feature the feature the point belongs to.
     * @param point the value of the point.
     */
    public synchronized void addPoint(Feature feature, HasCoordinates3d point) {
        points.put(feature, new Point3d(point.getX(), point.getY(), point.getZ()));
        center = null;
        maxDistance = null;
    }

    /**
     * Get the center (average) of all points in this model.
     *
     * @return the average of all the points of this model.
     */
    public synchronized HasCoordinates3d getCenter() {
        if (center == null) {
            double x = 0, y = 0, z = 0;
            for (Point3d p : getPoints()) {
                x += p.x;
                y += p.y;
                z += p.z;
            }
            int count = getPoints().size();
            center = new Point3d(x / count, y / count, z / count);
        }
        return center;
    }

    /**
     * Get the maximum distance of a point to the center of this model.
     *
     * @return the maximum distance.
     */
    public synchronized double getMaximumDistanceFromCenter() {
        if (maxDistance == null) {
            HasCoordinates3d center = getCenter();
            double maxd2 = 0;
            for (Point3d p : getPoints()) {
                double dx = p.x - center.getX();
                double dy = p.y - center.getY();
                double dz = p.z - center.getZ();
                double d2 = dx * dx + dy * dy + dz * dz;
                maxd2 = Math.max(maxd2, d2);
            }
            maxDistance = Math.sqrt(maxd2);
        }
        return maxDistance;
    }

    /**
     * <p>
     * Get all points in this model, in no particular order.
     * </p>
     * <p>
     * See {@link HashMap#values()} for details.
     * </p>
     *
     * @return a collection with all points in this model.
     */
    public Collection<Point3d> getPoints() {
        return points.values();
    }

    public Map<Feature, Point3d> getPointMap() {
        return points;
    }

    public Set<Feature> getCommonFeatures(Model other) {
        Set<Feature> result = new HashSet<>();
        for (Feature f : points.keySet()) {
            if (other.points.containsKey(f)) {
                result.add(f);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Model [\n");
        for (Point3d p : points.values()) {
            str.append("  " + p + "\n");
        }
        str.append("\n");
        return str.toString();
    }

    /**
     * Merge the points from
     * <code>pts</code> into this model.
     *
     * @param pts the points to merge
     * @param featureCount the number points that have been found for each
     * feature so far.
     */
    public void merge(Map<Feature, Point3d> pts, Map<Feature, Integer> featureCount) {
        for (Entry<Feature, Point3d> f : pts.entrySet()) {
            if (points.containsKey(f.getKey())) {
                Integer count = featureCount.get(f.getKey());
                if (count == null) {
                    count = 1;
                }
                Point3d p = points.get(f.getKey());
                p.x = (p.x * count + f.getValue().x) / (count + 1);
                p.y = (p.y * count + f.getValue().y) / (count + 1);
                p.z = (p.z * count + f.getValue().z) / (count + 1);
                featureCount.put(f.getKey(), count + 1);
            } else {
                points.put(f.getKey(), f.getValue());
                featureCount.put(f.getKey(), 1);
            }
        }
    }
}
