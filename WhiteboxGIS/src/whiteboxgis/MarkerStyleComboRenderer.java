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
import java.util.ArrayList;


/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
class MarkerStyleComboBoxRenderer extends JLabel implements ListCellRenderer {

    private ArrayList<double[][]> markers = new ArrayList<double[][]>();
    float lineThickness;
    Color lineColour;
    Color fillColour;
    float markerSize;
    boolean isFilled;
    boolean isOutlined;
    public MarkerStyleComboBoxRenderer(ArrayList<double[][]> markers, float lineThickness, 
            Color lineColour, Color fillColour, float markerSize, boolean isFilled,
            boolean isOutlined) {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
        this.markers = markers;
        this.lineColour = lineColour;
        this.fillColour = fillColour;
        this.lineThickness = lineThickness;
        this.markerSize = markerSize;
        this.isFilled = isFilled;
        this.isOutlined = isOutlined;
    }

    /*
     * This method finds the image and text corresponding
     * to the selected value and returns the label, set up
     * to display the text and image.
     */
    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        
        Component returnValue = null;
        
        //Get the selected index. (The index param isn't
        //always valid, so just use the value.)
        int selectedIndex = ((Integer) value).intValue();

        Color backColour;
        if (isSelected) {
            backColour = list.getSelectionBackground();
        } else {
            backColour = list.getBackground();
        }
        
        MarkerSample ms = new MarkerSample(80, 24, selectedIndex, markers.get(selectedIndex), 
                backColour, lineThickness, lineColour, fillColour, markerSize,
                isFilled, isOutlined);
        returnValue = ms;
        return returnValue;
    }
    
    
}