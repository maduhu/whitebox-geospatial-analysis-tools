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
public final class NorthArrowMarkers {
    public enum MarkerStyle {
        STANDARD, STAR, NULL;
    }
    
    public static ArrayList<double[][]> getAllSymbols(float markerSize) {
        ArrayList<double[][]> markers = new ArrayList<double[][]>();
        markers.add(NorthArrowMarkers.getStandard(markerSize));
        markers.add(NorthArrowMarkers.getStar(markerSize));
        return markers;
    }
    
    public static MarkerStyle findMarkerStyleFromIndex(int index) {
        switch (index) {
            case 0:
                return MarkerStyle.STANDARD;
            case 1:
                return MarkerStyle.STAR;
            default:
                return MarkerStyle.STANDARD;
        }
    }
    
    
    public static MarkerStyle findMarkerStyleFromString(String style) {
        if (style.toLowerCase().equals("standard")) {
            return MarkerStyle.STANDARD;
        } else if (style.toLowerCase().equals("star")) {
            return MarkerStyle.STAR;
        }
        return MarkerStyle.STANDARD;
    }
    
    public static double[][] getMarkerData(MarkerStyle markerStyle, float markerSize) {
        switch (markerStyle) {
            case STANDARD:
                return getStandard(markerSize);
            case STAR:
                return getStar(markerSize);
            default:
                return getStandard(markerSize);
        }
    }
    
    public static double[][] getStandard(float markerSize) {
        double[][] ret = new double[19][3];
        double x, y;
        double halfMS = markerSize / 2.0;
        double oneThirdMS = markerSize / 3.0;
        double oneQuarterMS = markerSize / 4.0;
        //double oneFifthMS = markerSize / 5.0;
        double oneEigthMS = markerSize / 8.0;
        double oneTenthMS = markerSize / 10.0;
        double oneTwentiethMS = markerSize / 20.0;
        
        x = -oneQuarterMS;
        y = -halfMS;
        
        // left triangle
        ret[0][0] = 0; // moveTo instruction
        ret[0][1] = x - x; // x
        ret[0][2] = y; // y
        
        ret[1][0] = 1; // lineTo instruction
        ret[1][1] = x; // x
        ret[1][2] = y + 2 * oneThirdMS; // y
        
        ret[2][0] = 1; // lineTo instruction
        ret[2][1] = x + oneQuarterMS; // x
        ret[2][2] = y + halfMS; // y
        
        ret[3][0] = 1; // lineTo instruction
        ret[3][1] = x - x; // x
        ret[3][2] = y; // y
        
        // right triangle
        ret[4][0] = 3; // moveTo instruction and fill path
        ret[4][1] = x - x; // x
        ret[4][2] = y; // y
        
        ret[5][0] = 1; // lineTo instruction
        ret[5][1] = x + 2 * oneQuarterMS; // x
        ret[5][2] = y + 2 * oneThirdMS; // y
        
        ret[6][0] = 1; // lineTo instruction
        ret[6][1] = x + oneQuarterMS; // x
        ret[6][2] = y + halfMS; // y
        
        ret[7][0] = 1; // lineTo instruction
        ret[7][1] = x - x; // x
        ret[7][2] = y; // y
        
        // the 'N'
        x = -oneEigthMS;
        y = halfMS - oneThirdMS + oneTwentiethMS;
        
        double nThickness = markerSize / 20.0;
        
        ret[8][0] = 3; // lineTo instruction
        ret[8][1] = x; // x
        ret[8][2] = y + oneThirdMS - oneTwentiethMS; // y
        
        ret[9][0] = 1; // moveTo instruction
        ret[9][1] = x; // x
        ret[9][2] = y; // y
        
        ret[10][0] = 1; // moveTo instruction
        ret[10][1] = x + nThickness; // x
        ret[10][2] = y; // y
   
        ret[11][0] = 1; // lineTo instruction
        ret[11][1] = x + 2 * oneEigthMS - nThickness; // x
        ret[11][2] = y + oneThirdMS - oneTwentiethMS - nThickness; // y
        
        ret[12][0] = 1; // lineTo instruction
        ret[12][1] = x + 2 * oneEigthMS - nThickness; // x
        ret[12][2] = y; // y
        
        ret[13][0] = 1; // lineTo instruction
        ret[13][1] = x + 2 * oneEigthMS; // x
        ret[13][2] = y; // y
        
        ret[14][0] = 1; // lineTo instruction
        ret[14][1] = x + 2 * oneEigthMS; // x
        ret[14][2] = y + oneThirdMS - oneTwentiethMS; // y
        
        ret[15][0] = 1; // lineTo instruction
        ret[15][1] = x + 2 * oneEigthMS - nThickness; // x
        ret[15][2] = y + oneThirdMS - oneTwentiethMS; // y
 
        ret[16][0] = 1; // moveTo instruction
        ret[16][1] = x + nThickness; // x
        ret[16][2] = y + nThickness; // y
        
        ret[17][0] = 1; // lineTo instruction
        ret[17][1] = x + oneTwentiethMS; // x
        ret[17][2] = y + oneThirdMS - oneTwentiethMS; // y
        
        ret[18][0] = 1; // lineTo instruction
        ret[18][1] = x; // x
        ret[18][2] = y + oneThirdMS - oneTwentiethMS; // y
        
        return ret;
    }
    
    public static double[][] getStar(float markerSize) {
        double[][] ret = new double[4][3];
//        float halfMS = markerSize / 2.0f;
//        
//        ret[0][0] = 0; // moveTo instruction
//        ret[0][1] = 0; // x
//        ret[0][2] = -halfMS; // y
//        
//        ret[1][0] = 1; // lineTo instruction
//        ret[1][1] = halfMS; // x
//        ret[1][2] = halfMS; // y
//        
//        ret[2][0] = 1; // lineTo instruction
//        ret[2][1] = -halfMS; // x
//        ret[2][2] = halfMS; // y
//        
//        ret[3][0] = 1; // lineTo instruction
//        ret[3][1] = 0; // x
//        ret[3][2] = -halfMS; // y
        
        return ret; 
    }
    
    
    
}
