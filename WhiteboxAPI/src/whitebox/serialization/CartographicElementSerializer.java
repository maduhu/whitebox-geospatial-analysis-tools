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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import whitebox.cartographic.*;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.CartographicElement.CartographicElementType.*;
import static whitebox.interfaces.CartographicElement.CartographicElementType.MAPTITLE;
import whitebox.interfaces.MapLayer;

/**
 *
 * @author johnlindsay
 */
public class CartographicElementSerializer implements JsonSerializer<CartographicElement> {

    @Override
    public JsonElement serialize(CartographicElement t, Type type, JsonSerializationContext jsc) {
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(MapLayer.class, new MapLayerSerializer());
            Gson gson = gsonBuilder.create();
            JsonObject jo = new JsonObject();

            jo.addProperty("cartographicElementType", t.getCartographicElementType().toString());
            jo.addProperty("elementNumber", t.getElementNumber());
            jo.addProperty("lowerRightX", t.getLowerRightX());
            jo.addProperty("lowerRightY", t.getLowerRightY());
            jo.addProperty("upperLeftX", t.getUpperLeftX());
            jo.addProperty("upperLeftY", t.getUpperLeftY());
            jo.addProperty("name", t.getName());
            jo.addProperty("isSelected", t.isSelected());
            jo.addProperty("isVisible", t.isVisible());

            switch (t.getCartographicElementType()) {
                case MAPAREA:
                    MapArea ma = (MapArea) t;
                    jo.addProperty("height", ma.getHeight());
                    jo.addProperty("width", ma.getWidth());
                    jo.addProperty("lineWidth", ma.getLineWidth());
                    jo.addProperty("numLayers", ma.getNumLayers());
                    jo.addProperty("referenceMarksSize", ma.getReferenceMarksSize());
                    jo.addProperty("rotation", ma.getRotation());
                    jo.addProperty("isBackgroundVisible", ma.isBackgroundVisible());
                    jo.addProperty("isBorderVisible", ma.isBorderVisible());
                    jo.addProperty("isNeatlineVisible", ma.isNeatlineVisible());
                    jo.addProperty("isReferenceMarksVisible", ma.isReferenceMarksVisible());
                    jo.addProperty("isSizeMaximizedToScreenSize", ma.isSizeMaximizedToScreenSize());
                    jo.addProperty("activeLayerOverlayNumber", ma.getActiveLayerOverlayNumber());
                    jo.add("currentExtent", gson.toJsonTree(ma.getCurrentExtent()));
                    jo.add("currentMapExtent", gson.toJsonTree(ma.getCurrentMapExtent()));
                    jo.add("backgroundColour", gson.toJsonTree(ma.getBackgroundColour()));
                    jo.add("borderColour", gson.toJsonTree(ma.getBorderColour()));
                    jo.add("fontColour", gson.toJsonTree(ma.getFontColour()));
                    jo.add("labelFont", gson.toJsonTree(ma.getLabelFont()));
                    
                    Type listOfLayerObject = new TypeToken<List<MapLayer>>() {}.getType();
                    jo.add("layersList", gson.toJsonTree(ma.getLayersList(), listOfLayerObject));
                    break;
                case MAPTITLE:
                    MapTitle mt = (MapTitle) t;
                    jo.addProperty("label", mt.getLabel());
                    jo.addProperty("fontHeight", mt.getFontHeight());
                    jo.addProperty("height", mt.getHeight());
                    jo.addProperty("width", mt.getWidth());
                    jo.addProperty("lineWidth", mt.getLineWidth());
                    jo.addProperty("margin", mt.getMargin());
                    jo.addProperty("isBackgroundVisible", mt.isBackgroundVisible());
                    jo.addProperty("isBorderVisible", mt.isBorderVisible());
                    jo.addProperty("isOutlineVisible", mt.isOutlineVisible());
                    jo.add("backColour", gson.toJsonTree(mt.getBackColour()));
                    jo.add("borderColour", gson.toJsonTree(mt.getBorderColour()));
                    jo.add("fontColour", gson.toJsonTree(mt.getFontColour()));
                    jo.add("outlineColour", gson.toJsonTree(mt.getOutlineColour()));
                    jo.add("labelFont", gson.toJsonTree(mt.getLabelFont()));
                    
                    break;
                case NEATLINE:
                    NeatLine nl = (NeatLine) t;
                    jo.addProperty("doubleLineGap", nl.getDoubleLineGap());
                    jo.addProperty("height", nl.getHeight());
                    jo.addProperty("width", nl.getWidth());
                    jo.addProperty("innerLineWidth", nl.getInnerLineWidth());
                    jo.addProperty("outerLineWidth", nl.getOuterLineWidth());
                    jo.addProperty("isBackgroundVisible", nl.isBackgroundVisible());
                    jo.addProperty("isBorderVisible", nl.isBorderVisible());
                    jo.addProperty("isDoubleLine", nl.isDoubleLine());
                    jo.add("backgroundColour", gson.toJsonTree(nl.getBackgroundColour()));
                    jo.add("borderColour", gson.toJsonTree(nl.getBorderColour()));
                    break;
                case NORTHARROW:
                    NorthArrow na = (NorthArrow) t;
                    jo.addProperty("height", na.getHeight());
                    jo.addProperty("width", na.getWidth());
                    jo.addProperty("lineWidth", na.getLineWidth());
                    jo.addProperty("margin", na.getMargin());
                    jo.addProperty("markerSize", na.getMarkerSize());
                    jo.addProperty("isBackgroundVisible", na.isBackgroundVisible());
                    jo.addProperty("isBorderVisible", na.isBorderVisible());
                    jo.add("backColour", gson.toJsonTree(na.getBackColour()));
                    jo.add("borderColour", gson.toJsonTree(na.getBorderColour()));
                    jo.add("outlineColour", gson.toJsonTree(na.getOutlineColour()));
                    break;
                case MAPSCALE:
                    MapScale ms = (MapScale) t;
                    jo.addProperty("barLength", ms.getBarLength());
                    jo.addProperty("conversionToMetres", ms.getConversionToMetres());
                    jo.addProperty("height", ms.getHeight());
                    jo.addProperty("width", ms.getWidth());
                    jo.addProperty("lineWidth", ms.getLineWidth());
                    jo.addProperty("lowerLabel", ms.getLowerLabel());
                    jo.addProperty("margin", ms.getMargin());
                    jo.addProperty("numberDivisions", ms.getNumberDivisions());
                    jo.addProperty("representativeFraction", ms.getRepresentativeFraction());
                    jo.addProperty("scale", ms.getScale());
                    jo.addProperty("units", ms.getUnits());
                    jo.addProperty("backgroundVisible", ms.isBackgroundVisible());
                    jo.addProperty("borderVisible", ms.isBorderVisible());
                    jo.addProperty("outlineVisible", ms.isOutlineVisible());
                    jo.addProperty("representativeFractionVisible", ms.isRepresentativeFractionVisible());
                    jo.add("backColour", gson.toJsonTree(ms.getBackColour()));
                    jo.add("borderColour", gson.toJsonTree(ms.getBorderColour()));
                    jo.add("fontColour", gson.toJsonTree(ms.getFontColour()));
                    jo.add("outlineColour", gson.toJsonTree(ms.getOutlineColour()));
                    jo.addProperty("mapAreaElementNumber", ms.getMapAreaElementNumber());
                    break;
                case LEGEND:
                    Legend l = (Legend) t;
                    jo.addProperty("label", l.getLabel());
                    jo.addProperty("borderWidth", l.getBorderWidth());
                    jo.addProperty("fontHeight", l.getFontHeight());
                    jo.addProperty("height", l.getHeight());
                    jo.addProperty("width", l.getWidth());
                    jo.addProperty("lineWidth", l.getLineWidth());
                    jo.addProperty("margin", l.getMargin());
                    jo.addProperty("numberOfLegendEntries", l.getNumberOfLegendEntries());
                    jo.addProperty("backgroundVisible", l.isBackgroundVisible());
                    jo.addProperty("borderVisible", l.isBorderVisible());
                    jo.add("backgroundColour", gson.toJsonTree(l.getBackgroundColour()));
                    jo.add("borderColour", gson.toJsonTree(l.getBorderColour()));
                    jo.add("fontColour", gson.toJsonTree(l.getFontColour()));
                    jo.add("labelFont", gson.toJsonTree(l.getLabelFont()));
                    
                    break;
            }

            return jo;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
