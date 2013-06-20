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
package whitebox.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 *
 * @author johnlindsay
 */
public abstract class LogHandler {

    private static final Logger logger = Logger.getLogger(LogHandler.class.getPackage().getName());
    private static String logDirectory = "";
    private void setLogger() {
        try {
            String pathSep = File.pathSeparator;
            String applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            //getClass().getProtectionDomain().
            if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                applicationDirectory = new File(applicationDirectory).getParent();
            } else {
                // Add the path to the class files
                applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                // Step one level up as we are only interested in the
                // directory containing the class files
                applicationDirectory = new File(applicationDirectory).getParent();
            }
            
            
            applicationDirectory = new File(applicationDirectory).getParent();
            
            String resourcesDirectory = applicationDirectory + pathSep + "resources" + pathSep;
            logDirectory = resourcesDirectory + "logs" + pathSep;
            
            // set up the logger
            int limit = 1000000; // 1 Mb
            int numLogFiles = 3;
            FileHandler fh = new FileHandler(logDirectory + "WhiteboxLog%g_%u.xml", limit, numLogFiles, true);
            fh.setFormatter(new XMLFormatter());
            logger.addHandler(fh);
            
        } catch (IOException | SecurityException e) {
            System.err.println(e.toString());
        }
    }
}
