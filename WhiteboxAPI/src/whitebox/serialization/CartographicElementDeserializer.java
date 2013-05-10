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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import whitebox.cartographic.*;
import whitebox.interfaces.CartographicElement;
import whitebox.structures.BoundingBox;
import whitebox.interfaces.MapLayer;

/**
 *
 * @author johnlindsay
 */
public class CartographicElementDeserializer implements JsonDeserializer<CartographicElement> {

    private String workingDirectory;
    private String paletteDirectory = null;
    
    public CartographicElementDeserializer(String currentWorkingDirectory, String paletteDirectory) {
        this.workingDirectory = currentWorkingDirectory;
        this.paletteDirectory = paletteDirectory;
    }

    @Override
    public CartographicElement deserialize(JsonElement je, Type type,
            JsonDeserializationContext jdc)
            throws JsonParseException {

        if (workingDirectory == null || paletteDirectory == null) {
            throw new JsonParseException("The current working directory or palette directory must be set");
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(MapLayer.class, 
                new MapLayerDeserializer(workingDirectory, paletteDirectory));
        Gson gson = gsonBuilder.create();
        Type clrType = new TypeToken<Color>() {}.getType();
        Color clr;
        Type fontType = new TypeToken<Font>() {}.getType();
        Font font;
        Type bbType = new TypeToken<BoundingBox>() {}.getType();
        BoundingBox bb;

        JsonObject jo = je.getAsJsonObject();
        String elementType = jo.getAsJsonPrimitive("cartographicElementType").getAsString();
        String name = jo.getAsJsonPrimitive("name").getAsString();

        switch (elementType) {
            case "MAPAREA":
                MapArea ma = new MapArea(name);
                ma.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                ma.setHeight(jo.getAsJsonPrimitive("height").getAsInt());
                ma.setWidth(jo.getAsJsonPrimitive("width").getAsInt());
                ma.setLineWidth(jo.getAsJsonPrimitive("lineWidth").getAsFloat());
                ma.setReferenceMarksSize(jo.getAsJsonPrimitive("referenceMarksSize").getAsInt());
                ma.setRotation(jo.getAsJsonPrimitive("rotation").getAsDouble());
                ma.setBackgroundVisible(jo.getAsJsonPrimitive("isBackgroundVisible").getAsBoolean());
                ma.setBorderVisible(jo.getAsJsonPrimitive("isBorderVisible").getAsBoolean());
                ma.setNeatlineVisible(jo.getAsJsonPrimitive("isNeatlineVisible").getAsBoolean());
                ma.setReferenceMarksVisible(jo.getAsJsonPrimitive("isReferenceMarksVisible").getAsBoolean());
                ma.setSizeMaximizedToScreenSize(jo.getAsJsonPrimitive("isSizeMaximizedToScreenSize").getAsBoolean());
                ma.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                ma.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                ma.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                ma.setName(jo.getAsJsonPrimitive("name").getAsString());
                clr = gson.fromJson(jo.get("backgroundColour"), clrType);
                ma.setBackgroundColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                ma.setBorderColour(clr);
                clr = gson.fromJson(jo.get("fontColour"), clrType);
                ma.setFontColour(clr);
                font = gson.fromJson(jo.get("labelFont"), fontType);
                ma.setLabelFont(font);
                bb = gson.fromJson(jo.get("currentExtent"), bbType);
                ma.setCurrentExtent(bb);
                bb = gson.fromJson(jo.get("currentMapExtent"), bbType);
                ma.setCurrentMapExtent(bb);
                
                Type listOfLayerObject = new TypeToken<List<MapLayer>>() {}.getType();
                ArrayList<MapLayer> mlList = gson.fromJson(jo.get("layersList"), listOfLayerObject);
                for (MapLayer ml : mlList) {
                    ma.addLayer(ml);
                }
                ma.setActiveLayer(jo.getAsJsonPrimitive("activeLayerOverlayNumber").getAsInt());
                return ma;

            case "MAPTITLE":
                MapTitle mt = new MapTitle(name);
                mt.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                mt.setLabel(jo.getAsJsonPrimitive("label").getAsString());
                mt.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                mt.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                mt.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                mt.setHeight(jo.getAsJsonPrimitive("height").getAsInt());
                mt.setMargin(jo.getAsJsonPrimitive("margin").getAsInt());
                mt.setBackgroundVisible(jo.getAsJsonPrimitive("isBackgroundVisible").getAsBoolean());
                mt.setBorderVisible(jo.getAsJsonPrimitive("isBorderVisible").getAsBoolean());
                mt.setOutlineVisible(jo.getAsJsonPrimitive("isOutlineVisible").getAsBoolean());
                clr = gson.fromJson(jo.get("backColour"), clrType);
                mt.setBackColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                mt.setBorderColour(clr);
                clr = gson.fromJson(jo.get("fontColour"), clrType);
                mt.setFontColour(clr);
                clr = gson.fromJson(jo.get("outlineColour"), clrType);
                mt.setOutlineColour(clr);
                font = gson.fromJson(jo.get("labelFont"), fontType);
                mt.setLabelFont(font);
                return mt;

           case "MAPTEXTAREA":
                MapTextArea mta = new MapTextArea(name);
                mta.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                mta.setLabel(jo.getAsJsonPrimitive("label").getAsString());
                mta.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                mta.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                mta.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                mta.setHeight(jo.getAsJsonPrimitive("height").getAsInt());
                mta.setMargin(jo.getAsJsonPrimitive("margin").getAsInt());
                mta.setBackgroundVisible(jo.getAsJsonPrimitive("isBackgroundVisible").getAsBoolean());
                mta.setBorderVisible(jo.getAsJsonPrimitive("isBorderVisible").getAsBoolean());
                clr = gson.fromJson(jo.get("backColour"), clrType);
                mta.setBackColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                mta.setBorderColour(clr);
                clr = gson.fromJson(jo.get("fontColour"), clrType);
                mta.setFontColour(clr);
                font = gson.fromJson(jo.get("labelFont"), fontType);
                mta.setLabelFont(font);
                return mta;

            case "NORTHARROW":
                NorthArrow na = new NorthArrow(name);
                na.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                na.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                na.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                na.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                na.setMargin(jo.getAsJsonPrimitive("margin").getAsInt());
                na.setMarkerSize(jo.getAsJsonPrimitive("markerSize").getAsInt());
                na.setLineWidth(jo.getAsJsonPrimitive("lineWidth").getAsFloat());
                na.setBackgroundVisible(jo.getAsJsonPrimitive("isBackgroundVisible").getAsBoolean());
                na.setBorderVisible(jo.getAsJsonPrimitive("isBorderVisible").getAsBoolean());
                clr = gson.fromJson(jo.get("backColour"), clrType);
                na.setBackColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                na.setBorderColour(clr);
                clr = gson.fromJson(jo.get("outlineColour"), clrType);
                na.setOutlineColour(clr);
                return na;

            case "NEATLINE":
                Neatline nl = new Neatline(name);
                nl.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                nl.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                nl.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                nl.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                nl.setDoubleLineGap(jo.getAsJsonPrimitive("doubleLineGap").getAsInt());
                nl.setHeight(jo.getAsJsonPrimitive("height").getAsInt());
                nl.setWidth(jo.getAsJsonPrimitive("width").getAsInt());
                nl.setInnerLineWidth(jo.getAsJsonPrimitive("innerLineWidth").getAsFloat());
                nl.setOuterLineThickness(jo.getAsJsonPrimitive("outerLineWidth").getAsFloat());
                nl.setBackgroundVisible(jo.getAsJsonPrimitive("isBackgroundVisible").getAsBoolean());
                nl.setBorderVisible(jo.getAsJsonPrimitive("isBorderVisible").getAsBoolean());
                nl.setDoubleLine(jo.getAsJsonPrimitive("isDoubleLine").getAsBoolean());
                clr = gson.fromJson(jo.get("backgroundColour"), clrType);
                nl.setBackgroundColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                nl.setBorderColour(clr);
                
                return nl;
                
            case "MAPSCALE":
                MapScale ms = new MapScale(name);
                ms.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                ms.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                ms.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                ms.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                clr = gson.fromJson(jo.get("backColour"), clrType);
                ms.setBackColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                ms.setBorderColour(clr);
                clr = gson.fromJson(jo.get("fontColour"), clrType);
                ms.setFontColour(clr);
                clr = gson.fromJson(jo.get("outlineColour"), clrType);
                ms.setOutlineColour(clr);
                ms.setBackgroundVisible(jo.getAsJsonPrimitive("backgroundVisible").getAsBoolean());
                ms.setBorderVisible(jo.getAsJsonPrimitive("borderVisible").getAsBoolean());
                ms.setOutlineVisible(jo.getAsJsonPrimitive("outlineVisible").getAsBoolean());
                ms.setConversionToMetres(jo.getAsJsonPrimitive("conversionToMetres").getAsDouble());
                ms.setHeight(jo.getAsJsonPrimitive("height").getAsInt());
                ms.setWidth(jo.getAsJsonPrimitive("width").getAsInt());
                ms.setLineWidth(jo.getAsJsonPrimitive("lineWidth").getAsFloat());
                ms.setLowerLabel(jo.getAsJsonPrimitive("lowerLabel").getAsString());
                ms.setMargin(jo.getAsJsonPrimitive("margin").getAsInt());
                ms.setNumberDivisions(jo.getAsJsonPrimitive("numberDivisions").getAsInt());
                ms.setRepresentativeFractionVisible(jo.getAsJsonPrimitive("representativeFractionVisible").getAsBoolean());
                ms.setUnits(jo.getAsJsonPrimitive("units").getAsString());
                ms.setMapAreaElementNumber(jo.getAsJsonPrimitive("mapAreaElementNumber").getAsInt());
                return ms;
                
            case "LEGEND":
                Legend l = new Legend(name);
                l.setVisible(jo.getAsJsonPrimitive("isVisible").getAsBoolean());
                l.setLabel(jo.getAsJsonPrimitive("label").getAsString());
                font = gson.fromJson(jo.get("labelFont"), fontType);
                l.setLabelFont(font);
                l.setMargin(jo.getAsJsonPrimitive("margin").getAsInt());
                l.setElementNumber(jo.getAsJsonPrimitive("elementNumber").getAsInt());
                clr = gson.fromJson(jo.get("backgroundColour"), clrType);
                l.setBackgroundColour(clr);
                clr = gson.fromJson(jo.get("borderColour"), clrType);
                l.setBorderColour(clr);
                clr = gson.fromJson(jo.get("fontColour"), clrType);
                l.setFontColour(clr);
                l.setBackgroundVisible(jo.getAsJsonPrimitive("backgroundVisible").getAsBoolean());
                l.setBorderVisible(jo.getAsJsonPrimitive("borderVisible").getAsBoolean());
                l.setLineWidth(jo.getAsJsonPrimitive("lineWidth").getAsFloat());
                l.setBorderWidth(jo.getAsJsonPrimitive("borderWidth").getAsFloat());
                l.setHeight(jo.getAsJsonPrimitive("height").getAsInt());
                l.setWidth(jo.getAsJsonPrimitive("width").getAsInt());
                l.setUpperLeftX(jo.getAsJsonPrimitive("upperLeftX").getAsInt());
                l.setUpperLeftY(jo.getAsJsonPrimitive("upperLeftY").getAsInt());
                
                return l;
        }

        return null;
    }
}
