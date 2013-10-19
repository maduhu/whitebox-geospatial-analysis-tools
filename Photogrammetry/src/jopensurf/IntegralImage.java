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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import whitebox.geospatialfiles.WhiteboxRaster;

/**
 * ABOUT generateIntegralImage
 *
 * When OpenSURF stores it's version of the integral image, some slight rounding
 * actually occurs, it doesn't maintain the same values from when it calculates
 * the integral image to when it calls BoxIntegral on the same data
 *
 * @author astromberg
 *
 * Example from C++ OpenSURF - THIS DOESN'T HAPPEN IN THE JAVA VERSION
 *
 * IntegralImage Values at Calculation Time: integralImage[11][9] = 33.69019699
 * integralImage[16][9] = 47.90196228 integralImage[11][18] = 65.84313202
 * integralImage[16][18] = 93.58038330
 *
 *
 * integralImage[11][18] = 65.84313202 que? integralImage[18][11] = 64.56079102
 *
 * IntegralImage Values at BoxIntegral Time: img[11][9] = 33.83921814
 * img[11][18] = 64.56079102 img[16][9] = 48.76078796 img[16][18] = 93.03530884
 *
 */
public class IntegralImage implements Serializable {

    private static final long serialVersionUID = 1L;
    private float[][] mIntImage;
    private int mWidth = -1;
    private int mHeight = -1;

    public float[][] getValues() {
        return mIntImage;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public float getValue(int column, int row) {
        return mIntImage[column][row];
    }

    public IntegralImage(WhiteboxRaster input) {
        mIntImage = new float[input.getNumberColumns()][input.getNumberRows()];
        mWidth = mIntImage.length;
        mHeight = mIntImage[0].length;

        double nodata = input.getNoDataValue();
        double imageMin = input.getDisplayMinimum();
        double imageRange = input.getDisplayMaximum()- imageMin;
        int width = input.getNumberColumns();
        int height = input.getNumberRows();

        double intensity;
        float sum;
        if (input.getDataScale() != WhiteboxRaster.DataScale.RGB) {
            for (int y = 0; y < height; y++) {
                sum = 0F;
                for (int x = 0; x < width; x++) {
                    intensity = input.getValue(y, x);
                    if (intensity == nodata) {
                        intensity = 0;
                    } else {
                        intensity = (intensity - imageMin) / imageRange;
                    }
                    sum += (float) intensity;
                    if (y == 0) {
                        mIntImage[x][y] = sum;
                    } else {
                        mIntImage[x][y] = sum + mIntImage[x][y - 1];
                    }
                }
            }
        } else {
            double r, g, b;
            for (int y = 0; y < height; y++) {
                sum = 0F;
                for (int x = 0; x < width; x++) {
                    intensity = input.getValue(y, x);
                    if (intensity == nodata) {
                        intensity = 0;
                    } else {
                        r = (double) ((int) intensity & 0xFF);
                        g = (double) (((int) intensity >> 8) & 0xFF);
                        b = (double)(((int)intensity >> 16) & 0xFF);
                        intensity = Math.round((0.299D * r + 0.587D * b + 0.114D * b)) / 255F;
                    }
                    sum += (float) intensity;
                    if (y == 0) {
                        mIntImage[x][y] = sum;
                    } else {
                        mIntImage[x][y] = sum + mIntImage[x][y - 1];
                    }
                }
            }
        }
    }

    public IntegralImage(BufferedImage input) {
        mIntImage = new float[input.getWidth()][input.getHeight()];
        mWidth = mIntImage.length;
        mHeight = mIntImage[0].length;

        int width = input.getWidth();
        int height = input.getHeight();

        WritableRaster raster = input.getRaster();
        int[] pixel = new int[4];
        float sum;
        for (int y = 0; y < height; y++) {
            sum = 0F;
            for (int x = 0; x < width; x++) {
                raster.getPixel(x, y, pixel);
                /**
                 * TODO: FIX LOSS IN PRECISION HERE, DON'T ROUND BEFORE THE
                 * DIVISION (OR AFTER, OR AT ALL) This was done to match the C++
                 * version, can be removed after confident that it's working
                 * correctly.
                 */
                float intensity = Math.round((0.299D * pixel[0] + 0.587D * pixel[1] + 0.114D * pixel[2])) / 255F;
                sum += intensity;
                if (y == 0) {
                    mIntImage[x][y] = sum;
                } else {
                    mIntImage[x][y] = sum + mIntImage[x][y - 1];
                }
            }
        }
    }
}
