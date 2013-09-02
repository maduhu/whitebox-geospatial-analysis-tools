/*
 * Modified by John Lindsay 2013
 */
package jopensurf;
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

import java.io.Serializable;

public class ResponseLayer implements Serializable {

    private static final long serialVersionUID = 1L;
    private int mWidth;
    private int mHeight;
    private int mStep;
    private int mFilter;
    private char[][] mLaplacian;
    private double[][] mResponses;

    ResponseLayer(int width, int height, int step, int filter, IntegralImage integralImage) {
        mWidth = width;
        mHeight = height;
        mStep = step;
        mFilter = filter;

        mLaplacian = new char[mWidth][mHeight];
        mResponses = new double[mWidth][mHeight];

        buildResponseLayer(integralImage);
    }

    private void buildResponseLayer(IntegralImage img) {
        int b = (mFilter - 1) / 2;
        int l = mFilter / 3;
        int w = mFilter;
        double inverse_area = 1D / (w * w);
        double Dxx, Dyy, Dxy;
        //System.out.println("w: " + mWidth + ", h: " + mHeight + ", step: " + mStep + ", filter: " + mFilter);
        //System.out.println("filter: " + mFilter + ", b: " + b + ", l: " + l + ", w: " + w);
        //System.out.println("inverse area = " + inverse_area);
        for (int r, c, ar = 0, index = 0; ar < mHeight; ++ar) {
            for (int ac = 0; ac < mWidth; ++ac, index++) {
                r = ar * mStep;
                c = ac * mStep;

                // Compute response components
                Dxx = ImageTransformUtils.BoxIntegral(img, r - l + 1, c - b, 2 * l - 1, w)
                        - ImageTransformUtils.BoxIntegral(img, r - l + 1, c - l / 2, 2 * l - 1, l) * 3;
                Dyy = ImageTransformUtils.BoxIntegral(img, r - b, c - l + 1, w, 2 * l - 1)
                        - ImageTransformUtils.BoxIntegral(img, r - l / 2, c - l + 1, l, 2 * l - 1) * 3;
                Dxy = +ImageTransformUtils.BoxIntegral(img, r - l, c + 1, l, l)
                        + ImageTransformUtils.BoxIntegral(img, r + 1, c - l, l, l)
                        - ImageTransformUtils.BoxIntegral(img, r - l, c - l, l, l)
                        - ImageTransformUtils.BoxIntegral(img, r + 1, c + 1, l, l);

                //System.out.println("dxx: " + Dxx + ", dyy: " + Dyy + ", Dxy: " + Dxy);

                // Normalise the filter responses with respect to their size
                Dxx *= inverse_area;
                Dyy *= inverse_area;
                Dxy *= inverse_area;

                // Get the determinant of hessian response & laplacian sign
                mResponses[ac][ar] = (Dxx * Dyy - 0.81f * Dxy * Dxy);
                mLaplacian[ac][ar] = (char) (Dxx + Dyy >= 0 ? 1 : 0);
            }
        }
    }

    public double getResponse(int row, int col) {
        return mResponses[col][row];
    }

    public char getLaplacian(int row, int col) {
        return mLaplacian[col][row];
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFilter() {
        return mFilter;
    }

    public int getStep() {
        return mStep;
    }

    public double getResponse(int row, int col, ResponseLayer src) {
        int scale = getWidth() / src.getWidth();
        return getResponse(row * scale, col * scale);
    }

    public float getLaplacian(int row, int col, ResponseLayer src) {
        int scale = getWidth() / src.getWidth();
        return getLaplacian(row * scale, col * scale);
    }
}
