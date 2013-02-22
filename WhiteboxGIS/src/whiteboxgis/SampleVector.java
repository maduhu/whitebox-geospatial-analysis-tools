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
package whiteboxgis;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.cartographic.PointMarkers;
import whiteboxgis.VectorLayerInfo.LegendEntry;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class SampleVector extends JPanel {
    private ShapeType st;
    private VectorLayerInfo vli;
    private Color backColour = Color.white;
    private Color fillColour;
    private Color lineColour;
    private float lineThickness;
    private float markerSize;
    private boolean isFilled;
    private boolean isOutlined;
    private boolean bentLine = true;
    private boolean isFilledWithOneColour = false;
    private boolean isOutlinedWithOneColour = false;
    private LegendEntry[] le;
    private int numEntries = 1;
    private double margin = 7.0;
    private double sqrSize = 18.0;
    private double spacing = 3.0;
                        
    public SampleVector(ShapeType shapeType, VectorLayerInfo vli, boolean bentLine) {
        this.st = shapeType;
        this.vli = vli;
        
        fillColour = vli.getFillColour();
        lineColour = vli.getLineColour();
        lineThickness = vli.getLineThickness();
        markerSize = vli.getMarkerSize();
        isFilled = vli.isFilled();
        isOutlined = vli.isOutlined();
        this.bentLine = bentLine;
        
        /* figure out the width and height. This will depend on the
         * fill method. If it is being filled using an attribute field
         * then it may have a greater than normal height.
        */
        this.isFilledWithOneColour = vli.isFilledWithOneColour();
        this.isOutlinedWithOneColour = vli.isOutlinedWithOneColour();
        
        int width = 100;
        int height = 30;
        
        
        if (!isFilledWithOneColour || !isOutlinedWithOneColour) {
            // how many legend entries are there?
            le = vli.getLegendEntries();
            numEntries = le.length;
            height = (int)(margin + numEntries * (sqrSize + spacing) + margin);
            width = 170;
        }
        this.setMaximumSize(new Dimension(width, height));
        this.setPreferredSize(new Dimension(width, height));
        
    }

    public int getNumEntries() {
        return numEntries;
    }
    
    @Override
    public void paint (Graphics g) {
        try {
            int width = getWidth();
            int height = getHeight();
            double x1, y1;
            double halfMS = markerSize / 2.0;
            Graphics2D g2d = (Graphics2D)g;
            
            RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
            rh = new RenderingHints(
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHints(rh);
            rh = new RenderingHints(
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            g2d.setRenderingHints(rh);
            rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
            
            g2d.setColor(backColour);
            g2d.fillRect(0, 0, width, height);
                    
            if (st.getBaseType() == ShapeType.POLYGON || st == ShapeType.MULTIPATCH) {
                double top = 7.0;
                double bottom = height - 7.0;
                double left = 15.0;
                double right = width - 15.0;
                GeneralPath polyline;
                if (fillColour.equals(Color.white) && !isOutlined) {
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.fillRect(0, 0, width, height);

                }
                if (isFilled) {
                    if (isFilledWithOneColour) {
                        g2d.setColor(fillColour);
                        polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                        polyline.moveTo(left, bottom);
                        polyline.lineTo(left, top);
                        polyline.lineTo(right, top);
                        polyline.lineTo(right, bottom);
                        polyline.closePath();
                        g2d.fill(polyline);
                    } else {
                        double t, b;
                        double r = margin + sqrSize;
                        for (int j = 0; j < numEntries; j++) {
                            t = margin + j * (sqrSize + spacing);
                            b = t + sqrSize;
                            g2d.setColor(le[j].legendColour);
                            polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                            polyline.moveTo(margin, b);
                            polyline.lineTo(margin, t);
                            polyline.lineTo(r, t);
                            polyline.lineTo(r, b);
                            polyline.closePath();
                            g2d.fill(polyline);
                        }
                    }
                    
                }

                if (isOutlined) {
                    BasicStroke myStroke = new BasicStroke(lineThickness);
                    if (vli.isDashed()) {
                        myStroke =
                                new BasicStroke(lineThickness,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER,
                                10.0f, vli.getDashArray(), 0.0f);
                    }
                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(myStroke);

                    g2d.setColor(lineColour);
                    if (isFilledWithOneColour) {
                        polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                        polyline.moveTo(left, bottom);
                        polyline.lineTo(left, top);
                        polyline.lineTo(right, top);
                        polyline.lineTo(right, bottom);
                        polyline.closePath();
                        g2d.draw(polyline);
                    } else {
                        double t, b;
                        double r = margin + sqrSize;
                        for (int j = 0; j < numEntries; j++) {
                            t = margin + j * (sqrSize + spacing);
                            b = t + sqrSize;
                            polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                            polyline.moveTo(margin, b);
                            polyline.lineTo(margin, t);
                            polyline.lineTo(r, t);
                            polyline.lineTo(r, b);
                            polyline.closePath();
                            g2d.draw(polyline);
                        }
                    }
                    g2d.setStroke(oldStroke);
                }
                
                if (!isFilledWithOneColour) {
                    Font font = new Font("SanSerif", Font.PLAIN, 11);
                    g2d.setFont(font);
                    FontMetrics metrics = g.getFontMetrics(font);
                    double hgt = metrics.getHeight() / 4.0; // why a quarter rather than a half? I really can't figure it out either. But it works.
                    g2d.setColor(Color.BLACK);
                    double vOffset = (sqrSize / 2.0) - hgt;
                    double t, b;
                    double r = margin + sqrSize + 6;
                    for (int j = 0; j < numEntries; j++) {
                        t = margin + j * (sqrSize + spacing);
                        b = t + sqrSize;
                        String label = le[j].getLegendLabel().trim();
                        g2d.drawString(label, (float) (r), (float) (b - vOffset));
                    }

                }
            } else if (st.getBaseType() == ShapeType.POINT || st.getBaseType() == ShapeType.MULTIPOINT) {
                
                double[][] xyData = PointMarkers.getMarkerData(vli.getMarkerStyle(), vli.getMarkerSize());
                
                if (isFilledWithOneColour) {
                    x1 = width / 2.0;
                    y1 = height / 2.0;
                    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                    for (int a = 0; a < xyData.length; a++) {
                        if (xyData[a][0] == 0) { // moveTo
                            gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                        } else if (xyData[a][0] == 1) { // lineTo
                            gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                        } else if (xyData[a][0] == 2) { // elipse2D
                            Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);
                            gp.append(circle, true);
                        }
                    }
                    if (isFilled) {
                        g2d.setColor(fillColour);
                        g2d.fill(gp);
                    }
                    if (isOutlined) {
                        BasicStroke myStroke = new BasicStroke(lineThickness);
                        Stroke oldStroke = g2d.getStroke();
                        g2d.setStroke(myStroke);

                        g2d.setColor(lineColour);
                        g2d.draw(gp);

                        g2d.setStroke(oldStroke);
                    }
                } else {
                    double t;
                    for (int j = 0; j < numEntries; j++) {
                        t = margin + j * (sqrSize + spacing);
                        x1 = margin + sqrSize / 2.0;
                        y1 = t + sqrSize / 2.0;
                        GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                        for (int a = 0; a < xyData.length; a++) {
                            if (xyData[a][0] == 0) { // moveTo
                                gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                            } else if (xyData[a][0] == 1) { // lineTo
                                gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                            } else if (xyData[a][0] == 2) { // elipse2D
                                Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);
                                gp.append(circle, true);
                            }
                        }
                        if (isFilled) {
                            g2d.setColor(le[j].legendColour);
                            g2d.fill(gp);
                        }
                        if (isOutlined) {
                            BasicStroke myStroke = new BasicStroke(lineThickness);
                            Stroke oldStroke = g2d.getStroke();
                            g2d.setStroke(myStroke);

                            g2d.setColor(lineColour);
                            g2d.draw(gp);

                            g2d.setStroke(oldStroke);
                        }
                    }
                    Font font = new Font("SanSerif", Font.PLAIN, 11);
                    g2d.setFont(font);
                    FontMetrics metrics = g.getFontMetrics(font);
                    double hgt = metrics.getHeight() / 4.0; // why a quarter rather than a half? I really can't figure it out either. But it works.
                    g2d.setColor(Color.BLACK);
                    double vOffset = (sqrSize / 2.0) - hgt;
                    double b;
                    double r = margin + sqrSize + 6;
                    for (int j = 0; j < numEntries; j++) {
                        t = margin + j * (sqrSize + spacing);
                        b = t + sqrSize;
                        String label = le[j].getLegendLabel().trim();
                        g2d.drawString(label, (float) (r), (float) (b - vOffset));
                    }
                }
            } else if (st.getBaseType() == ShapeType.POLYLINE) {
                
                double top = 7.0;
                double bottom = height - 7.0;
                double left = 10.0;
                double right = width - 10.0;
                double oneThirdWidth = (right - left) / 3.0;
                
                if (isOutlinedWithOneColour) {
                    if (lineColour.equals(Color.white)) {
                        g2d.setColor(Color.LIGHT_GRAY);
                        g2d.fillRect(0, 0, width, height);

                    }
                    g2d.setColor(lineColour);
                    BasicStroke myStroke = new BasicStroke(lineThickness);
                    if (vli.isDashed()) {
                        myStroke =
                                new BasicStroke(lineThickness,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER,
                                10.0f, vli.getDashArray(), 0.0f);
                    }

                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(myStroke);

                    GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                    if (bentLine) {
                        polyline.moveTo(left, bottom);
                        polyline.lineTo(left + oneThirdWidth, top);
                        polyline.lineTo(left + oneThirdWidth * 2, bottom);
                        polyline.lineTo(left + oneThirdWidth * 3, top);
                    } else {
                        double middle = height / 2.0;
                        polyline.moveTo(left, middle);
                        polyline.lineTo(right, middle);
                    }
                    g2d.draw(polyline);

                    g2d.setStroke(oldStroke);
                } else {
                    BasicStroke myStroke = new BasicStroke(lineThickness);
                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(myStroke);

                    double t, b;
                    double r = margin + 2 * sqrSize;
                    oneThirdWidth = (r - margin) / 3.0;
                    for (int j = 0; j < numEntries; j++) {
                        t = margin + j * (sqrSize + spacing);
                        b = t + sqrSize;
                        x1 = margin + sqrSize / 2.0;
                        y1 = t + sqrSize / 2.0;
                        GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                        if (bentLine) {
                            polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                            polyline.moveTo(margin, b);
                            polyline.lineTo(margin + oneThirdWidth, t);
                            polyline.lineTo(margin + oneThirdWidth * 2, b);
                            polyline.lineTo(margin + oneThirdWidth * 3, t);
                        } else {
                            double middle = height / 2.0;
                            polyline.moveTo(left, middle);
                            polyline.lineTo(right, middle);
                        }
                        g2d.setColor(le[j].legendColour);
                        g2d.draw(polyline);

                    }
                    g2d.setStroke(oldStroke);
                    
                    Font font = new Font("SanSerif", Font.PLAIN, 11);
                    g2d.setFont(font);
                    FontMetrics metrics = g.getFontMetrics(font);
                    double hgt = metrics.getHeight() / 4.0; // why a quarter rather than a half? I really can't figure it out either. But it works.
                    g2d.setColor(Color.BLACK);
                    double vOffset = (sqrSize / 2.0) - hgt;
                    r = margin + 2 * sqrSize + 6;
                    for (int j = 0; j < numEntries; j++) {
                        t = margin + j * (sqrSize + spacing);
                        b = t + sqrSize;
                        String label = le[j].getLegendLabel().trim();
                        g2d.drawString(label, (float) (r), (float) (b - vOffset));
                    }
                }
                
            }
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }
}
