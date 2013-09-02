package photogrammetry.util;

import java.util.Collection;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import photogrammetry.util.model.Feature;
import photogrammetry.util.model.Point2d;
import photogrammetry.util.model.SceneView;

/**
 * Estimates the essential matrix.
 *
 * @author johannes
 */
public class EssentialMatrixEstimator {

    /**
     * Callback for debugging purposes.
     *
     * @author johannes
     */
    public interface EssentialMatrixEstimatorCallback {

        /**
         * Called when the "A" matrix has been calculated.
         *
         * @param a the "A" matrix
         */
        public void acceptA(Matrix a);

        /**
         * Called when the "e" vector has been calculated.
         *
         * @param e the "e" vector
         */
        public void acceptEVector(Matrix e);

        /**
         * Called when the initial (non-refined) e matrix has been calculated.
         *
         * @param e Essential matrix
         */
        public void acceptE(Matrix e);

        /**
         * Called when the homogenized image points have been calculated.
         *
         * @param index index of the view. 0 or 1.
         * @param imgpts the homogenized image points
         */
        public void acceptImagePoints(int index, Matrix imgpts);
    }
    private static final EssentialMatrixEstimatorCallback NULL_CALLBACK = new EssentialMatrixEstimatorCallback() {
        @Override
        public void acceptEVector(Matrix e) {
        }

        @Override
        public void acceptE(Matrix e) {
        }

        @Override
        public void acceptA(Matrix m) {
        }

        @Override
        public void acceptImagePoints(int index, Matrix imgpts) {
        }
    };
    private final Collection<Feature> commonFeatures;
    private final SceneView image1;
    private final SceneView image2;
    private Matrix essentialMatrix, refinedEssentialMatrix;
    private final EssentialMatrixEstimatorCallback callback;
    private double error, refinedError;

    /**
     * Construct a new essential matrix estimator for two images.
     *
     * @param im1 the first view
     * @param im2 the second view
     * @param c the camera used in both views
     */
    public EssentialMatrixEstimator(SceneView im1, SceneView im2, Camera c) {
        this(im1, im2, c, null);
    }

    /**
     * Construct a new essential matrix estimator for two images with a callback
     * object.
     *
     * @param im1 the first view
     * @param im2 the second view
     * @param c the camera used in both views
     * @param callback the callback object (can be null)
     */
    public EssentialMatrixEstimator(SceneView im1, SceneView im2, Camera c,
            EssentialMatrixEstimatorCallback callback) {
        image1 = im1;
        image2 = im2;
        commonFeatures = image1.getCommonFeatures(image2);
        this.callback = callback == null ? NULL_CALLBACK : callback;
        estimateEssentialMatrix(c);
    }

    /**
     * Gets homogenized undistorted image points.
     *
     * @param c the camera to use for undistorting
     * @param img the image
     * @return a 3x(commonFeatures.size()) Matrix
     */
    private Matrix getHomogenizedPoints(Camera c, SceneView img) {
        Matrix result = new Matrix(3, commonFeatures.size());
        int i = 0;
        for (Feature f : commonFeatures) {
            Point2d p = c.undistort(img.getLocationInView(f));
            result.set(0, i, p.x);
            result.set(1, i, p.y);
            result.set(2, i, 1);
            i++;
        }
        return result;
    }

    /**
     * Normalizes image points.
     *
     * @param points the points to normalize
     * @param kInv the inverse of the camera matrix
     */
    private void normalize(Matrix points, Matrix kInv) {
        Matrix d = new Matrix(3, 1);
        for (int i = 0; i < points.getColumnDimension(); i++) {
            for (int j = 0; j < 3; j++) {
                d.set(j, 0, points.get(j, i));
            }
            points.setMatrix(0, 2, i, i, kInv.times(d));
        }
    }

