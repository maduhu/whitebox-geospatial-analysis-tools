/*
 * Modified by John Lindsay 2013
 */

/*
 This work was derived from Chris Evan's opensurf project and re-licensed as the
 3 clause BSD license with permission of the original author. Thank you Chris! 

 Copyright (c) 2010, Andrew Stromberg
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither Andrew Stromberg nor the
 names of its contributors may be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL Andrew Stromberg BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jopensurf;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

public class FastHessian implements Serializable {

    private static final long serialVersionUID = 1L;
    private static int[][] filter_map = {{0, 1, 2, 3}, {1, 3, 4, 5}, {3, 5, 6, 7}, {5, 7, 8, 9}, {7, 9, 10, 11}};
    private IntegralImage mIntegralImage;
    private List<SURFInterestPoint> mInterestPoints;
    private int mOctaves;
    private int mInitSample;
    private float mThreshold;
    private int mHeight;
    private int mWidth;
    private boolean mRecalculateInterestPoints = true;
    List<ResponseLayer> mLayers;

    public FastHessian(IntegralImage integralImage,
            int octaves, int initSample, float threshold,
            float balanceValue) {
        mIntegralImage = integralImage;
        mOctaves = octaves;
        mInitSample = initSample;
        mThreshold = threshold;
        mWidth = integralImage.getWidth();
        mHeight = integralImage.getHeight();
    }

    public List<SURFInterestPoint> getIPoints() {
        if (mInterestPoints == null || mRecalculateInterestPoints) {
            mInterestPoints = new LinkedList<>();
            buildResponseMap();

            ResponseLayer b, m, t;
            for (int o = 0; o < mOctaves; o++) {
                for (int i = 0; i <= 1; i++) {
                    b = mLayers.get(filter_map[o][i]);
                    m = mLayers.get(filter_map[o][i + 1]);
                    t = mLayers.get(filter_map[o][i + 2]);

                    // loop over middle response layer at density of the most
                    // sparse layer (always top), to find maxima across scale and space
                    for (int r = 0; r < t.getHeight(); r++) {
                        for (int c = 0; c < t.getWidth(); c++) {
                            if (isExtremum(r, c, t, m, b)) {
                                //System.out.println("r = " + r + ", c = " + c);
                                SURFInterestPoint point = interpolateExtremum(r, c, t, m, b);
                                if (point != null) {
                                    mInterestPoints.add(point);
                                }
                            }
                        }
                    }
                }
            }
        }
        return mInterestPoints;
    }

    private void buildResponseMap() {
        mLayers = new LinkedList<>();

        int w = mWidth / mInitSample;
        int h = mHeight / mInitSample;
        int s = mInitSample;
//        if (mOctaves >= 1) {
//            mLayers.add(new ResponseLayer(w, h, s, 9, mIntegralImage));
//            mLayers.add(new ResponseLayer(w, h, s, 15, mIntegralImage));
//            mLayers.add(new ResponseLayer(w, h, s, 21, mIntegralImage));
//            mLayers.add(new ResponseLayer(w, h, s, 27, mIntegralImage));
//        }
//
//        if (mOctaves >= 2) {
//            mLayers.add(new ResponseLayer(w / 2, h / 2, s * 2, 39, mIntegralImage));
//            mLayers.add(new ResponseLayer(w / 2, h / 2, s * 2, 51, mIntegralImage));
//        }
//
//        if (mOctaves >= 3) {
//            mLayers.add(new ResponseLayer(w / 4, h / 4, s * 4, 75, mIntegralImage));
//            mLayers.add(new ResponseLayer(w / 4, h / 4, s * 4, 99, mIntegralImage));
//
//        }
//
//        if (mOctaves >= 4) {
//            mLayers.add(new ResponseLayer(w / 8, h / 8, s * 8, 147, mIntegralImage));
//            mLayers.add(new ResponseLayer(w / 8, h / 8, s * 8, 195, mIntegralImage));
//        }
//
//        if (mOctaves >= 5) {
//            mLayers.add(new ResponseLayer(w / 16, h / 16, s * 16, 291, mIntegralImage));
//            mLayers.add(new ResponseLayer(w / 16, h / 16, s * 16, 387, mIntegralImage));
//        }

//        if (mOctaves > 5) {
//            mOctaves = 5;
//        }

        int filterIncrease = 3;
        int firstFiltersizeOfTheOctave = 9;
        for (int k = 0; k < mOctaves; k++) {
            filterIncrease = 2 * filterIncrease;
            int[] octaveFilterSizes = new int[4];
            int i = 0;
            for (int filtersize = firstFiltersizeOfTheOctave;
                    filtersize < (firstFiltersizeOfTheOctave
                    + (3 * filterIncrease) + 1); filtersize += filterIncrease) {
                octaveFilterSizes[i] = filtersize;
                i++;
            }
            if (k > 0) {
                int value = (int)Math.pow(2, k);
                mLayers.add(new ResponseLayer(w / value, h / value, s * value, octaveFilterSizes[2], mIntegralImage));
                mLayers.add(new ResponseLayer(w / value, h / value, s * value, octaveFilterSizes[3], mIntegralImage));
            } else {
                mLayers.add(new ResponseLayer(w, h, s, octaveFilterSizes[0], mIntegralImage));
                mLayers.add(new ResponseLayer(w, h, s, octaveFilterSizes[1], mIntegralImage));
                mLayers.add(new ResponseLayer(w, h, s, octaveFilterSizes[2], mIntegralImage));
                mLayers.add(new ResponseLayer(w, h, s, octaveFilterSizes[3], mIntegralImage));
            }
            firstFiltersizeOfTheOctave += filterIncrease;
        }
        
        /* I'll admit that I don't understand how filter map quite works.
         * I'm rewritten the code in this class to handle an arbitrary number of
         * octaves whereas previously it was a maximum of 5. filter_map was 
         * previously hard coded with:
         * filter_map = {{0, 1, 2, 3}, {1, 3, 4, 5}, {3, 5, 6, 7}, {5, 7, 8, 9}, {7, 9, 10, 11}};
         * Why is there the skipped value between the first and second entry? I 
         * really don't know. I truly dislike writing code that I don't understand.
         * 
         * John Lindsay (2013)
         */
        filter_map = new int[mOctaves][4];
        for (int k = 0; k < mOctaves; k++) {
            if (k > 0) {
                filter_map[k][0] = 1 + (k - 1) * 2;
                filter_map[k][1] = filter_map[k][0] + 2;
                filter_map[k][2] = filter_map[k][1] + 1;
                filter_map[k][3] = filter_map[k][2] + 1;
            } else {
                filter_map[k][0] = 0;
                filter_map[k][1] = 1;
                filter_map[k][2] = 2;
                filter_map[k][3] = 3;
                
            }
        }
    }

    private boolean isExtremum(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
        int layerBorder = (t.getFilter() + 1) / (2 * t.getStep());

        if (r <= layerBorder || r >= t.getHeight() - layerBorder || c <= layerBorder || c >= t.getWidth() - layerBorder) {
            return false;
        }

//        double candidate = m.getResponse(r, c, t);
//
//        if (candidate < mThreshold) {
//            return false;
//        }
//
//        //System.out.println("r = " + r + ", c = " + c);
//        //See if the response in 3x3x3 is greater, then it isn't a local maxima
//        for (int rr = -1; rr <= 1; rr++) {
//            for (int cc = -1; cc <= 1; cc++) {
//                if (t.getResponse(r + rr, c + cc) >= candidate
//                        || ((rr != 0 || cc != 0) && m.getResponse(r + rr, c + cc, t) >= candidate)
//                        || b.getResponse(r + rr, c + cc, t) >= candidate) {
//                    return false;
//                }
//            }
//        }
//        return true;

        final double candidate = m.getResponse(r, c, t);
        if (Math.abs(candidate) < mThreshold) {
            return false;
        }

        // System.out.println("r = " + r + ", c = " + c);
        // See if the response in 3x3x3 is greater or lower, then it isn't a local extrema
        for (int rr = -1; rr <= 1; rr++) {
            for (int cc = -1; cc <= 1; cc++) {
                if (candidate > 0) {
                    if (t.getResponse(r + rr, c + cc) >= candidate
                            || (rr != 0 || cc != 0)
                            && m.getResponse(r + rr, c + cc, t) >= candidate
                            || b.getResponse(r + rr, c + cc, t) >= candidate) {
                        return false;
                    }
                } else {
                    if (t.getResponse(r + rr, c + cc) <= candidate
                            || (rr != 0 || cc != 0)
                            && m.getResponse(r + rr, c + cc, t) <= candidate
                            || b.getResponse(r + rr, c + cc, t) <= candidate) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private SURFInterestPoint interpolateExtremum(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
        //should check to make sure that m's filter value is less than t's and greater than b's
        int filterStep = m.getFilter() - b.getFilter();

        double xi = 0, xr = 0, xc = 0;
        double[] values = interpolateStep(r, c, t, m, b);
        xi = values[0];
        xr = values[1];
        xc = values[2];

        if (Math.abs(xi) < 0.5f && Math.abs(xr) < 0.5f && Math.abs(xc) < 0.5f) {
            //WE"VE GOT AN INTERESTING POINT HERE
            float x = (float) (c + xc) * t.getStep();
            float y = (float) (r + xr) * t.getStep();
            float scale = (float) (0.1333F * (m.getFilter() + xi * filterStep));
            int laplacian = (int) m.getLaplacian(r, c, t);
            //System.out.println("x: " + x + ", y: " + y + ", scale: " + scale + ", lap: " + laplacian);
            return new SURFInterestPoint(x, y, scale, laplacian);
        }
        return null;
    }

    private double[] interpolateStep(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
        double[] values = new double[3];

        RealMatrix partialDerivs = getPartialDerivativeMatrix(r, c, t, m, b);
        RealMatrix hessian3D = getHessian3DMatrix(r, c, t, m, b);

        DecompositionSolver solver = new LUDecomposition(hessian3D).getSolver();
        RealMatrix X = solver.getInverse().multiply(partialDerivs);

//		System.out.println("X = " + X.getColumnDimension() + " col x " + X.getRowDimension() + " rows");
//		for ( int i = 0; i < X.getRowDimension(); i++ ){
//			for ( int j = 0; j < X.getColumnDimension(); j++ ){
//				System.out.print(X.getEntry(i,j) + (j != X.getColumnDimension()-1 ? " - " : ""));
//			}
//			System.out.println();
//		}
//		System.out.println();
//		
        //values of them are used
        //xi
        values[0] = -X.getEntry(2, 0);
        //xr
        values[1] = -X.getEntry(1, 0);
        //xc
        values[2] = -X.getEntry(0, 0);

        return values;
    }

    private RealMatrix getPartialDerivativeMatrix(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
        //deriv[0][0] = dx, deriv[1][0] = dy, deriv[2][0] = ds
        double[][] derivs = new double[3][1];

        derivs[0][0] = (m.getResponse(r, c + 1, t) - m.getResponse(r, c - 1, t)) / 2.0D;
        derivs[1][0] = (m.getResponse(r + 1, c, t) - m.getResponse(r - 1, c, t)) / 2.0D;
        derivs[2][0] = (t.getResponse(r, c) - b.getResponse(r, c, t)) / 2.0D;

        //System.out.format("dx = %.8f, dy = %.8f, ds = %.8f",derivs[0][0],derivs[1][0],derivs[2][0]);
        //System.out.println();

        RealMatrix matrix = new Array2DRowRealMatrix(derivs);
        //System.out.println("Matrix Num Rows: " + matrix.getRowDimension() + ", num columns: " + matrix.getColumnDimension());
        return matrix;
    }

    private RealMatrix getHessian3DMatrix(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
        //Layout:
        //  [dxx][dxy][dxs]
        //  [dxy][dyy][dys]
        //  [dxs][dys][dss]
        double[][] hessian = new double[3][3];

        double v = m.getResponse(r, c, t);

        //dxx
        hessian[0][0] = m.getResponse(r, c + 1, t)
                + m.getResponse(r, c - 1, t)
                - 2 * v;

        //dyy
        hessian[1][1] = m.getResponse(r + 1, c, t)
                + m.getResponse(r - 1, c, t)
                - 2 * v;

        //dss
        hessian[2][2] = t.getResponse(r, c)
                + b.getResponse(r, c, t)
                - 2 * v;

        //dxy
        hessian[0][1] = hessian[1][0] = (m.getResponse(r + 1, c + 1, t)
                - m.getResponse(r + 1, c - 1, t)
                - m.getResponse(r - 1, c + 1, t)
                + m.getResponse(r - 1, c - 1, t)) / 4.0;

        //dxs
        hessian[0][2] = hessian[2][0] = (t.getResponse(r, c + 1)
                - t.getResponse(r, c - 1)
                - b.getResponse(r, c + 1, t)
                + b.getResponse(r, c - 1, t)) / 4.0;

        //dys
        hessian[1][2] = hessian[2][1] = (t.getResponse(r + 1, c)
                - t.getResponse(r - 1, c)
                - b.getResponse(r + 1, c, t)
                + b.getResponse(r - 1, c, t)) / 4.0;

        return new Array2DRowRealMatrix(hessian);
    }

    public static void main(String[] args) {
        try {
            IntegralImage integralImage = new IntegralImage(ImageIO.read(new File("C:\\workspace\\opensurf\\OpenSURF\\Images\\sf.jpg")));
            FastHessian hessian = new FastHessian(integralImage, 5, 2, 0.004F, 0.81F);
            hessian.getIPoints();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
