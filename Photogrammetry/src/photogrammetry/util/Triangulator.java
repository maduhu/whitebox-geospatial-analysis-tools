package photogrammetry.util;

import java.util.Arrays;
import java.util.List;

import photogrammetry.util.model.HasCoordinates2d;

import Jama.Matrix;

/**
 * Class with methods for triangulation according to
 * http://www.comp.nus.edu.sg/~cs4243/lecture/multiview.pdf p. 33-35.
 *
 * @author johannes
 */
public class Triangulator {

    /**
     * Perform triangulation according to
     * http://www.comp.nus.edu.sg/~cs4243/lecture/multiview.pdf p. 33-35.
     *
     * @param intr the camera matrix
     * @param r the rotation matrices (3x3)
     * @param c the camera center points (3x1)
     * @param x the image coordinates (3x1), (xt, yt, 1)T
     * @return the triangulated 3d coordinate.
     */
    public static Matrix triangulate(Matrix intr, List<Matrix> r, List<Matrix> c, List<Matrix> x) {
        Matrix intrInv = intr.inverse();
        Matrix mtmSum = new Matrix(3, 3);
        Matrix mtmcSum = new Matrix(3, 1);
        Matrix id = Matrix.identity(3, 3);
        for (int k = 0; k < r.size(); k++) {
            Matrix xk = x.get(k);
            Matrix vk = r.get(k).inverse().times(intrInv).times(xk);
            vk = vk.times(1 / vk.normF());
            Matrix mk = id.minus(vk.times(vk.transpose()));
            Matrix mktmk = mk.transpose().times(mk);
            mtmSum = mtmSum.plus(mktmk);
            mtmcSum = mtmcSum.plus(mktmk.times(c.get(k)));
        }
        return mtmSum.inverse().times(mtmcSum);
    }

    /**
     * Helper function for using
     * {@link Triangulator#triangulate(Matrix, List, List, List)} with just two
     * views and one point, where the first view's translation is 0, its
     * rotation matrix is I.
     *
     * @param c the camera to use for undistorting
     * @param r2 the second camera's rotation
     * @param c2 the second camera's center
     * @param x1 the coordinate of the feature in the first camera's image
     * @param x2 the coordinate of the feature in the second camera's image
     * @return the triangulated 3d coordinate.
     */
    public static Matrix triangulate(Camera c, Matrix r2, Matrix c2, HasCoordinates2d x1,
            HasCoordinates2d x2) {
        HasCoordinates2d x1U = c.undistort(x1);
        HasCoordinates2d x2U = c.undistort(x2);
        return triangulate(c.getIntrinsics(),
                Arrays.asList(Matrix.identity(3, 3), r2),
                Arrays.asList(new Matrix(3, 1), c2),
                Arrays.asList(new Matrix(new double[]{x1U.getX(), x1U.getY(), 1}, 3),
                new Matrix(new double[]{x2U.getX(), x2U.getY(), 1}, 3)));

    }
}
