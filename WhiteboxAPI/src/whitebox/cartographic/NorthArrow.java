/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.cartographic;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import whitebox.cartographic.NorthArrowMarkers.MarkerStyle;

/**
 *
 * @author johnlindsay
 */
public class NorthArrow {
    boolean visible = false;
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
            
    public boolean isVisible() {
        return visible;
    }

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
}
