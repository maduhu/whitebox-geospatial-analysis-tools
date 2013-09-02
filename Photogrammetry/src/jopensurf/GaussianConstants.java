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

public class GaussianConstants {

    /**
     * 7 x 7 Discrete Gaussian Distribution, sigma = 2.5 Copied from
     * http://code.google.com/p/opensurf1/source/browse/trunk/OpenSURFcpp/src/surf.cpp?spec=svn87&r=87
     */
    public static final double[][] Gauss25 = {
        {0.02546481, 0.02350698, 0.01849125, 0.01239505, 0.00708017, 0.00344629, 0.00142946},
        {0.02350698, 0.02169968, 0.01706957, 0.01144208, 0.00653582, 0.00318132, 0.00131956},
        {0.01849125, 0.01706957, 0.01342740, 0.00900066, 0.00514126, 0.00250252, 0.00103800},
        {0.01239505, 0.01144208, 0.00900066, 0.00603332, 0.00344629, 0.00167749, 0.00069579},
        {0.00708017, 0.00653582, 0.00514126, 0.00344629, 0.00196855, 0.00095820, 0.00039744},
        {0.00344629, 0.00318132, 0.00250252, 0.00167749, 0.00095820, 0.00046640, 0.00019346},
        {0.00142946, 0.00131956, 0.00103800, 0.00069579, 0.00039744, 0.00019346, 0.00008024}
    };

    public static double[][] getGaussianDistribution(int sampleCount, float range, float sigma) {
        double[][] distribution = new double[sampleCount][sampleCount];
        double sigmaSquared = Math.pow(sigma, 2);
        double inverseTwoPiSigmaSquared = 1 / (2 * Math.PI * sigmaSquared);
        for (int i = 0; i < sampleCount; i++) {
            for (int j = 0; j < sampleCount; j++) {
                double x = (range / (sampleCount - 1)) * i;
                double y = (range / (sampleCount - 1)) * j;
                double power = Math.pow(x, 2) / (2 * sigmaSquared) + Math.pow(y, 2) / (2 * sigmaSquared);
                distribution[i][j] = inverseTwoPiSigmaSquared * Math.pow(Math.E, -1 * power);
            }
        }
        return distribution;
    }

    public static void main(String args[]) {
        double[][] dist = getGaussianDistribution(7, 5.5F, 2.5F);
        for (double[] row : dist) {
            for (double value : row) {
                System.out.format("%.14f,", value);
            }
            System.out.println("");
        }
    }
}
