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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.management.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.util.List;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Main {
    private String[] args;
    private String applicationDirectory;
    private String pathSep;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
//            //setLookAndFeel("Nimbus");
//            setLookAndFeel("systemLAF");
//            
//            if (System.getProperty("os.name").contains("Mac")) {
//                System.setProperty("apple.laf.useScreenMenuBar", "true");
//                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Whitebox GAT");
//                System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
//                System.setProperty("Xdock:name", "Whitebox");
//                System.setProperty("Xdock:icon", "wbGAT.png");
//                System.setProperty("apple.awt.fileDialogForDirectories", "true");
//            }
            
//            WhiteboxGui wb = new WhiteboxGui();
//            wb.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//            wb.setVisible(true);
            
            

            //Print the jvm heap size.
            //System.out.println("Max heap size = " + getHeapSize() / 1073741824.0 + " Gb");
            //System.out.println("Free memory = " + Runtime.getRuntime().freeMemory() / 1048576.0 + " Mb");
            //System.out.println("Total memory = " + Runtime.getRuntime().totalMemory() / 1048576.0 + " Mb");
            //System.out.println("System memory = " + ((com.sun.management.OperatingSystemMXBean) ManagementFactory
            //    .getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / 1073741824.0 + " Gb"); //1048576.0);
            //System.out.println(System.getProperty("user.name"));
            //System.getProperties().list(System.out);
            
            Main main = new Main();
            main.args = args;
            main.launchProgram();
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void launchProgram() {
        try {
            List inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            boolean isDebug = inputArgs.contains("-Xdebug");

            if (!isDebug) {
                boolean flag = false;
                long amountOfMemory = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
                int heapSize;
                String heapSizeUnit = "G";
                if (System.getProperty("sun.arch.data.model").contains("32")) {
                    heapSizeUnit = "M";
                    if (196608 < amountOfMemory / 2) {
                        heapSize = 1536;
                    } else {
                        heapSize = (int) (amountOfMemory / 2);
                    }
                } else {
                    heapSize = (int) (amountOfMemory / 1073741824 / 1.5);
                }

                if (!startSecondJVM(heapSize, heapSizeUnit)) {
                    if (!GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(null, "There was a problem "
                                + "initializing the JVM heap size.",
                                "Error starting application",
                                javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
                System.exit(0);
            } else {
                WhiteboxGui.main(args);
            }
        } catch (Exception e) {
            
        }
    }
    
    private boolean startSecondJVM(int heapSize, String heapSizeUnit) {
        try {
            String xmx = "-Xmx" + heapSize + heapSizeUnit;
            String xms = "-Xms" + heapSize + heapSizeUnit;
            System.out.println(xmx);
            String separator = System.getProperty("file.separator");
            String classpath = System.getProperty("java.class.path");
            String path = System.getProperty("java.home")
                    + separator + "bin" + separator + "java";

            if (System.getProperty("os.name").contains("Mac")) {
                pathSep = File.separator;
                applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
                if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                    applicationDirectory = new File(applicationDirectory).getParent();
                } else {
                    // Add the path to the class files
                    applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                    // Step one level up as we are only interested in the
                    // directory containing the class files
                    applicationDirectory = new File(applicationDirectory).getParent();
                }
                if (!applicationDirectory.endsWith(pathSep)) {
                    applicationDirectory += pathSep;
                }
                findFile(new File(applicationDirectory), "wbGAT.png");
                String icon = "wbGAT.png";
                if (retFile != null) {
                    icon = "-Xdock:icon=" + retFile;
                }
                ProcessBuilder processBuilder =
                        new ProcessBuilder(path, xmx, xms, "-cp",
                        classpath, icon, "-Xdock:name=Whitebox",
                        WhiteboxGui.class.getName());
                Process process = processBuilder.start();
            } else {
                ProcessBuilder processBuilder =
                        new ProcessBuilder(path, xmx, xms, "-cp",
                        classpath,
                        WhiteboxGui.class.getName());
                Process process = processBuilder.start();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private String retFile;
    private boolean flag = true;

    private void findFile(File dir, String fileName) {
        if (flag) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    findFile(files[i], fileName);
                } else if (files[i].getName().equals(fileName)) {
                    retFile = files[i].toString();
                    flag = false;
                    break;
                }
            }
        }
    }
//    private static void setLookAndFeel(String lafName) {
//        try {
//            
//            if (lafName.equals("systemLAF")) {
//                lafName = getSystemLookAndFeelName();
//            }
//
//            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//                if (lafName.equals(info.getName())) {
//                    UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//
//
//        } catch (Exception e) {
//            System.err.println(e.getMessage());
//        }
//    }
//
//    private static String getSystemLookAndFeelName() {
//        String className = UIManager.getSystemLookAndFeelClassName();
//        String name = null;
//        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//            if (className.equals(info.getClassName())) {
//                name = info.getName();
//                break;
//            }
//        }
//        return name;
//    }
//    
//    private static long getHeapSize() {
//        //Get the jvm heap size.
//        long heapSize = Runtime.getRuntime().maxMemory();
//        
//        return heapSize;
//    }
}
