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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import whitebox.cartographic.NorthArrowMarkers.MarkerStyle;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class NorthArrow implements CartographicElement, Comparable<CartographicElement> {
    boolean visible = false;
    boolean selected = false;
    String name = "north arrow";
    int number = -1;
    int x = -1;
    int y = -1;
    int markerSize = 40;
    int margin = 4;
    boolean borderVisible = false;
    boolean backgroundVisible = false;
    Color backColour = Color.WHITE;
    Color borderColour = Color.BLACK;
    Color outlineColour = Color.BLACK;
    MarkerStyle markerStyle = MarkerStyle.STANDARD;
            
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        x = -1;
        y = -1;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getMarkerSize() {
        return markerSize;
    }

    public void setMarkerSize(int markerSize) {
        this.markerSize = markerSize;
    }

    public MarkerStyle getMarkerStyle() {
        return markerStyle;
    }

    public void setMarkerStyle(MarkerStyle markerStyle) {
        this.markerStyle = markerStyle;
    }

    public int getHeight() {
        return markerSize;
    }

    public int getWidth() {
        return markerSize;
    }

    public Color getOutlineColour() {
        return outlineColour;
    }

    public void setOutlineColour(Color outlineColour) {
        this.outlineColour = outlineColour;
    }

    public int getUpperLeftX() {
        return (int)(x - markerSize / 2.0);
    }

    public int getUpperLeftY() {
        return (int)(y - markerSize / 2.0);
    }
    
    public int getLowerRightX() {
        return (int)(x + markerSize / 2.0);
    }

    public int getLowerRightY() {
        return (int)(y + markerSize / 2.0);
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public boolean isBackgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color borderColour) {
        this.borderColour = borderColour;
    }

    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        if (margin > markerSize) {
            markerSize = margin * 3;
        }
        this.margin = margin;
    }
    
    public ArrayList<GeneralPath> getMarkerData() {
        ArrayList<GeneralPath> ret = new ArrayList<GeneralPath>();
        markerDrawingInstructions.clear();
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
        double[][] xyData = NorthArrowMarkers.getMarkerData(markerStyle, markerSize - 2 * margin);
        boolean drawing = false;
        for (int a = 0; a < xyData.length; a++) {
            if (xyData[a][0] == 0) { // moveTo no fill
                if (drawing) {
                    ret.add(gp);
                }
                drawing = true;
                markerDrawingInstructions.add(new Integer(0));
                gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                gp.moveTo(x + xyData[a][1], y + xyData[a][2]);
            } else if (xyData[a][0] == 1) { // lineTo
                gp.lineTo(x + xyData[a][1], y + xyData[a][2]);
            } else if (xyData[a][0] == 2) { // elipse2D
                Ellipse2D circle = new Ellipse2D.Double((x - xyData[a][1]), (y - xyData[a][1]), xyData[a][2], xyData[a][2]);
                gp.append(circle, true);
            } else if (xyData[a][0] == 3) { // moveTo with fill
                if (drawing) {
                    ret.add(gp);
                }
                drawing = true;
                markerDrawingInstructions.add(new Integer(1));
                gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                gp.moveTo(x + xyData[a][1], y + xyData[a][2]);
            }
        }
        ret.add(gp);
        
        return ret;
    }
    
    ArrayList<Integer> markerDrawingInstructions = new ArrayList<Integer>();
    public ArrayList<Integer> getMarkerDrawingInstructions() {
        return markerDrawingInstructions;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
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
    
    @Override
    public int compareTo(CartographicElement other) {
        final int BEFORE = 1;
        final int EQUAL = 0;
        final int AFTER = -1;
        
        // compare them based on their element (overlay) numbers
        if (this.number < other.getElementNumber()) {
            return BEFORE;
        } else if (this.number > other.getElementNumber()) {
            return AFTER;
        }

        return EQUAL;
    }
}
