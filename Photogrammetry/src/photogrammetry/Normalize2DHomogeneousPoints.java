/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package photogrammetry;

/**
 *
 * @author johnlindsay
 */
public class Normalize2DHomogeneousPoints {
    
    public static double[][] T;
    
    public static double[][] normalize(double[][] points) {
        if (points.length != 3) {
            return null;
        }
        int i, n;
        int numPoints = points[0].length;
        double[][] ret = new double[3][numPoints];
        // For the finite points ensure homogeneous coords have scale of 1
        for (i = 0; i < numPoints; i++) {
            if (!isPointAtInfinity(points[0][i])) {
                ret[0][i] = points[0][i] / points[2][i];
                ret[1][i] = points[1][i] / points[2][i];
                ret[2][i] = 1;
            }
        }
        
        // Shift the origin to the centroid
        double meanX = 0;
        double meanY = 0;
        n = 0;
        for (i = 0; i < numPoints; i++) {
            if (!isPointAtInfinity(points[0][i])) {
                meanX += ret[0][i];
                meanY += ret[1][i];
                n++;
            }
        }
        meanX = meanX / n;
        meanY = meanY / n;
        for (i = 0; i < numPoints; i++) {
            if (!isPointAtInfinity(points[0][i])) {
                ret[0][i] -= meanX;
                ret[1][i] -= meanY;
            }
        }
        
        double meanDist = 0;
        for (i = 0; i < numPoints; i++) {
            if (!isPointAtInfinity(points[0][i])) {
                meanDist += Math.sqrt(ret[0][i] * ret[0][i] + ret[1][i] * ret[1][i]);
            }
        }
        meanDist = meanDist / n;
        
        double scale = Math.sqrt(2) / meanDist;
        for (i = 0; i < numPoints; i++) {
            if (!isPointAtInfinity(points[0][i])) {
                ret[0][i] *= scale;
                ret[1][i] *= scale;
            }
        }
        
        T = new double[][]{{scale, 0, -scale * meanX},
        {0, scale, -scale * meanY},
        {0, 0, 1}};
        
        return ret;
    }
    
    public static boolean isPointAtInfinity(double value) {
        if (value == Float.POSITIVE_INFINITY) { return true; }
        if (value == Float.NEGATIVE_INFINITY) { return true; }
        if (value == Double.POSITIVE_INFINITY) { return true; }
        if (value == Double.NEGATIVE_INFINITY) { return true; }
        return false;
        
    }
}
