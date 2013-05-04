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
package whitebox.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import whitebox.interfaces.MapLayer;
import whitebox.geospatialfiles.RasterLayerInfo;
import whitebox.geospatialfiles.VectorLayerInfo;

/**
 *
 * @author johnlindsay
 */
public class MapLayerSerializer implements JsonSerializer<MapLayer> { 
    @Override
    public JsonElement serialize(MapLayer t, Type type, JsonSerializationContext jsc) {
        Gson gson = new Gson(); 
        
        JsonObject jo = new JsonObject();
        
        jo.addProperty("layerType", t.getLayerType().toString());
        jo.addProperty("layerTitle", t.getLayerTitle());
        jo.addProperty("overlayNumber", t.getOverlayNumber());
        jo.addProperty("isVisible", t.isVisible());
        
        if (t.getLayerType() == MapLayer.MapLayerType.RASTER) {
            RasterLayerInfo rli = (RasterLayerInfo)t;
            jo.addProperty("headerFile", rli.getHeaderFile());
            jo.addProperty("displayMinVal", rli.getDisplayMinVal());
            jo.addProperty("displayMaxVal", rli.getDisplayMaxVal());
            jo.addProperty("nonlinearity", rli.getNonlinearity());
            jo.addProperty("paletteFile", rli.getPaletteFile());
            jo.addProperty("alpha", rli.getAlpha());
            jo.addProperty("isPaletteReversed", rli.isPaletteReversed());
        } else if (t.getLayerType() == MapLayer.MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo)t;
            jo.addProperty("fileName", vli.getFileName());
            jo.addProperty("alpha", vli.getAlpha());
            jo.addProperty("fillAttribute", vli.getFillAttribute());
            jo.addProperty("lineAttribute", vli.getLineAttribute());
            jo.addProperty("lineThickness", vli.getLineThickness());
            jo.addProperty("markerSize", vli.getMarkerSize());
            jo.addProperty("maximumValue", vli.getMaximumValue());
            jo.addProperty("minimumValue", vli.getMinimumValue());
            jo.addProperty("nonlinearity", vli.getNonlinearity());
            jo.addProperty("paletteFile", vli.getPaletteFile());
            jo.addProperty("xyUnits", vli.getXYUnits());
            jo.addProperty("isDashed", vli.isDashed());
            jo.addProperty("isFilled", vli.isFilled());
            jo.addProperty("isFilledWithOneColour", vli.isFilledWithOneColour());
            jo.addProperty("isOutlined", vli.isOutlined());
            jo.addProperty("isOutlinedWithOneColour", vli.isOutlinedWithOneColour());
            jo.addProperty("isPaletteScaled", vli.isPaletteScaled());
            jo.add("fillColour", gson.toJsonTree(vli.getFillColour()));
            jo.add("lineColour", gson.toJsonTree(vli.getLineColour()));
            jo.addProperty("markerStyle", vli.getMarkerStyle().toString());
        }

        return jo;
    }
    
}
