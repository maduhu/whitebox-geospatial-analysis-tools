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
package whiteboxlauncher;

//import javax.swing.*;
import java.io.*;
import whiteboxgis.Main;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class WhiteboxLauncher {

    private final static int MIN_HEAP = 1024;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // Do we have enough memory already (some VMs and later Java 6 
            // revisions have bigger default heaps based on total machine memory)?
            float heapSizeMegs = (Runtime.getRuntime().maxMemory() / 1024) / 1024;

            if (heapSizeMegs > MIN_HEAP) {
                Main.main(args);

            } else {
                LaunchParameters lp = new LaunchParameters();
                String xmx = lp.readLaunchParameters();
                System.out.println(xmx);
                if (xmx != null) {
                    
                } else {
                    xmx = "-Xmx1g";
                }
                
                String sixtyFourBit = " -d64";
                
                //String pathToJar = WhiteboxGui.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                String classpath = System.getProperty("java.class.path");
                ProcessBuilder pb = new ProcessBuilder("java", xmx + sixtyFourBit, "-classpath", classpath, "whiteboxgis.Main");
                pb.start();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
             
    }
}

class LaunchParameters {
    private String parameterFile;
    private String iconFile;
    private String foundFile = null;
    
    public LaunchParameters() {
        try {
//            String pathSep = File.separator;
            String applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                applicationDirectory = new File(applicationDirectory).getParent();
            } else {
                // Add the path to the class files
                applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                // Step one level up as we are only interested in the
                // directory containing the class files
                applicationDirectory = new File(applicationDirectory).getParent();
            }
            System.out.println(applicationDirectory);
//            parameterFile = applicationDirectory + "launchParameters.txt";
            findFile(new File(applicationDirectory), "launchParameters.txt");
            parameterFile = foundFile;
            
            foundFile = null;
            findFile(new File(applicationDirectory), "wbGAT.png");
            iconFile = foundFile;
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
    
    protected String readLaunchParameters() {
        DataInputStream in = null;
        BufferedReader br = null;
        String ret = null;
        String units = "g";
        int maxHeapSize = 0;
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(parameterFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            if (parameterFile != null) {
                String line;
                String[] str;
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    if (line.toLowerCase().contains("max heap size:")) {
                        str = line.split(":");
                        line = str[str.length - 1].trim();
                        
                        if (line.toLowerCase().contains("g")) {
                            units = "g";
                        } else if (line.toLowerCase().contains("m")) {
                            units = "m";
                        }
                        
                        if (units.equals("g")) {
                            str = line.toLowerCase().split("g");
                        } else if (units.equals("m")) {
                            str = line.toLowerCase().split("m");
                        }
                                
                        maxHeapSize = Integer.parseInt(str[0]);
                        
                        ret = "-Xmx" + maxHeapSize + units;
                    }
                }
                //Close the input stream
                in.close();
                br.close();
                
//                ret += " -Xdock:name=\"Whitebox GAT\"";
//                if (iconFile != null) {
//                    ret += "-Xdock:icon=" + iconFile;
//                }

            }
        } catch (java.io.IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                if (in != null || br!= null) {
                    in.close();
                    br.close();
                }
            } catch (java.io.IOException ex) {
            }
            return ret;
        }
    }

    private void findFile(File dir, String fileName) {
        try {
            File[] files = dir.listFiles();
            for (int x = 0; x < files.length; x++) {
                if (files[x].isDirectory()) {
                    findFile(files[x], fileName);
                } else if (files[x].toString().contains(fileName)) {
                    foundFile = files[x].toString();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}