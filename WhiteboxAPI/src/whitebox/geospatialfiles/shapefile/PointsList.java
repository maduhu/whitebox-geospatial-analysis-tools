/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.geospatialfiles.shapefile;

import java.util.ArrayList;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PointsList {
    private ArrayList<ShapeFilePoint> myList = new ArrayList<ShapeFilePoint>();
    
    public void addPoint(double x, double y) {
        ShapeFilePoint sfp = new ShapeFilePoint(x, y);
        myList.add(sfp);
    }
    
    public void addMPoint(double x, double y, double m) {
        ShapeFilePoint sfp = new ShapeFilePoint(x, y);
        sfp.m = m;
        myList.add(sfp);
    }
    
    public void addMPoint(double x, double y) {
        ShapeFilePoint sfp = new ShapeFilePoint(x, y);
        myList.add(sfp);
    }
    
    public void addZPoint(double x, double y, double z, double m) {
        ShapeFilePoint sfp = new ShapeFilePoint(x, y);
        sfp.z = z;
        sfp.m = m;
        myList.add(sfp);
    }
    
    public void addZPoint(double x, double y, double z) {
        ShapeFilePoint sfp = new ShapeFilePoint(x, y);
        sfp.z = z;
        myList.add(sfp);
    }
    
    public void removePoint(int i) {
        myList.remove(i);
    }
    
    public double[][] getPointsArray() {
        double[][] ret = new double[myList.size()][2];
        int i = 0;
        for (ShapeFilePoint sfp : myList) {
            ret[i][0] = sfp.x;
            ret[i][1] = sfp.y;
            i++;
        }
        return ret;
    }
    
    public double[] getZArray() {
        double[] ret = new double[myList.size()];
        int i = 0;
        for (ShapeFilePoint sfp : myList) {
            ret[i] = sfp.z;
            i++;
        }
        return ret;
    }
    
    public double[] getMArray() {
        double[] ret = new double[myList.size()];
        int i = 0;
        for (ShapeFilePoint sfp : myList) {
            ret[i] = sfp.m;
            i++;
        }
        return ret;
    }
    
    private class ShapeFilePoint {
        double x;
        double y;
        double z = 0.0;
        double m = 0.0;
        
        private ShapeFilePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
