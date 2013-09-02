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
import java.util.Arrays;

public class SURFInterestPoint implements Serializable, Cloneable, InterestPoint {

    private static final long serialVersionUID = 1L;
    private float mX, mY;
    private float mScale;
    private float mOrientation;
    private int mLaplacian;
    private float[] mDescriptor;
    private float mDx, mDy;
    private int mClusterIndex;

    public SURFInterestPoint(float x, float y, float scale, int laplacian) {
        mX = x;
        mY = y;
        mScale = scale;
        mLaplacian = laplacian;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public float getScale() {
        return mScale;
    }

    public float getOrientation() {
        return mOrientation;
    }

    public void setOrientation(float orientation) {
        mOrientation = orientation;
    }

    public int getLaplacian() {
        return mLaplacian;
    }

    public float[] getDescriptor() {
        return mDescriptor;
    }

    /**
     * To take care of the InterestPoint Interface
     */
    @Override
    public float[] getLocation() {
        return mDescriptor;
    }

    public void setDescriptor(float[] descriptor) {
        mDescriptor = descriptor;
    }

    public float getDx() {
        return mDx;
    }

    public void setDx(float dx) {
        mDx = dx;
    }

    public float getDy() {
        return mDy;
    }

    public void setDy(float dy) {
        mDy = dy;
    }

    public int getClusterIndex() {
        return mClusterIndex;
    }

    public void setClusterIndex(int clusterIndex) {
        mClusterIndex = clusterIndex;
    }

    @Override
    public double getDistance(InterestPoint point) {
        double sum = 0;
        if (point.getLocation() == null || mDescriptor == null) {
            return Float.MAX_VALUE;
        }
        for (int i = 0; i < mDescriptor.length; i++) {
            double diff = mDescriptor[i] - point.getLocation()[i];
            sum += diff * diff;
        }
        return (double) Math.sqrt(sum);
    }

    public Float getCoord(int dimension) {
        return mDescriptor[dimension];
    }

    public int getDimensions() {
        return mDescriptor.length;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean isEquivalentTo(SURFInterestPoint point) {
        boolean isEquivalent = true;

        isEquivalent &= mX == point.getX();
        isEquivalent &= mY == point.getY();

        isEquivalent &= mDx == point.getDx();
        isEquivalent &= mDy == point.getDy();

        isEquivalent &= mOrientation == point.getOrientation();

        isEquivalent &= mScale == point.getScale();

        isEquivalent &= mLaplacian == point.getLaplacian();

        isEquivalent &= Arrays.equals(mDescriptor, point.getDescriptor());

        return isEquivalent;
    }
}
