package photogrammetry.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Wraps JAMA into vecmath matrices
 *
 * @author johannes
 */
public class MatrixUtils {

    /**
     * Compute eigenvectors and eigenvalues of a (symmetric) matrix.
     *
     * @param m the matrix to find the eigenvalues and eigenvectors of. All the
     * eigenvalues must be real.
     * @return eigenvalues and eigenvectors as column vectors
     */
    public static Pair<double[], Matrix[]> eig(Matrix m) {
        EigenvalueDecomposition ev = new EigenvalueDecomposition(m);
        Pair<double[], Matrix[]> result = new Pair<>();
        result.a = ev.getRealEigenvalues();
        Matrix eigvs = ev.getV();
        result.b = new Matrix[eigvs.getColumnDimension()];
        double[] eigvsCols = eigvs.getColumnPackedCopy();
        double[] oneColumn = new double[eigvs.getRowDimension()];
        for (int i = 0; i < eigvs.getColumnDimension(); i++) {
            System.arraycopy(eigvsCols, i * oneColumn.length, oneColumn, 0, oneColumn.length);
            result.b[i] = new Matrix(oneColumn.length, 1);
            for (int row = 0; row < oneColumn.length; row++) {
                result.b[i].set(row, 0, oneColumn[row]);
            }
        }
        return result;
    }

    /**
     * <p>
     * Load a matrix from a text file.
     * </p>
     * <p>
     * The order of the numbers in the array is the same as the order in the
     * text file.
     * </p>
     *
     * @param file the file to load the matrix from
     * @throws IOException
     */
    public static double[] loadMatrix(File file) throws IOException {
        return loadMatrix(file, ",");
    }

    /**
     * <p>
     * Load a matrix from a text file.
     * </p>
     * <p>
     * The order of the numbers in the array is the same as the order in the
     * text file.
     * </p>
     *
     * @param file the file to load the matrix from
     * @param separator the separator between columns
     * @throws IOException
     */
    public static double[] loadMatrix(File file, String separator) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            List<String[]> lines = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                lines.add(line.split(separator));
                if (lines.get(lines.size() - 1).length != lines.get(0).length) {
                    throw new IOException("Invalid matrix!");
                }
            }
            double[] result = new double[lines.size() * lines.get(0).length];
            int k = 0;
            for (String[] row : lines) {
                for (int j = 0; j < row.length; j++) {
                    result[k++] = Double.valueOf(row[j].trim());
                }
            }
            return result;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static String matrixToString(Matrix m) {
        StringBuilder result = new StringBuilder();

        for (int row = 0; row < m.getRowDimension(); row++) {
            for (int col = 0; col < m.getColumnDimension(); col++) {
                result.append(String.format("%10.5f ", m.get(row, col)));
            }
            result.append("\n");
        }

        return result.toString();
    }
}
