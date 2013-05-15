/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.ui;

import javax.swing.*;

/**
 *
 * @author johnlindsay
 */
public class ImageFileChooser extends JFileChooser {
    
    public ImageFileChooser() {
        addChoosableFileFilter(new ImageFilter());
        setAcceptAllFileFilterUsed(false);
 
        //Add custom icons for file types.
        setFileView(new ImageFileView());
 
        //Add the preview pane.
        setAccessory(new ImagePreview(this));
    }
}
