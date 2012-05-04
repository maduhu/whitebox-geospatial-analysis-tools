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
package whitebox.utilities;

import java.io.File;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public final class PaletteComparer {
    static String applicationDirectory;
    static String paletteDirectory;
    
    public PaletteComparer() {
        try {
            String pathSep = File.separator;

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

            paletteDirectory = pathSep + "resources" + pathSep + "palettes" + pathSep;
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static String paletteWithMoreEntries(String palette1, String palette2) {
        try {
            String str = null;
            if (palette1 != null && palette2 != null) {
                int numPaletteEntries1 = 0;
                int numPaletteEntries2 = 0;
                // see if the file exists, if not, set it to the default palette.
                String paletteName1 = paletteDirectory + palette1;
                String paletteName2 = paletteDirectory + palette2;
                File file1 = new File(paletteName1);
                File file2 = new File(paletteName2);
                
                numPaletteEntries1 = (int) (file1.length() / 4);
                numPaletteEntries2 = (int) (file2.length() / 4);
                
                if (numPaletteEntries1 < numPaletteEntries2) {
                    str = palette1;
                } else {
                    str = palette2;
                }
            }
            return str;
        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            return null;
        }
    }
}
