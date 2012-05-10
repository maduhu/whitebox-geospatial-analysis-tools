package whiteboxgis;

import java.io.File;
import javax.swing.JFrame;
import org.lobobrowser.gui.FramePanel;

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
