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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
//import org.lobobrowser.gui.FramePanel;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class HTMLViewer extends JFrame implements HyperlinkListener {

    private ArrayList<String> helpHistory = new ArrayList<>();
    private int helpHistoryIndex = 0;
    JEditorPane helpPane = new JEditorPane();

    public HTMLViewer(String fileOrURL) throws Exception {

        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);

            System.setProperty("apple.laf.useScreenMenuBar", "true");
            //System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Whitebox GAT");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
            //System.setProperty("Xdock:name", "Whitebox");
            System.setProperty("apple.awt.fileDialogForDirectories", "true");

            System.setProperty("apple.awt.textantialiasing", "true");

            System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
        }

//        if (System.getProperty("mrj.version") != null) {
//            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
//            System.setProperty("apple.laf.useScreenMenuBar", "true");
//        }
        helpPane.addHyperlinkListener(this);
        helpPane.setContentType("text/html");

        JScrollPane helpScroll = new JScrollPane(helpPane);
        this.getContentPane().add(helpScroll);

        if (helpHistoryIndex == helpHistory.size() - 1) {
            helpHistory.add(fileOrURL);
            helpHistoryIndex = helpHistory.size() - 1;
        } else {
            for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                helpHistory.remove(i);
            }
            helpHistory.add(fileOrURL);
            helpHistoryIndex = helpHistory.size() - 1;
        }
        try {
            if (!fileOrURL.toLowerCase().startsWith("http://")) {
                helpPane.setPage(new URL("file:///" + fileOrURL));
            } else {
                helpPane.setPage(new URL(fileOrURL));
            }
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        }

        // first off, is it a file or string?
//        if (fileOrURL.toLowerCase().endsWith(".html")) {
        this.setTitle("HTML Viewer: " + (new File(fileOrURL)).getName());

//        // This optional step initializes logging so only warnings
//        // are printed out.
//        PlatformInit.getInstance().initLogging(false);
//
//        // This step is necessary for extensions to work:
//        PlatformInit.getInstance().init(false, false);
//        } else {
//            this.setTitle("HTML Viewer");
//
//        }
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                if (helpHistoryIndex == helpHistory.size() - 1) {
                    helpHistory.add(event.getURL().getFile());
                    helpHistoryIndex = helpHistory.size() - 1;
                } else {
                    for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                        helpHistory.remove(i);
                    }
                    helpHistory.add(event.getURL().getFile());
                    helpHistoryIndex = helpHistory.size() - 1;
                }
                helpPane.setPage(event.getURL());
            } catch (IOException ioe) {
                // Some warning to user
            }
        }
    }
}
