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
package jopensurf;

import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * @author johnlindsay
 */
public class InterestPointMatcher implements Callable<InterestPointPair> {

    private SURFInterestPoint mPoint;
    private List<SURFInterestPoint> mPoints;
    private List<SURFInterestPoint> mComparisonPoints;
    private double mMatchThreshold;

    public InterestPointMatcher(int pointNum, List<SURFInterestPoint> points,
            List<SURFInterestPoint> comparisonPoints,
            double matchThreshold) {
        mPoint = points.get(pointNum);
        mPoints = points;
        mComparisonPoints = comparisonPoints;
        mMatchThreshold = matchThreshold;
    }

    @Override
    public InterestPointPair call() throws Exception {
        try {
            double smallestDistance = Float.MAX_VALUE;
            double nextSmallestDistance = Float.MAX_VALUE;
            SURFInterestPoint possibleMatch = null;
            int lap = mPoint.getLaplacian();
            for (SURFInterestPoint b : mComparisonPoints) {
                if (b.getLaplacian() == lap) {
                    double distance = mPoint.getDistance(b);
                    if (distance < smallestDistance) {
                        nextSmallestDistance = smallestDistance;
                        smallestDistance = distance;
                        possibleMatch = b;
                    } else if (distance < nextSmallestDistance) {
                        nextSmallestDistance = distance;
                    }
                }
            }

            // If match has a d1:d2 ratio < mMatchThreshold points are a match
            if (smallestDistance / nextSmallestDistance <= mMatchThreshold) {
                // is it symmetrical?
                double smallestDistanceSymm = Float.MAX_VALUE;
                SURFInterestPoint possibleMatchSymm = null;
                for (SURFInterestPoint b : mPoints) {
                    if (b.getLaplacian() == lap) {
                        double distance = possibleMatch.getDistance(b);
                        if (distance < smallestDistanceSymm) {
                            smallestDistanceSymm = distance;
                            possibleMatchSymm = b;
                        }
                    }
                }
                if (possibleMatchSymm.isEquivalentTo(mPoint)) {
                    // you have a point with a unambiguously matched point in the 
                    // other image for which the nearest point in the first image 
                    // is the point of interest.
                    return new InterestPointPair(mPoint, possibleMatch, smallestDistance);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
