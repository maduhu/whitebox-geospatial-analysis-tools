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

import java.io.File;
import javax.swing.JFrame;
import org.lobobrowser.gui.FramePanel;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class HTMLViewer extends JFrame {

    public HTMLViewer(String fileOrURL) throws Exception {

        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        if (System.getProperty("mrj.version") != null) {
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        
        FramePanel framePanel = new FramePanel();
        this.getContentPane().add(framePanel);
        
        // first off, is it a file or string?
//        if (fileOrURL.toLowerCase().endsWith(".html")) {
            this.setTitle("HTML Viewer: " + (new File(fileOrURL)).getName());

//        // This optional step initializes logging so only warnings
//        // are printed out.
//        PlatformInit.getInstance().initLogging(false);
//
//        // This step is necessary for extensions to work:
//        PlatformInit.getInstance().init(false, false);

            framePanel.navigate(fileOrURL);
//        } else {
//            this.setTitle("HTML Viewer");
//
//        }
    }
}
