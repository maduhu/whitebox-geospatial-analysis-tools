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
package whiteboxgis;

import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.*;
/**
 * This class is used to digitize new shapefiles.
 * @author johnlindsay
 */
public class ShapeFileDigitizer {
    
    private ShapeFile shapeFile;
    private String fileName;
    private ShapeType shapeType;
    // constructors
    public ShapeFileDigitizer() {
        // no-arg constructor
    }
    
    /**
     * Constructor method
     * @param fileName  File name given to the new shapefile. If this file already
     *                  exists it will be deleted.
     */
    public ShapeFileDigitizer(String fileName) {
        this.fileName = fileName;
    }

    // properties
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public ShapeFile getShapefile() {
        return shapeFile;
    }
    
    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }
    
    // methods

}