    /**
     * Create the "A" matrix
     *
     * @param points1 normalized points from first view
     * @param points2 normalized points from second view
     * @return matrix A (see slides)
     */
    private Matrix buildA(Matrix points1, Matrix points2) {
        Matrix result = new Matrix(points1.getColumnDimension(), 9);
        for (int i = 0; i < result.getRowDimension(); i++) {
            double x1i = points1.get(0, i);
            double x2i = points2.get(0, i);
            double y1i = points1.get(1, i);
            double y2i = points2.get(1, i);

            double[] row = new double[]{x1i * x2i, y1i * x2i, x2i, x1i * y2i, y1i * y2i, y2i,
                x1i, y1i, 1};
            for (int col = 0; col < 9; col++) {
                result.set(i, col, row[col]);
            }
        }
        return result;
    }

    /**
     * Compute the error � (x2^T * E * x1)^2
     *
     * @param e essential matrix
     * @param imgpts1 3xn matrix: (xi, yi, 1)
     * @param imgpts2 3xn matrix: (xi, yi, 1)
     * @return the error
     */
    private double getError(Matrix e, Matrix imgpts1, Matrix imgpts2) {
        double val = 0;
        for (int i = 0; i < imgpts1.getColumnDimension(); i++) {
            Matrix pt2 = imgpts2.getMatrix(0, 2, i, i).transpose();
            Matrix pt1 = imgpts1.getMatrix(0, 2, i, i);

            Matrix pt2timesE = pt2.times(e);

            val += Math.pow(pt2timesE.times(pt1).get(0, 0), 2);
        }
        return val;
    }

    /**
     * Estimate essential matrix
     *
     * @param c the camera used in both views
     */
    private void estimateEssentialMatrix(Camera c) {
        final Matrix imgpts2 = getHomogenizedPoints(c, image2);
        final Matrix imgpts1 = getHomogenizedPoints(c, image1);
        final Matrix kInv = c.getInvIntrinsics();
        essentialMatrix = new Matrix(3, 3);

        // normalize image points
        normalize(imgpts1, kInv);
        normalize(imgpts2, kInv);

        callback.acceptImagePoints(0, imgpts1);
        callback.acceptImagePoints(1, imgpts2);

        final Matrix a = buildA(imgpts1, imgpts2);
        callback.acceptA(a);

        final Matrix ata = a.transpose().times(a);

        final Pair<double[], Matrix[]> eigs = MatrixUtils.eig(ata);
        // find evector
        double minEval = Math.abs(eigs.a[0]);
        Matrix evector = eigs.b[0];
        callback.acceptEVector(evector);
        for (int i = 1; i < eigs.a.length; i++) {
            if (minEval > Math.abs(eigs.a[i])) {
                minEval = Math.abs(eigs.a[i]);
                evector = eigs.b[i];
            }
        }

        for (int i = 0; i < 9; i++) {
            essentialMatrix.set(i / 3, i % 3, evector.get(i, 0));
        }
        callback.acceptE(essentialMatrix);

        error = getError(essentialMatrix, imgpts1, imgpts2);

        SingularValueDecomposition svd = new SingularValueDecomposition(essentialMatrix);
        Matrix s = svd.getS();
        System.out.println("S:\n" + MatrixUtils.matrixToString(s));
        s.set(2, 2, 0);
        refinedEssentialMatrix = svd.getU().times(s).times(svd.getV().transpose());

        refinedError = getError(refinedEssentialMatrix, imgpts1, imgpts2);
    }

    /**
     * Return the estimated essential matrix.
     *
     * @return the essential matrix
     */
    public Matrix getEssentialMatrix() {
        return essentialMatrix;
    }

    /**
     * <p>
     * Return the refined essential matrix.
     * </p>
     * <p>
     * The refined essential matrix's singular values are (s1, s2, 0).
     * </p>
     *
     * @return the refined essential matrix.
     */
    public Matrix getRefinedEssentialMatrix() {
        return refinedEssentialMatrix;
    }

    /**
     * Return the error of the essential matrix: � (x2^T * E * x1)^2
     *
     * @return the error
     */
    public double getError() {
        return error;
    }

    /**
     * Return the error of the refined essential matrix: � (x2^T * E' * x1)^2
     *
     * @return the error
     */
    public double getRefinedError() {
        return refinedError;
    }
}
