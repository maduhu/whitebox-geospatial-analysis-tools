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

import java.awt.*;
import javax.swing.*;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
class LineStyleComboBoxRenderer extends JLabel implements ListCellRenderer {

    float[][] dashArray;
    float lineThickness;
    Color lineColour;
    public LineStyleComboBoxRenderer(float[][] dashArray, float lineThickness, Color lineColour) {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
        this.dashArray = dashArray.clone();
        this.lineColour = lineColour;
        this.lineThickness = lineThickness;
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
        
        DashedLineSample dls = new DashedLineSample(80, 24, selectedIndex, dashArray[selectedIndex], 
                backColour, lineThickness, lineColour);
        returnValue = dls;
        return returnValue;
    }
    
    
}