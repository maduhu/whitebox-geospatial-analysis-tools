/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.cartographic;

import java.awt.Color;
import java.text.DecimalFormat;
/**
 *
 * @author johnlindsay
 */
public class MapScale {
    double pointsPerMetre = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() * 39.3701; 
    boolean visible = false;
    boolean showRepresentativeFraction = false;
    boolean borderVisible = false;
    boolean backgroundVisible = false;
    int upperLeftX = -99;
    int upperLeftY = -99;
    int height = 50; // in points
    int width = 150; // in points
    int margin = 10;
    double barLength = 5.0;
    int numberDivisions = 5;
    String units = "kilometres";
    double conversionToMetres = 1000;
    Color backColour = Color.WHITE;
    Color outlineColour = Color.BLACK;
    Color legendColour = Color.BLACK;
    double scale = 0;
    DecimalFormat dfScale = new DecimalFormat("###,###,###.#");
    String representativeFraction;
    String lowerLabel = "0";
    String upperLabel = "5";

    public boolean isRepresentativeFractionVisible() {
        return showRepresentativeFraction;
    }

    public void setRepresentativeFractionVisible(boolean showRepresentativeFraction) {
        this.showRepresentativeFraction = showRepresentativeFraction;
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

    public int getUpperLeftX() {
        return upperLeftX;
    }

    public void setUpperLeftX(int upperLeftX) {
        this.upperLeftX = upperLeftX;
    }

    public int getUpperLeftY() {
        return upperLeftY;
    }

    public void setUpperLeftY(int upperLeftY) {
        this.upperLeftY = upperLeftY;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        upperLeftX = -99;
        upperLeftY = -99;
    }

    public double getBarLength() {
        return barLength;
    }

    public void setBarLength(double barLength) {
        this.barLength = barLength;
    }

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

    public Color getOutlineColour() {
        return outlineColour;
    }

    public void setOutlineColour(Color outlineColour) {
        this.outlineColour = outlineColour;
    }

    public Color getLegendColour() {
        return legendColour;
    }

    public void setLegendColour(Color legendColour) {
        this.legendColour = legendColour;
    }
    
    
    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        representativeFraction = "Scale 1:" + dfScale.format(scale);
        this.scale = scale;
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
    
}
