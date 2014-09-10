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
package whitebox.cartographic;

import java.util.ArrayList;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public final class PointMarkers {
    public enum MarkerStyle {
        CIRCLE, SQUARE, TRIANGLE, TRIANGLE2, DIAMOND, THICK_CROSS, CROSS, X, 
        SIMPLE_STAR, NULL;
    }
    
    public static ArrayList<double[][]> getAllSymbols(float markerSize) {
        ArrayList<double[][]> markers = new ArrayList<>();
        markers.add(PointMarkers.getCircle(markerSize));
        markers.add(PointMarkers.getSquare(markerSize));
        markers.add(PointMarkers.getTriangle(markerSize));
        markers.add(PointMarkers.getTriangle2(markerSize));
        markers.add(PointMarkers.getDiamond(markerSize));
        markers.add(PointMarkers.getThickCross(markerSize));
        markers.add(PointMarkers.getCross(markerSize));
        markers.add(PointMarkers.getX(markerSize));
        markers.add(PointMarkers.getSimpleStar(markerSize));
        return markers;
    }
    
    public static MarkerStyle findMarkerStyleFromIndex(int index) {
        switch (index) {
            case 0:
                return MarkerStyle.CIRCLE;
            case 1:
                return MarkerStyle.SQUARE;
            case 2:
                return MarkerStyle.TRIANGLE;
            case 3:
                return MarkerStyle.TRIANGLE2;
            case 4:
                return MarkerStyle.DIAMOND;
            case 5:
                return MarkerStyle.THICK_CROSS;
            case 6:
                return MarkerStyle.CROSS;
            case 7:
                return MarkerStyle.X;
            case 8:
                return MarkerStyle.SIMPLE_STAR;
            default:
                return MarkerStyle.CIRCLE;
        }
    }
    
    
    public static MarkerStyle findMarkerStyleFromString(String style) {
        if (style.toLowerCase().equals("circle")) {
            return MarkerStyle.CIRCLE;
        } else if (style.toLowerCase().equals("square")) {
            return MarkerStyle.SQUARE;
        } else if (style.toLowerCase().equals("triangle")) {
            return MarkerStyle.TRIANGLE;
        } else if (style.toLowerCase().equals("triangle2")) {
            return MarkerStyle.TRIANGLE2;
        } else if (style.toLowerCase().equals("diamond")) {
            return MarkerStyle.DIAMOND;
        } else if (style.toLowerCase().equals("cross")) {
            return MarkerStyle.CROSS;
        } else if (style.toLowerCase().equals("x")) {
            return MarkerStyle.X;
        } else if (style.toLowerCase().equals("simple_star")) {
            return MarkerStyle.SIMPLE_STAR;
        }
        return MarkerStyle.CIRCLE;
    }
    
    public static double[][] getMarkerData(MarkerStyle markerStyle, float markerSize) {
        switch (markerStyle) {
            case CROSS:
                return getCross(markerSize);
            case DIAMOND:
                return getDiamond(markerSize);
            case SIMPLE_STAR:
                return getSimpleStar(markerSize);
            case SQUARE:
                return getSquare(markerSize);
            case THICK_CROSS:
                return getThickCross(markerSize);
            case TRIANGLE:
                return getTriangle(markerSize);
            case TRIANGLE2:
                return getTriangle2(markerSize);
            case X:
                return getX(markerSize);
            default:
                return getCircle(markerSize);
        }
    }
    
    public static double[][] getCircle(float markerSize) {
        double[][] ret = new double[1][3];
        float halfMS = markerSize / 2.0f;
        ret[0][0] = 2; // Elipse2D instruction
        ret[0][1] = halfMS; // x
        ret[0][2] = markerSize; // y
        
        return ret;
    }
    
    public static double[][] getSquare(float markerSize) {
        double[][] ret = new double[5][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = -halfMS; // x
        ret[0][2] = -halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = halfMS; // x
        ret[1][2] = -halfMS; // y
        
        ret[2][0] = 1; // lineTo instruction
        ret[2][1] = halfMS; // x
        ret[2][2] = halfMS; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = -halfMS; // x
        ret[3][2] = halfMS; // y
        
        ret[4][0] = 1; // lineTo instruction
        ret[4][1] = -halfMS; // x
        ret[4][2] = -halfMS; // y
        
        return ret;
    }
    
    public static double[][] getTriangle(float markerSize) {
        double[][] ret = new double[4][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = 0; // x
        ret[0][2] = -halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = halfMS; // x
        ret[1][2] = halfMS; // y
        
        ret[2][0] = 1; // lineTo instruction
        ret[2][1] = -halfMS; // x
        ret[2][2] = halfMS; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = 0; // x
        ret[3][2] = -halfMS; // y
        
        return ret; 
    }
    
    public static double[][] getTriangle2(float markerSize) {
        double[][] ret = new double[4][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = 0; // x
        ret[0][2] = halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = halfMS; // x
        ret[1][2] = -halfMS; // y
        
        ret[2][0] = 1; // lineTo instruction
        ret[2][1] = -halfMS; // x
        ret[2][2] = -halfMS; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = 0; // x
        ret[3][2] = halfMS; // y
        
        return ret; 
    }
    
    public static double[][] getDiamond(float markerSize) {
        double[][] ret = new double[5][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = 0; // x
        ret[0][2] = -halfMS * 1.2; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = halfMS; // x
        ret[1][2] = 0; // y
        
        ret[2][0] = 1; // lineTo instruction
        ret[2][1] = 0; // x
        ret[2][2] = halfMS * 1.2; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = -halfMS; // x
        ret[3][2] = 0; // y
        
        ret[4][0] = 1; // lineTo instruction
        ret[4][1] = 0; // x
        ret[4][2] = -halfMS * 1.2; // y
        
        return ret; 
    }
    
    public static double[][] getCross(float markerSize) {
        double[][] ret = new double[4][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = 0; // x
        ret[0][2] = -halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = 0; // x
        ret[1][2] = halfMS; // y
        
        ret[2][0] = 0; // moveTo instruction
        ret[2][1] = -halfMS; // x
        ret[2][2] = 0; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = halfMS; // x
        ret[3][2] = 0; // y
        
        return ret; 
    }
    
    public static double[][] getX(float markerSize) {
        double[][] ret = new double[4][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = -halfMS; // x
        ret[0][2] = -halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = halfMS; // x
        ret[1][2] = halfMS; // y
        
        ret[2][0] = 0; // moveTo instruction
        ret[2][1] = -halfMS; // x
        ret[2][2] = halfMS; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = halfMS; // x
        ret[3][2] = -halfMS; // y
        
        return ret; 
    }
    
    public static double[][] getSimpleStar(float markerSize) {
        double[][] ret = new double[8][3];
        float halfMS = markerSize / 2.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = -halfMS; // x
        ret[0][2] = -halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = halfMS; // x
        ret[1][2] = halfMS; // y
        
        ret[2][0] = 0; // moveTo instruction
        ret[2][1] = -halfMS; // x
        ret[2][2] = halfMS; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = halfMS; // x
        ret[3][2] = -halfMS; // y
        
        ret[4][0] = 0; // moveTo instruction
        ret[4][1] = 0; // x
        ret[4][2] = halfMS * 1.2; // y
        
        ret[5][0] = 1; // lineTo instruction
        ret[5][1] = 0; // x
        ret[5][2] = -halfMS * 1.2; // y
        
        ret[6][0] = 0; // moveTo instruction
        ret[6][1] = -halfMS * 1.2; // x
        ret[6][2] = 0; // y
        
        ret[7][0] = 1; // lineTo instruction
        ret[7][1] = halfMS * 1.2; // x
        ret[7][2] = 0; // y
        
        return ret; 
    }
    
    public static double[][] getThickCross(float markerSize) {
        double[][] ret = new double[13][3];
        float halfMS = markerSize / 2.0f;
        float thickness = markerSize / 6.0f;
        
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = -thickness; // x
        ret[0][2] = -halfMS; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = thickness; // x
        ret[1][2] = -halfMS; // y
        
        ret[2][0] = 1; // lineTo instruction
        ret[2][1] = thickness; // x
        ret[2][2] = -thickness; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = halfMS; // x
        ret[3][2] = -thickness; // y
        
        ret[4][0] = 1; // lineTo instruction
        ret[4][1] = halfMS; // x
        ret[4][2] = thickness; // y
        
        ret[5][0] = 1; // lineTo instruction
        ret[5][1] = thickness; // x
        ret[5][2] = thickness; // y
        
        ret[6][0] = 1; // lineTo instruction
        ret[6][1] = thickness; // x
        ret[6][2] = halfMS; // y
        
        ret[7][0] = 1; // lineTo instruction
        ret[7][1] = -thickness; // x
        ret[7][2] = halfMS; // y
        
        ret[8][0] = 1; // lineTo instruction
        ret[8][1] = -thickness; // x
        ret[8][2] = thickness; // y
        
        ret[9][0] = 1; // lineTo instruction
        ret[9][1] = -halfMS; // x
        ret[9][2] = thickness; // y
        
        ret[10][0] = 1; // lineTo instruction
        ret[10][1] = -halfMS; // x
        ret[10][2] = -thickness; // y
        
        ret[11][0] = 1; // lineTo instruction
        ret[11][1] = -thickness; // x
        ret[11][2] = -thickness; // y
        
        ret[12][0] = 1; // lineTo instruction
        ret[12][1] = -thickness; // x
        ret[12][2] = -halfMS; // y
        
        return ret; 
    }
    
//    private double[][] getCircle(float markerSize) {
//        double[][] ret = new double[4][3];
//        double kappa = 0.5522847498;
//        float R = markerSize / 2.0f;
//        ret[0][0] = 0; // moveTo instruction
//        ret[0][1] = 0; // x
//        ret[0][2] = -R; // y
//        
//        ret[1][0] = 2; // curveTo instruction
//        ret[1][1] = R * kappa; // x
//        ret[1][2] = -R; // y
//        
//        //circle.curveTo(x + R * kappa, y - R, x + R, y - R * kappa, x + R, y); // curve to
////        A
////        ', B'
////        , B // Second
////                circle
////        .curveTo(x + R, y + R * kappa, x + R * kappa, y + R, x, y + R);
////
////// Third
////        circle.curveTo(x - R * kappa, y + R, x - R, y + R * kappa, x - R, y);
////
////// Last
////        circle.curveTo(x - R, y - R * kappa, x - R * kappa, y - R, x, y - R);
//
//        Ellipse2D circle = new Ellipse2D.Double((middleX - halfMarkerSize), (middleY - halfMarkerSize), markerSize, markerSize);
//        return ret;     
//    }
    
    
}
