package photogrammetry.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import Jama.Matrix;

import photogrammetry.util.model.Feature;
import photogrammetry.util.model.Point3d;
import photogrammetry.util.model.models.Model;

/**
 * Computes rotation matrix R and translation vector T
 *
 * @author Piotr
 */
public class AbsoluteOrientation {

    private final Matrix T;
    private final Matrix R;
    private final double error;
    private final double scaleFactor;
    final private Model model1;
    final private Model model2;

    /**
     * Construct a rotation & translation matrix. Implemented according to
     * http://www.comp.nus.edu.sg/~cs4243/lecture/multiview.pdf, slides 21 to
     * 24.
     *
     * @param mod1 a model
     * @param mod2 another "view" of the same model
     */
    public AbsoluteOrientation(Model mod1, Model mod2) {
        model1 = mod1;
        model2 = mod2;

        List<Feature> featuresForAbsoluteOrientation = new ArrayList<>(
                model1.getCommonFeatures(model2));
        Matrix m1 = createMatrix(featuresForAbsoluteOrientation, model1.getPointMap());
        Matrix m2 = createMatrix(featuresForAbsoluteOrientation, model2.getPointMap());

        Matrix points1 = m1.copy();
        Matrix points2 = m2.copy();

        Matrix mean1 = CalculateMeanValue(m1);
        Matrix mean2 = CalculateMeanValue(m2);

        normalizeCoordinates(m1, mean1);
        normalizeCoordinates(m2, mean2);
        scaleFactor = getScaleFactor(m1, m2);
        R = rotationMatrix(m1, m2);
        T = translationVector(R, mean1, mean2);
        error = calculateError(points1, points2);
    }

    /**
     * Get the error of the projection of model1's coordinates in model2's
     * coordinate frame.
     *
     * @return the sum squared error
     */
    public double getError() {
        return error;
    }

    /**
     * Creates Matrix object from given points
     *
     * @param order the features to extract from the pts map (and the order).
     * @param pts a map from features to points
     * @return Matrix with points
     */
    private static Matrix createMatrix(List<Feature> order, Map<Feature, Point3d> pts)
            throws IllegalArgumentException {
        double[][] A = new double[3][order.size()];
        int i = 0;
        for (Feature f : order) {
            Point3d p = pts.get(f);
            A[0][i] = p.x;
            A[1][i] = p.y;
            A[2][i] = p.z;
            i++;
        }
        return new Matrix(A);
    }

    /**
     * Calculates mean value for each Matrix of points
     *
     * @param points Matrix with points
     * @return Mean value (x,y,z)
     */
    private static Matrix CalculateMeanValue(Matrix points) {
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;

        for (int i = 0; i < points.getColumnDimension(); i++) {
            sumX += points.get(0, i);
            sumY += points.get(1, i);
            sumZ += points.get(2, i);
        }
        sumX = sumX / points.getColumnDimension();
        sumY = sumY / points.getColumnDimension();
        sumZ = sumZ / points.getColumnDimension();
        Matrix mean = new Matrix(1, 3);
        mean.set(0, 0, sumX);
        mean.set(0, 1, sumY);
        mean.set(0, 2, sumZ);
        return mean;
    }

    /**
     * Computes normalized coordinates (x_i = x_i-x_mean)
     *
     * @param points Matrix of points
     * @param mean Mean value (x,y,z)
     */
    private static void normalizeCoordinates(Matrix points, Matrix mean) {
        for (int i = 0; i < points.getColumnDimension(); i++) {
            points.set(0, i, (points.get(0, i) - mean.get(0, 0)));
            points.set(1, i, (points.get(1, i) - mean.get(0, 1)));
            points.set(2, i, (points.get(2, i) - mean.get(0, 2)));
        }
    }

