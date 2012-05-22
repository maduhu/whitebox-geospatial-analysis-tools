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

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
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
