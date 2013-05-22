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

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapScale implements CartographicElement, Comparable<CartographicElement> {
    double pointsPerMetre = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() * 39.3701; 
    boolean visible = true;
    boolean selected = false;
    int number = -1;
    boolean showRepresentativeFraction = false;
    boolean showGraphicalScale = true;
    boolean borderVisible = false;
    boolean backgroundVisible = false;
    int upperLeftX = -32768;
    int upperLeftY = -32768;
    int height = 50; // in points
    int width = 150; // in points
    int margin = 10;
    double barLength = 5.0;
    int numberDivisions = 5;
    String units = "metres";
    double conversionToMetres = 1;
    Color backColour = Color.WHITE;
    Color borderColour = Color.BLACK;
    Color outlineColour = Color.BLACK;
    Color legendColour = Color.BLACK;
    Color fontColour = Color.BLACK;
    static DecimalFormat dfScale = new DecimalFormat("###,###,###.#");
    String representativeFraction;
    String lowerLabel = "0";
    String upperLabel = "5";
    String name = "mapScale";
    float lineWidth = 0.75f;
    private MapArea mapArea = null;
    private int selectedOffsetX;
    private int selectedOffsetY;
    boolean outlineVisible = false;
    Font labelFont = new Font("SanSerif", Font.PLAIN, 10);
    private ScaleStyle scaleStyle = ScaleStyle.STANDARD;
    
    public enum ScaleStyle {
        STANDARD, SIMPLE, COMPLEX;
    }

    public MapScale(String name) {
        this.name = name;
    }
    
    public boolean isRepresentativeFractionVisible() {
        return showRepresentativeFraction;
    }

    public void setRepresentativeFractionVisible(boolean showRepresentativeFraction) {
        this.showRepresentativeFraction = showRepresentativeFraction;
        if (!showRepresentativeFraction && !showGraphicalScale) {
            showGraphicalScale = true;
    }
    }

    public boolean isGraphicalScaleVisible() {
        return showGraphicalScale;
    }

    public void setGraphicalScaleVisible(boolean showGraphicalScale) {
        this.showGraphicalScale = showGraphicalScale;
        if (!showRepresentativeFraction && !showGraphicalScale) {
            showRepresentativeFraction = true;
        }
    }
    
    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
        if (units.toLowerCase().contains("met")) {
            conversionToMetres = 1.0;
        } else if (units.toLowerCase().equals("m")) {
            conversionToMetres = 1.0;
        } else if (units.toLowerCase().contains("feet")) {
            conversionToMetres = 0.3048;
        } else if (units.toLowerCase().contains("ft")) {
            conversionToMetres = 0.3048;
        } else if (units.toLowerCase().contains("miles")) {
            conversionToMetres = 1609.34;
        } else if (units.toLowerCase().contains("mi")) {
            conversionToMetres = 1609.34;
        } else if (units.toLowerCase().contains("kilo")) {
            conversionToMetres = 1000;
        } else if (units.toLowerCase().contains("km")) {
            conversionToMetres = 1000;
        } else {
            this.units = "metres";
            conversionToMetres = 1.0;
        }
    }

    @Override
    public int getUpperLeftX() {
        return upperLeftX;
    }

    @Override
    public void setUpperLeftX(int upperLeftX) {
        this.upperLeftX = upperLeftX;
    }

    @Override
    public int getUpperLeftY() {
        return upperLeftY;
    }

    @Override
    public void setUpperLeftY(int upperLeftY) {
        this.upperLeftY = upperLeftY;
    }
    
    @Override
    public int getLowerRightX() {
        return upperLeftX + width;
    }

    @Override
    public int getLowerRightY() {
        return upperLeftY + height;
    }

    @Override
    public int getSelectedOffsetX() {
        return selectedOffsetX;
    }

    @Override
    public void setSelectedOffsetX(int selectedOffsetX) {
        this.selectedOffsetX = selectedOffsetX;
    }

    @Override
    public int getSelectedOffsetY() {
        return selectedOffsetY;
    }

    @Override
    public void setSelectedOffsetY(int selectedOffsetY) {
        this.selectedOffsetY = selectedOffsetY;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public ScaleStyle getScaleStyle() {
        return scaleStyle;
    }

    public void setScaleStyle(ScaleStyle scaleStyle) {
        this.scaleStyle = scaleStyle;
    }
    
    int mapAreaElementNumber = -1;
    public int getMapAreaElementNumber() {
        if (mapAreaElementNumber < 0) {
            mapAreaElementNumber = mapArea.getElementNumber();
        }
        return mapAreaElementNumber;
    }
    
    public void setMapAreaElementNumber(int num) {
        this.mapAreaElementNumber = num;
    }
    
    public boolean isOutlineVisible() {
        return outlineVisible;
    }

    public void setOutlineVisible(boolean outlineVisible) {
        this.outlineVisible = outlineVisible;
    }
    
    public double getBarLength() {
        return barLength;
    }

//    public void setBarLength(double barLength) {
//        this.barLength = barLength;
//    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getNumberDivisions() {
        return numberDivisions;
    }

    public void setNumberDivisions(int numberDivisions) {
        this.numberDivisions = numberDivisions;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color outlineColour) {
        this.borderColour = outlineColour;
    }

    public Color getLegendColour() {
        return legendColour;
    }

    public void setLegendColour(Color legendColour) {
        this.legendColour = legendColour;
    }
    
    public Color getFontColour() {
        return fontColour;
    }

    public void setFontColour(Color fontColour) {
        this.fontColour = fontColour;
    }
    
    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public Font getLabelFont() {
        return labelFont;
    }

    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
    }
    
    public double getScale() {
        return scale;
//        if (mapArea != null) {
//            double scale = mapArea.getScale();
//            //what is the width of the scale box in ground units?
//            double widthGU = (width - 4 * margin) / pointsPerMetre * scale / conversionToMetres;
//            // the number of divisions can range between 2 and 10
//            // given this, figure out what the appropriate division length is
//            double[] possibleLengths = {0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0, 50.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0, 50000.0, 100000.0};
//            int[] numDecimals = {3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//            int closestDivision = 0;
//            double closestDivisionBarLength = 0;
//            double minDist = Float.POSITIVE_INFINITY;
//            double dist;
//            int numDecimalsInLabel = 0;
//            for (int a = 2; a <= 10; a++) {
//                for (int b = 0; b < possibleLengths.length; b++) {
//                    dist = Math.abs(widthGU - (a * possibleLengths[b]));
//                    if ((dist < minDist) && (a * possibleLengths[b] < widthGU)) {
//                        minDist = dist;
//                        closestDivision = a;
//                        closestDivisionBarLength = possibleLengths[b];
//                        numDecimalsInLabel = numDecimals[b];
//                    }
//                }
//            }
//            barLength = closestDivisionBarLength * closestDivision;
//            numberDivisions = closestDivision;
//
//            // what are the upper and lower labels?
//            String formatString = "0";
//            if (numDecimalsInLabel > 0) {
//                formatString += ".";
//                for (int a = 0; a < numDecimalsInLabel; a++) {
//                    formatString += "0";
//                }
//            }
//            DecimalFormat df = new DecimalFormat(formatString);
//            lowerLabel = df.format(0.0);
//            upperLabel = df.format(barLength);
//
//
//            return scale;
//        } else {
//            return 0.0;
//        }
    }

    private double scale = 0;
    public void setScale() {
        representativeFraction = "Scale 1:" + dfScale.format(scale);
        this.scale = mapArea.getScale();
        //what is the width of the scale box in ground units?
        double widthGU = (width - 4 * margin) / pointsPerMetre * scale / conversionToMetres;
        // the number of divisions can range between 2 and 10
        // given this, figure out what the appropriate division length is
        double[] possibleLengths = {0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0, 50.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0, 50000.0, 100000.0};
        int[] numDecimals = {3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int closestDivision = 0;
        double closestDivisionBarLength = 0;
        double minDist = Float.POSITIVE_INFINITY;
        double dist;
        int numDecimalsInLabel = 0;
        for (int a = 2; a <= 10; a++) {
            for (int b = 0; b < possibleLengths.length; b++) {
                dist = Math.abs(widthGU - (a * possibleLengths[b]));
                if ((dist < minDist) && (a * possibleLengths[b] < widthGU)) {
                    minDist = dist;
                    closestDivision = a;
                    closestDivisionBarLength = possibleLengths[b];
                    numDecimalsInLabel = numDecimals[b];
                }
            }
        }
        barLength = closestDivisionBarLength * closestDivision;
        numberDivisions = closestDivision;
        
        // what are the upper and lower labels?
        String formatString = "0";
        if (numDecimalsInLabel > 0) {
            formatString += ".";
            for (int a = 0; a < numDecimalsInLabel; a++) {
                formatString += "0";
            }
        }
        DecimalFormat df = new DecimalFormat(formatString);
        lowerLabel = df.format(0.0);
        upperLabel = df.format(barLength);
        
    }
    
    public String getRepresentativeFraction() {
        return representativeFraction;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int scaleMargin) {
        this.margin = scaleMargin;
    }

    public double getConversionToMetres() {
        return conversionToMetres;
    }

    public void setConversionToMetres(double conversionToMetres) {
        this.conversionToMetres = conversionToMetres;
    }

    public String getLowerLabel() {
        return lowerLabel;
    }

    public void setLowerLabel(String lowerLabel) {
        this.lowerLabel = lowerLabel;
    }

    public String getUpperLabel() {
        return upperLabel;
    }

    public void setUpperLabel(String upperLabel) {
        this.upperLabel = upperLabel;
    }

    public boolean isBackgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Color getOutlineColour() {
        return outlineColour;
    }

    public void setOutlineColour(Color outlineColour) {
        this.outlineColour = outlineColour;
    }

    @Override
    public int getElementNumber() {
        return number;
    }

    @Override
    public void setElementNumber(int number) {
        this.number = number;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public MapArea getMapArea() {
        return mapArea;
    }

    public void setMapArea(MapArea mapArea) {
        this.mapArea = mapArea;
        getScale();
    }
    
    
    
    @Override
    public int compareTo(CartographicElement other) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        
        // compare them based on their element (overlay) numbers
        if (this.number < other.getElementNumber()) {
            return BEFORE;
        } else if (this.number > other.getElementNumber()) {
            return AFTER;
        }

        return EQUAL;
    }

    @Override
    public void resize(int x, int y, int resizeMode) {
        int minSizeX = 60;
        int minSizeY = 30;
        int deltaX = 0;
        int deltaY = 0;
        switch (resizeMode) {
            case 0: // off the north edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSizeY) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                break;
            case 1: // off the south edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSizeY) {
                    height += deltaY;
                }
                break;
            case 2: // off the east edge
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSizeX) {
                    width += deltaX;
                }
                break;
            case 3: // off the west edge
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSizeX) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
            case 4: // off the northeast edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSizeY) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSizeX) {
                    width += deltaX;
                }
                break;
            case 5: // off the northwest edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSizeY) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSizeX) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
            case 6: // off the southeast edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSizeY) {
                    height += deltaY;
                }
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSizeX) {
                    width += deltaX;
                }
                break;
            case 7: // off the southwest edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSizeY) {
                    height += deltaY;
                }
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSizeX) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
        }
    }
    
    @Override
    public CartographicElementType getCartographicElementType() {
        return CartographicElementType.MAP_SCALE;
    }
}
