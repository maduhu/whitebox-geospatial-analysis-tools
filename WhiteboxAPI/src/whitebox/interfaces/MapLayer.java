/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import whitebox.structures.BoundingBox;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public interface MapLayer {
    /**
     * Gets the layer title.
     * @return Layer title.
     */
    public String getLayerTitle();
    
    /**
     * Sets the layer title.
     * @param title 
     */
    public void setLayerTitle(String title);
    
    /**
     * Gets the map layer type.
     * @return MapLayerType (RASTER, VECTOR, or MULTISPECTRAL)
     */
    public MapLayerType getLayerType();
    
    /**
     * Gets the full extent of the layer.
     * @return BoundingBox of full extent.
     */
    public BoundingBox getFullExtent();
    
    /**
     * Used to get the current extent of the map layer.
     * @return BoundingBox of the current extent.
     */
    public BoundingBox getCurrentExtent();
    
    /**
     * Used to set the current extent of the map layer.
     * @param db, a BoundingBox.
     */
    public void setCurrentExtent(BoundingBox db);
    
    /**
     * Used to determine whether or not the layer is currently visible.
     * @return true or false, depending on current visibility.
     */
    public boolean isVisible();
    
    /**
     * Used to set the visibility of the layer.
     * @param value 
     */
    public void setVisible(boolean value);
    
    /**
     * Used to get the overlay number of this layer on the current map.
     * @return Overlay number.
     */
    public int getOverlayNumber();
    
    /**
     * Used to set the overlay number of this layer on the current map.
     * @param value 
     */
    public void setOverlayNumber(int value);

    /**
     * An enum of map layer types.
     */
    public enum MapLayerType {
        RASTER, VECTOR, MULTISPECTRAL
    }
}