package photogrammetry.util;

import java.util.ArrayList;
import java.util.List;


import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Performs estimation of rotation and translation between two views using the essential matrix.
 * 
 * @author johannes
 */
public class RotationTranslationEstimator {

	private final Matrix t;
	private final Matrix r1;
	private final Matrix r2;

	/**
	 * Construct a rotation & translation estimator for an essential matrix. Implemented according
	 * to http://www.comp.nus.edu.sg/~cs4243/lecture/multiview.pdf, slides 52 to 54.
	 * 
	 * @param e
	 *            the essential matrix
	 */
	public RotationTranslationEstimator(Matrix e) {
		final SingularValueDecomposition svd = new SingularValueDecomposition(e);

		System.out.println("U:");
		System.out.println(MatrixUtils.matrixToString(svd.getU()));
		System.out.println("S:");
		System.out.println(MatrixUtils.matrixToString(svd.getS()));
		System.out.println("V:");
		System.out.println(MatrixUtils.matrixToString(svd.getV()));

		Matrix tt = new Matrix(3, 1);
		for (int i = 0; i < 3; i++) {
			tt.set(i, 0, svd.getU().get(i, 2));
		}
		t = tt.times(1 / tt.normF());

		Matrix RT = new Matrix(new double[][] { new double[] { 0, -1, 0 },
				new double[] { 1, 0, 0 }, new double[] { 0, 0, 1 } });

		List<Matrix> matrices = new ArrayList<>(4);

		matrices.add(svd.getU().times(RT).times(svd.getV().transpose()));
		matrices.add(svd.getU().times(RT).times(svd.getV().transpose()).times(-1));
		RT = RT.transpose();
		matrices.add(svd.getU().times(RT).times(svd.getV().transpose()));
		matrices.add(svd.getU().times(RT).times(svd.getV().transpose()).times(-1));

		for (int i = 3; i >= 0; i--) {
			if (Math.abs(matrices.get(i).det() - 1) > 0.0001) {
				matrices.remove(i);
			}
		}

		r1 = matrices.get(0);
		r2 = matrices.get(1);
	}

	/**
	 * Return the estimated direction of translation.
	 * 
	 * @return the direction of translation
	 */
	public Matrix getT() {
		return t;
	}

	/**
	 * Return the first possible rotation matrix.
	 * 
	 * @return the first of two rotation matrices
	 */
	public Matrix getR1() {
		return r1;
	}

	/**
	 * Return the second possible rotation matrix.
	 * 
	 * @return the second of two rotation matrices
	 */
	public Matrix getR2() {
		return r2;
	}

	@Override
	public String toString() {
		return "Translation: \n" + MatrixUtils.matrixToString(t) + "R1: \n"
				+ MatrixUtils.matrixToString(r1) + "R2: \n" + MatrixUtils.matrixToString(r2);
	}

}