    /**
     * Computes scaling factor (for rigid transformation s = 1)
     *
     * @param pts1 normalized coordinates 1
     * @param pts2 normalized coordinates 2
     * @return scaling factor (double)
     */
    private static double getScaleFactor(Matrix pts1, Matrix pts2) {
        double r1 = 0;
        double r2 = 0;
        for (int i = 0; i < pts1.getColumnDimension(); i++) {
            r1 += pts1.get(0, i) * pts1.get(0, i) + pts1.get(1, i) * pts1.get(1, i)
                    + pts1.get(2, i) * (pts1.get(2, i));
            r2 += pts2.get(0, i) * pts2.get(0, i) + pts2.get(1, i) * pts2.get(1, i)
                    + pts2.get(2, i) * (pts2.get(2, i));
        }
        return Math.sqrt(r2 / r1);
    }

    /**
     * Computes rotation Matrix R (3x3)
     *
     * @param pts1 normalized coordinates 1
     * @param pts2 normalized coordinates 2
     * @return Matrix R
     */
    private static Matrix rotationMatrix(Matrix pts1, Matrix pts2) {
        Matrix M = pts1.times(pts2.transpose());
        Matrix Q = M.times(M.transpose());
        Matrix V = Q.eig().getV();
        double[] d = Q.eig().getRealEigenvalues();
        Matrix A = new Matrix(3, 3);
        for (int i = 0; i < 3; i++) {
            A.set(i, i, 1 / Math.sqrt(d[i]));
        }
        return M.transpose().times(V).times(A).times(V.transpose());
    }

    /**
     * Computes translation vector T
     *
     * @param R Rotation matrix
     * @param mean1 Mean value for 1st set of pts
     * @param mean2 Mean value for 2nd set of pts
     * @return Translation vector T (1x3)
     */
    private Matrix translationVector(Matrix R, Matrix mean1, Matrix mean2) {
        return mean2.transpose().minus(R.times(scaleFactor).times(mean1.transpose()));
    }

    /**
     * Calculates error
     *
     * @return error (double)
     */
    private double calculateError(Matrix points1, Matrix points2) {
        Matrix pts2est = R.times(points1).times(scaleFactor);

        for (int i = 0; i < pts2est.getColumnDimension(); i++) {
            pts2est.set(0, i, pts2est.get(0, i) + T.get(0, 0));
            pts2est.set(1, i, pts2est.get(1, i) + T.get(1, 0));
            pts2est.set(2, i, pts2est.get(2, i) + T.get(2, 0));
        }
        double error = 0;
        for (int i = 0; i < pts2est.getColumnDimension(); i++) {
            error += Math.pow(pts2est.get(0, i) - points2.get(0, i), 2);
            error += Math.pow(pts2est.get(1, i) - points2.get(1, i), 2);
            error += Math.pow(pts2est.get(2, i) - points2.get(2, i), 2);
        }
        return error;
    }

    /**
     * Translates the points in model 1 into the second model's coordinate
     * frame.
     *
     * @return the first model's features mapped into the second model's
     * coordinate frame.
     */
    public Map<Feature, Point3d> adjustModel1() {
        Map<Feature, Point3d> map = new HashMap<>();
        Matrix p = new Matrix(3, 1);
        for (Entry<Feature, Point3d> e : model1.getPointMap().entrySet()) {
            p.set(0, 0, e.getValue().x);
            p.set(1, 0, e.getValue().y);
            p.set(2, 0, e.getValue().z);
            p = R.times(p).times(scaleFactor).plus(T);
            map.put(e.getKey(), new Point3d(p.get(0, 0), p.get(1, 0), p.get(2, 0)));
        }
        return map;
    }

    /**
     * Translates the points in model 2 into the first model's coordinate frame.
     *
     * @return the second model's features mapped into the first model's
     * coordinate frame.
     */
    public Map<Feature, Point3d> adjustModel2() {
        Map<Feature, Point3d> map = new HashMap<>();
        Matrix p = new Matrix(3, 1);
        Matrix r = R.inverse().times(1 / scaleFactor);
        for (Entry<Feature, Point3d> e : model2.getPointMap().entrySet()) {
            p.set(0, 0, e.getValue().x);
            p.set(1, 0, e.getValue().y);
            p.set(2, 0, e.getValue().z);
            p = r.times(p.minus(T));
            map.put(e.getKey(), new Point3d(p.get(0, 0), p.get(1, 0), p.get(2, 0)));
        }
        return map;
    }
}
