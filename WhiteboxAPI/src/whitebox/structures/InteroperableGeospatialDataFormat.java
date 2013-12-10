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

package whitebox.structures;

import whitebox.interfaces.InteropPlugin.InteropPluginType;

/**
 *
 * @author johnlindsay
 */
public class InteroperableGeospatialDataFormat {
    String interopClass;
    String[] supportedExtensions;
    String name;
    boolean isRasterFormat;
    InteropPluginType interopPluginType;
    
    public InteroperableGeospatialDataFormat(String name, String[] supportedExtensions, 
            String interopClass, boolean isRasterFormat, InteropPluginType pluginType) {
        this.name = name;
        this.supportedExtensions = supportedExtensions;
        this.interopClass = interopClass;
        this.isRasterFormat = isRasterFormat;
        this.interopPluginType = pluginType;
    }

    public String getInteropClass() {
        return interopClass;
    }

    public void setInteropClass(String interopClass) {
        this.interopClass = interopClass;
    }

    public String[] getSupportedExtensions() {
        return supportedExtensions;
    }

    public void setSupportedExtensions(String[] supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIsRasterFormat() {
        return isRasterFormat;
    }

    public void setIsRasterFormat(boolean isRasterFormat) {
        this.isRasterFormat = isRasterFormat;
    }

    public InteropPluginType getInteropPluginType() {
        return interopPluginType;
    }

    public void setInteropPluginType(InteropPluginType interopPluginType) {
        this.interopPluginType = interopPluginType;
    }
}
