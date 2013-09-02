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

/**
 *
 * @author johnlindsay
 */
public class InterestPointPair implements Comparable<InterestPointPair> {
    
    private SURFInterestPoint mPoint1;
    private SURFInterestPoint mPoint2;
    private double mDistance;
    
    public InterestPointPair() {
        
    }
    
    public InterestPointPair(SURFInterestPoint point1, SURFInterestPoint point2, double distance) {
        mPoint1 = point1;
        mPoint2 = point2;
        mDistance = distance;
    }
    
    public SURFInterestPoint getPoint1() {
        return mPoint1;
    }
    
    public void setPoint1(SURFInterestPoint point) {
        mPoint1 = point;
    }
    
    public SURFInterestPoint getPoint2() {
        return mPoint2;
    }
    
    public void setPoint2(SURFInterestPoint point) {
        mPoint2 = point;
    }
    
    public double getDistance() {
        return mDistance;
    }
    
    public void setDistance(double distance) {
        mDistance = distance;
    }

    @Override
    public int compareTo(InterestPointPair o) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        if (mDistance < o.getDistance()) {
            return BEFORE;
        } else if (mDistance > o.getDistance()) {
            return AFTER;
        }
        return EQUAL;
    }
}
