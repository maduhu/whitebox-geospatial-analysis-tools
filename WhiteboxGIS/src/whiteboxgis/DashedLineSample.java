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
public class DashedLineSample extends JPanel {
    Color backColour = Color.white;
    int markerSize;
    boolean isFilled;
    boolean isOutlined;
    int index;
    float[] dashArray;
    float lineThickness;
    Color lineColour;
    
    public DashedLineSample(int width, int height, int index, float[] dashArray, Color backColour,
            float lineThickness, Color lineColour) {
        this.setMaximumSize(new Dimension(width, height));
        this.setPreferredSize(new Dimension(width, height));
        this.index = index;
        this.dashArray = dashArray.clone();
        this.backColour = backColour;
        this.lineColour = lineColour;
        this.lineThickness = lineThickness;
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

            double left = 10.0;
            double right = width - 10.0;
            
            g2d.setColor(lineColour);
            BasicStroke myStroke = new BasicStroke(lineThickness);
            
            if (index > 0) {
                myStroke =
                    new BasicStroke(lineThickness,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f, dashArray, 0.0f);
                
            }  

            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(myStroke);
    
            GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
            double middle = height / 2.0;
            polyline.moveTo(left, middle);
            polyline.lineTo(right, middle);

            g2d.draw(polyline);

            g2d.setStroke(oldStroke);


            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }
}
