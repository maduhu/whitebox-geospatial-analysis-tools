/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.algorithms;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.algorithm.ConvexHull;

/**
 *
 * @author johnlindsay
 */
public class MinimumBoundingRectangle {

    public enum MinimizationCriterion {

        AREA, PERIMETER;
    }

    double[][] coordinates;
    final double rightAngle = Math.PI / 2.0;
    GeometryFactory factory = new GeometryFactory();
    boolean minimizeArea = true;
    double boxCentreX;
    double boxCentreY;
    double shortAxis;
    double longAxis;
    double longAxisOrientation;
    double slope;
    boolean boxCalculated = false;

    public MinimumBoundingRectangle() {

    }

    public MinimumBoundingRectangle(MinimizationCriterion criterion) {
        minimizeArea = criterion == MinimizationCriterion.AREA;
    }

    public MinimumBoundingRectangle(double[][] coordinates) {
        this.coordinates = coordinates.clone();
    }

    public MinimumBoundingRectangle(double[][] coordinates, MinimizationCriterion criterion) {
        this.coordinates = coordinates.clone();
        minimizeArea = criterion == MinimizationCriterion.AREA;
    }

    public void setCoordinates(double[][] coordinates) {
        this.coordinates = coordinates.clone();
        boxCalculated = false;
    }

    public void setMinimizationCriterion(MinimizationCriterion criterion) {
        minimizeArea = criterion == MinimizationCriterion.AREA;
    }

    public double[] getBoxCentrePoint() throws Exception {
        if (!boxCalculated) {
            getBoundingBox();
        }
        return new double[]{boxCentreX, boxCentreY};
    }

    public double getLongAxisLength() throws Exception {
        if (!boxCalculated) {
            getBoundingBox();
        }
        return longAxis;
    }

    public double getShortAxisLength() throws Exception {
        if (!boxCalculated) {
            getBoundingBox();
        }
        return shortAxis;
    }

    public double getElongationRatio() throws Exception {
        if (!boxCalculated) {
            getBoundingBox();
        }
        return 1 - shortAxis / longAxis;
    }

    public double getLongAxisOrientation() throws Exception {
        if (!boxCalculated) {
            getBoundingBox();
        }
        return 90 + Math.toDegrees(Math.atan(Math.tan(-slope)));
    }
    
    public double getShortAxisOrientation() throws Exception {
        if (!boxCalculated) {
            getBoundingBox();
        }
        
        double orient = getLongAxisOrientation();
        return (orient >= 90) ? orient - 90 : orient + 90;
    }

