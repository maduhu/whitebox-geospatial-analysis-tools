package whiteboxgis;

import org.lobobrowser.gui.*;
//import org.lobobrowser.main.*;
import javax.swing.*;
import java.io.File;
public class HTMLViewer extends JFrame {

//    public static void main(String[] args) throws Exception {
////        // This optional step initializes logging so only warnings
////        // are printed out.
////        PlatformInit.getInstance().initLogging(false);
////
////        // This step is necessary for extensions to work:
////        PlatformInit.getInstance().init(false, false);
//
//        // Create frame with a specific size.
//        JFrame frame = new HTMLViewer("/Users/johnlindsay/Documents/Data/LandsatData/KIA.html"); //"http://www.uoguelph.ca/~hydrogeo/Whitebox/Help/MainHelp.html");
//        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        frame.setSize(600, 400);
//        frame.setVisible(true);
//    }

    public HTMLViewer(String fileOrURL) throws Exception {
        super("HTML Viewer: " + (new File(fileOrURL)).getName());
        
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        if (System.getProperty("mrj.version") != null) {
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        
//        // This optional step initializes logging so only warnings
//        // are printed out.
//        PlatformInit.getInstance().initLogging(false);
//
//        // This step is necessary for extensions to work:
//        PlatformInit.getInstance().init(false, false);

        FramePanel framePanel = new FramePanel();
        this.getContentPane().add(framePanel);
        framePanel.navigate(fileOrURL);
    }
}
