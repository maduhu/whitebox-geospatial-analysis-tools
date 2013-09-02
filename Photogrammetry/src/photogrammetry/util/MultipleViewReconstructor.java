package photogrammetry.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import photogrammetry.util.model.Feature;
import photogrammetry.util.model.Image;
import photogrammetry.util.model.Point3d;
import photogrammetry.util.model.SceneView;
import photogrammetry.util.model.models.Model;

/**
 * Reconstruct a model from multiple views.
 *
 * @author johannes
 */
public class MultipleViewReconstructor {

    private final Camera camera;
    private final Model model;

    /**
     * Construct a new multiple view reconstructor.
     *
     * @param c the camera used in all views
     * @param views the views
     */
    public MultipleViewReconstructor(Camera c, List<SceneView> views) {
        camera = c;

        // pairs of images we use for reconstruction
        final List<Pair<SceneView, SceneView>> pairs = new ImageChooser(views).getPairs();

        // reconstructed models, two for each pair of images
        final List<List<Model>> models = createModels(pairs);

        if (models.size() > 0) {
            // our initial model. We're going to iteratively add features to this.
            model = models.get(0).get(0);
            Map<Feature, Integer> featureCount = new HashMap<>();

            for (int i = 1; i < models.size(); i++) {
                Map<Feature, Point3d> bestModelPts = null;
                double bestError = Double.POSITIVE_INFINITY;

                for (Model m : models.get(i)) {
                    AbsoluteOrientation absOrient = new AbsoluteOrientation(m, model);
                    if (absOrient.getError() < bestError) {
                        bestError = absOrient.getError();
                        bestModelPts = absOrient.adjustModel1();
                    }
                }

                System.out.println(bestError);
                if (bestError < Double.POSITIVE_INFINITY) {
                    model.merge(bestModelPts, featureCount);
                }
            }
        } else {
            model = null;
        }
    }

    /**
     * Creates models from list of images
     *
     * @param imgList List containing Images with common features
     * @return List containing Models for reconstruction (For each pair of
     * images two models)
     */
    private List<List<Model>> createModels(List<Pair<SceneView, SceneView>> imgList) {
        List<List<Model>> modelList = new ArrayList<List<Model>>();
        for (Pair<SceneView, SceneView> p : imgList) {
            modelList.add(TwoViewReconstructor.getPossibleModels(camera, p.a, p.b, false));
        }
        return modelList;
    }

    /**
     * Get the reconstructed model.
     *
     * @return the reconstructed model, or null if reconstruction failed.
     */
    public Model getModel() {
        return model;
    }
}