    public double[][] getBoundingBox() throws Exception {
        double[][] ret = new double[5][2];
        int numPoints = coordinates.length;
        Coordinate[] coords = new Coordinate[numPoints];

        double east = Double.NEGATIVE_INFINITY;
        double west = Double.POSITIVE_INFINITY;
        double north = Double.NEGATIVE_INFINITY;
        double south = Double.POSITIVE_INFINITY;

        for (int i = 0; i < numPoints; i++) {
            if (coordinates[i][0] > east) {
                east = coordinates[i][0];
            }
            if (coordinates[i][0] < west) {
                west = coordinates[i][0];
            }
            if (coordinates[i][1] > north) {
                north = coordinates[i][1];
            }
            if (coordinates[i][1] < south) {
                south = coordinates[i][1];
            }
            coords[i] = new Coordinate(coordinates[i][0], coordinates[i][1]);
        }

        double midX = west + (east - west) / 2.0;
        double midY = south + (north - south) / 2.0;

        ConvexHull hull = new ConvexHull(coords, factory);
        Coordinate[] hullPoints = hull.getConvexHull().getCoordinates();

        int numHullPoints = hullPoints.length;
        double[][] verticesRotated = new double[numHullPoints][2];
        double[] newBoundingBox = new double[4];
        double[] axes = new double[2];
        axes[0] = 9999999;
        axes[1] = 9999999;
        double x, y;
        slope = 0;
        boxCentreX = 0;
        boxCentreY = 0;
        // Rotate the hull points to align with the orientation of each side in order.
        for (int m = 0; m < numHullPoints - 1; m++) {
            double xDiff = hullPoints[m + 1].x - hullPoints[m].x;
            double yDiff = hullPoints[m + 1].y - hullPoints[m].y;
            double psi = -Math.atan2(yDiff, xDiff);
            // Rotate each edge cell in the array by m degrees.
            for (int n = 0; n < numHullPoints; n++) {
                x = hullPoints[n].x - midX;
                y = hullPoints[n].y - midY;
                verticesRotated[n][0] = (x * Math.cos(psi)) - (y * Math.sin(psi));
                verticesRotated[n][1] = (x * Math.sin(psi)) + (y * Math.cos(psi));
            }
            // calculate the minimum area bounding box in this coordinate 
            // system and see if it is less
            newBoundingBox[0] = Double.MAX_VALUE; // west
            newBoundingBox[1] = Double.MIN_VALUE; // east
            newBoundingBox[2] = Double.MAX_VALUE; // north
            newBoundingBox[3] = Double.MIN_VALUE; // south
            for (int n = 0; n < numHullPoints; n++) {
                x = verticesRotated[n][0];
                y = verticesRotated[n][1];
                if (x < newBoundingBox[0]) {
                    newBoundingBox[0] = x;
                }
                if (x > newBoundingBox[1]) {
                    newBoundingBox[1] = x;
                }
                if (y < newBoundingBox[2]) {
                    newBoundingBox[2] = y;
                }
                if (y > newBoundingBox[3]) {
                    newBoundingBox[3] = y;
                }
            }
            double newXAxis = Math.abs(newBoundingBox[1] - newBoundingBox[0]);
            double newYAxis = Math.abs(newBoundingBox[3] - newBoundingBox[2]);
            double newValue = minimizeArea ? newXAxis * newYAxis : (newXAxis + newYAxis) * 2;
            double currentValue = minimizeArea ? axes[0] * axes[1] : (axes[0] + axes[1]) * 2;
            if (newValue < currentValue) { // minimize the metric of the bounding box.
                axes[0] = newXAxis;
                axes[1] = newYAxis;

                if (axes[0] > axes[1]) {
                    slope = -psi;
                } else {
                    slope = -(rightAngle + psi);
                }
                x = newBoundingBox[0] + newXAxis / 2;
                y = newBoundingBox[2] + newYAxis / 2;
                boxCentreX = midX + (x * Math.cos(-psi)) - (y * Math.sin(-psi));
                boxCentreY = midY + (x * Math.sin(-psi)) + (y * Math.cos(-psi));
            }
        }
        longAxis = Math.max(axes[0], axes[1]);
        shortAxis = Math.min(axes[0], axes[1]);

        double[][] axesEndPoints = new double[4][2];
        axesEndPoints[0][0] = boxCentreX + longAxis / 2.0 * Math.cos(slope);
        axesEndPoints[0][1] = boxCentreY + longAxis / 2.0 * Math.sin(slope);
        axesEndPoints[1][0] = boxCentreX - longAxis / 2.0 * Math.cos(slope);
        axesEndPoints[1][1] = boxCentreY - longAxis / 2.0 * Math.sin(slope);
        axesEndPoints[2][0] = boxCentreX + shortAxis / 2.0 * Math.cos(rightAngle + slope);
        axesEndPoints[2][1] = boxCentreY + shortAxis / 2.0 * Math.sin(rightAngle + slope);
        axesEndPoints[3][0] = boxCentreX - shortAxis / 2.0 * Math.cos(rightAngle + slope);
        axesEndPoints[3][1] = boxCentreY - shortAxis / 2.0 * Math.sin(rightAngle + slope);

        ret[0][0] = axesEndPoints[0][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
        ret[0][1] = axesEndPoints[0][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);

        ret[1][0] = axesEndPoints[0][0] - shortAxis / 2.0 * Math.cos(rightAngle + slope);
        ret[1][1] = axesEndPoints[0][1] - shortAxis / 2.0 * Math.sin(rightAngle + slope);

        ret[2][0] = axesEndPoints[1][0] - shortAxis / 2.0 * Math.cos(rightAngle + slope);
        ret[2][1] = axesEndPoints[1][1] - shortAxis / 2.0 * Math.sin(rightAngle + slope);

        ret[3][0] = axesEndPoints[1][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
        ret[3][1] = axesEndPoints[1][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);

        ret[4][0] = axesEndPoints[0][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
        ret[4][1] = axesEndPoints[0][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);

        boxCalculated = true;
        return ret;
    }

}
