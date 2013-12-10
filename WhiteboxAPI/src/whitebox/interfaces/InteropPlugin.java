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

package whitebox.interfaces;

/**
 * This interface is used by the import tools so that the supported file formats
 * can be added to the current map using the AddLayer method.
 * @author johnlindsay
 */
public interface InteropPlugin {
    
    public enum InteropPluginType {
       importPlugin, exportPlugin;   
   }
    
    public String[] getExtensions();
    
    public String getFileTypeName();
    
    public boolean isRasterFormat();
    
    public InteropPluginType getInteropPluginType();
}
