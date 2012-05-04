package whiteboxgis;

import javax.swing.*;

/**
 *
 * @author johnlindsay
 */
public class Main {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //setLookAndFeel("Nimbus");
            setLookAndFeel("systemLAF");

            WhiteboxGui wb = new WhiteboxGui();
            wb.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            wb.setVisible(true);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private static void setLookAndFeel(String lafName) {
        try {
            if (System.getProperty("os.name").contains("Mac")) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Whitebox GAT");
                System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
                System.setProperty("Xdock:name", "Whitebox");
                System.setProperty("Xdock:icon", "wbGAT.png");
                System.setProperty("apple.awt.fileDialogForDirectories", "true");
            }

            if (lafName.equals("systemLAF")) {
                lafName = getSystemLookAndFeelName();
            }

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (lafName.equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static String getSystemLookAndFeelName() {
        String className = UIManager.getSystemLookAndFeelClassName();
        String name = null;
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (className.equals(info.getClassName())) {
                name = info.getName();
                break;
            }
        }
        return name;
    }
}
