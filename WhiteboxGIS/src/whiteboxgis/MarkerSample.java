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

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MarkerSample extends JPanel {
    Color backColour = Color.white;
    float markerSize;
    boolean isFilled;
    boolean isOutlined;
    int index;
    float lineThickness;
    Color lineColour;
    Color fillColour;
    double[][] xyData;
    
    public MarkerSample(int width, int height, int index, double[][] xyData, Color backColour,
            float lineThickness, Color lineColour, Color fillColour, float markerSize, boolean isFilled,
            boolean isOutlined) {
        this.setMaximumSize(new Dimension(width, height));
        this.setPreferredSize(new Dimension(width, height));
        this.index = index;
        this.backColour = backColour;
        this.lineColour = lineColour;
        this.fillColour = fillColour;
        this.lineThickness = lineThickness;
        this.markerSize = markerSize;
        this.xyData = xyData;
        this.isFilled = isFilled;
        this.isOutlined = isOutlined;
    }
    
    @Override
    public void paint (Graphics g) {
        try {
            int width = getWidth();
            int height = getHeight();
            double x1, y1;
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
            
            if (lineColour.equals(Color.white)) {
                backColour = Color.lightGray;
            }
            g2d.setColor(backColour);
            g2d.fillRect(0, 0, width, height);

            g2d.setColor(lineColour);
            BasicStroke myStroke = new BasicStroke(lineThickness);
            
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(myStroke);
            
            double middleY = height / 2.0;
            double middleX = width / 2.0;
            double halfMarkerSize = markerSize / 2.0;

            if (index >= 0) {
    
                GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                for (int a = 0; a < xyData.length; a++) {
                    if (xyData[a][0] == 0) { // moveTo
                        polyline.moveTo(middleX + xyData[a][1], middleY + xyData[a][2]);
                    } else if (xyData[a][0] == 1) { // lineTo
                        polyline.lineTo(middleX + xyData[a][1], middleY + xyData[a][2]);
                    } else if (xyData[a][0] == 2) { // elipse2D
                        Ellipse2D circle = new Ellipse2D.Double((middleX - xyData[a][1]), (middleY - xyData[a][1]), xyData[a][2], xyData[a][2]);
                
                        polyline.append(circle, true);
                    }
                }
                
                g2d.setColor(fillColour);
                g2d.fill(polyline);
                g2d.setColor(lineColour);
                g2d.draw(polyline);
            } else {
                Ellipse2D circle = new Ellipse2D.Double((middleX - halfMarkerSize), (middleY - halfMarkerSize), markerSize, markerSize);
                if (isFilled) {
                    g2d.setColor(fillColour);
                    g2d.fill(circle);
                }
                if (isOutlined) {
                    g2d.setColor(lineColour);
                    g2d.draw(circle);
                }
            }

            g2d.setStroke(oldStroke);


            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }
}
