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

package whitebox.geospatialfiles;
 

/**
 * NOTE: THIS CLASS IS NO LONGER RECOMMENDED. THE WHITEBOXRASTERBASE CLASS HAS BEEN MADE 
 * NON-ABSTRACT. USE IT INSTEAD. THIS CLASS IS LEGACY ONLY.
 * 
 * The WhiteboxRasterInfo is a lightweight alternative to the WhiteboxRaster class and can be
 * used to manipulate Whitebox GAT raster header files (.dep). If you need to read or write data
 * to the grid cells contained in the raster, you should use the WhiteboxRaster class instead.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class WhiteboxRasterInfo extends WhiteboxRasterBase {


    // ************************
    // Fields
    // ************************
    //private boolean isDirty = false;

    // ************************
    // Constructors
    // ************************


    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param HeaderFile The name of the WhiteboxRaster header file.
     */
    public WhiteboxRasterInfo(String HeaderFile)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess("rw");
        readHeaderFile();
        
    }
    
//    /**
//     * Used to perform closing functionality when a whiteboxRaster is no longer needed.
//     */
//    @Override
//    public void close() {
//        if (saveChanges) {
//            if (isDirty) {
//                writeHeaderFile();
//            }
//        }
//    }

}
