package photogrammetry.util.model;

import java.util.Collection;

/**
 * A view of the scene to be reconstructed.
 *
 * @author johannes
 */
public interface SceneView {

    /**
     * <p>
     * Return the location of the feature in the view.
     * </p>
     * <p>
     * If the feature is not in the view,
     * <code>null</code> is returned.
     * </p>
     *
     * @param f the Feature to be looked up
     * @return The location of the feature, or null if its location is unknown
     */
    public HasCoordinates2d getLocationInView(Feature f);

    /**
     * <p>Return a set of all features in the view.</p>
     *
     * @return
     */
    public Collection<Feature> getFeatures();

    public Collection<Feature> getCommonFeatures(SceneView other);
}
