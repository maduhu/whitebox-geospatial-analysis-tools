package photogrammetry.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import Jama.Matrix;
import photogrammetry.util.model.Feature;
import photogrammetry.util.model.HasCoordinates2d;
import photogrammetry.util.model.Point3d;
import photogrammetry.util.model.SceneView;
import photogrammetry.util.model.models.Model;

/**
 * Reconstructs models from two views.
 *
 * @author johannes
 */
public class TwoViewReconstructor {

    private TwoViewReconstructor() {
    }

    /**
     * Reconstructs models from pair of scene views.
     *
     * @param camera the camera used to capture both views
     * @param view1 the first view
     * @param view2 the second view
     * @param allModels if set to true, all generated models will be returned.
     * @return a list of possible models matching the two views
     */
    public static List<Model> getPossibleModels(Camera camera, SceneView view1, SceneView view2,
            boolean allModels) {
        final EssentialMatrixEstimator essEst = new EssentialMatrixEstimator(view1, view2, camera);
        final RotationTranslationEstimator rotTransEst = new RotationTranslationEstimator(
                essEst.getRefinedEssentialMatrix());
        final Collection<Feature> commonFeatures = view1.getCommonFeatures(view2);
        final Matrix r1 = rotTransEst.getR1();
        final Matrix r2 = rotTransEst.getR2();
        Matrix t = rotTransEst.getT();

        List<Model> candidateModels = new ArrayList<>(4);
        List<Integer> noInvPts = new ArrayList<>(4);
        try {
            noInvPts.add(reconstructModel(camera, candidateModels, commonFeatures, t, r1, view1,
                    view2));
        } catch (RuntimeException e) {
        }
        try {
            noInvPts.add(reconstructModel(camera, candidateModels, commonFeatures, t, r2, view1,
                    view2));
        } catch (RuntimeException e) {
        }
        t = t.times(-1);
        try {
            noInvPts.add(reconstructModel(camera, candidateModels, commonFeatures, t, r1, view1,
                    view2));
        } catch (RuntimeException e) {
        }
        try {
            noInvPts.add(reconstructModel(camera, candidateModels, commonFeatures, t, r2, view1,
                    view2));
        } catch (RuntimeException e) {
        }

        // TODO check whether these two models are really the ones we want!

        if (allModels) {
            return candidateModels;
        }

        // the maximum is the model that is behind both cameras
        List<Model> resultList = new ArrayList<>(2);
        int idx = getMaxIndex(noInvPts);
        resultList.add(candidateModels.remove(idx));
        noInvPts.remove(idx);

        // the minimum is the model that is in front of both cameras
        idx = getMinIndex(noInvPts);
        resultList.add(candidateModels.remove(idx));
        noInvPts.remove(idx);

        return resultList;
    }

    private static int getMaxIndex(List<Integer> l) {
        int max = l.get(0);
        int maxIndex = 0;
        for (int i = 1; i < l.size(); i++) {
            if (l.get(i) > max) {
                max = l.get(i);
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private static int getMinIndex(List<Integer> l) {
        int min = l.get(0);
        int minIndex = 0;
        for (int i = 1; i < l.size(); i++) {
            if (l.get(i) < min) {
                min = l.get(i);
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * Create a Model using triangulation.
     *
     * @param candidateModels the model will be added to this list
     * @param commonFeatures set of all features that should be triangulated
     * @param t location of the second view
     * @param r rotation of the second view
     * @param i1 first view
     * @param i2 second view
     * @return the number of points that lie behind the camera
     */
    private static int reconstructModel(Camera camera, List<Model> candidateModels,
            Collection<Feature> commonFeatures, Matrix t, Matrix r, SceneView i1, SceneView i2) {
        Model m = new Model();
        Matrix c = r.transpose().times(t).times(-1);
        Matrix zeroTrans = new Matrix(3, 1);
        Matrix identityRot = Matrix.identity(3, 3);
        final SceneView left = i1;
        final SceneView right = i2;
        int result = 0;

        for (Feature f : commonFeatures) {
            final HasCoordinates2d fLeft = left.getLocationInView(f);
            final HasCoordinates2d fRight = right.getLocationInView(f);
            Matrix x = Triangulator.triangulate(camera, r, c, fLeft, fRight);

            m.addPoint(f, new Point3d(x.get(0, 0), x.get(1, 0), x.get(2, 0)));
            if (camera.isPointBehindCamera(x, t, r)) {
                result++;
            }
            if (camera.isPointBehindCamera(x, zeroTrans, identityRot)) {
                result++;
            }
        }
        candidateModels.add(m);
        return result;
    }
}
