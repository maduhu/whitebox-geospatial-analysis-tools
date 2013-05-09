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
import java.lang.reflect.Type;
import whitebox.cartographic.PointMarkers;
import whitebox.interfaces.MapLayer;
import whitebox.geospatialfiles.RasterLayerInfo;
import whitebox.geospatialfiles.VectorLayerInfo;
import java.io.File;

/**
 *
 * @author johnlindsay
 */
public class MapLayerDeserializer implements JsonDeserializer<MapLayer> {
    
    private String workingDirectory;
    private String paletteDirectory;
    
    public MapLayerDeserializer(String currentWorkingDirectory, String paletteDirectory) {
        this.workingDirectory = currentWorkingDirectory;
        this.paletteDirectory = paletteDirectory;
    }
    
    @Override
    public MapLayer deserialize(JsonElement je, Type type, 
                                JsonDeserializationContext jdc)
                           throws JsonParseException {
        
        if (workingDirectory == null || paletteDirectory == null) {
            throw new JsonParseException("The current working directory or palette directory must be set.");
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(MapLayer.class, 
                new MapLayerDeserializer(workingDirectory, paletteDirectory));
        Gson gson = gsonBuilder.create();
        Type clrType = new TypeToken<Color>() {}.getType();
        Color clr;
        int alpha;
        double nonlinearity;
        String paletteFile;
        
        JsonObject jo = je.getAsJsonObject();
        String layerType = jo.getAsJsonPrimitive("layerType").getAsString();
        String layerTitle = jo.getAsJsonPrimitive("layerTitle").getAsString();
        int overlayNumber = jo.getAsJsonPrimitive("overlayNumber").getAsInt();
        boolean isVisible = jo.getAsJsonPrimitive("isVisible").getAsBoolean();
        switch (layerType) {
            case "RASTER":
                // find the header file
                String headerFile = jo.getAsJsonPrimitive("headerFile").getAsString();
                
                // see whether it exists, and if it doesn't, see whether a file of the same
                // name exists in the working directory or any of its subdirectories.
                if (!new File(headerFile).exists()) {
                    flag = true;
                    findFile(new File(workingDirectory), new File(headerFile).getName());
                    if (!retFile.equals("")) {
                        headerFile = retFile;
                    } else {
                        throw new JsonParseException("Could not locate data file referred to in map file.");
                    }
                }
                
                double displayMinVal = jo.getAsJsonPrimitive("displayMinVal").getAsDouble();
                double displayMaxVal = jo.getAsJsonPrimitive("displayMaxVal").getAsDouble();
                nonlinearity = jo.getAsJsonPrimitive("nonlinearity").getAsDouble();
                
                // find the palette file
                paletteFile = jo.getAsJsonPrimitive("paletteFile").getAsString();
                
                // see whether it exists, and if it doesn't, see whether a file of the same
                // name exists in the working directory or any of its subdirectories.
                if (!new File(paletteFile).exists()) {
                    flag = true;
                    findFile(new File(paletteDirectory), new File(paletteFile).getName());
                    if (!retFile.equals("")) {
                        paletteFile = retFile;
                    } else {
                        // could not locate the palette file so go with a default palette.
                        paletteFile = paletteDirectory + "spectrum.pal";
                    }
                }
                alpha = jo.getAsJsonPrimitive("alpha").getAsInt();
                boolean isPaletteReversed = jo.getAsJsonPrimitive("isPaletteReversed").getAsBoolean();
                
                RasterLayerInfo rli = new RasterLayerInfo(headerFile, paletteFile, alpha, overlayNumber);
                rli.setDisplayMaxVal(displayMaxVal);
                rli.setDisplayMinVal(displayMinVal);
                rli.setNonlinearity(nonlinearity);
                rli.setLayerTitle(layerTitle);
                rli.setPaletteReversed(isPaletteReversed);
                rli.setVisible(isVisible);
                
                return rli;
            case "VECTOR":
                // find the header file
                String fileName = jo.getAsJsonPrimitive("fileName").getAsString();
                
                // see whether it exists, and if it doesn't, see whether a file of the same
                // name exists in the working directory or any of its subdirectories.
                if (!new File(fileName).exists()) {
                    flag = true;
                    findFile(new File(workingDirectory), new File(fileName).getName());
                    if (!retFile.equals("")) {
                        fileName = retFile;
                    } else {
                        throw new JsonParseException("Could not locate data file referred to in map file.");
                    }
                }
                
                alpha = jo.getAsJsonPrimitive("alpha").getAsInt();
                String fillAttribute = jo.getAsJsonPrimitive("fillAttribute").getAsString();
                String lineAttribute = jo.getAsJsonPrimitive("lineAttribute").getAsString();
                float lineThickness = jo.getAsJsonPrimitive("lineThickness").getAsFloat();
                float markerSize = jo.getAsJsonPrimitive("markerSize").getAsFloat();
//                String markerStyle = jo.getAsJsonPrimitive("markerStyle").getAsString();
                nonlinearity = jo.getAsJsonPrimitive("nonlinearity").getAsDouble();
                String xyUnits = jo.getAsJsonPrimitive("xyUnits").getAsString();
                boolean isDashed = jo.getAsJsonPrimitive("isDashed").getAsBoolean();
                boolean isFilled = jo.getAsJsonPrimitive("isFilled").getAsBoolean();
                boolean isFilledWithOneColour = jo.getAsJsonPrimitive("isFilledWithOneColour").getAsBoolean();
                boolean isOutlined = jo.getAsJsonPrimitive("isOutlined").getAsBoolean();
                boolean isOutlinedWithOneColour = jo.getAsJsonPrimitive("isOutlinedWithOneColour").getAsBoolean();
                boolean isPaletteScaled = jo.getAsJsonPrimitive("isPaletteScaled").getAsBoolean();
                // find the palette file
                paletteFile = jo.getAsJsonPrimitive("paletteFile").getAsString();
                
                // see whether it exists, and if it doesn't, see whether a file of the same
                // name exists in the working directory or any of its subdirectories.
                if (!new File(paletteFile).exists()) {
                    flag = true;
                    findFile(new File(paletteDirectory), new File(paletteFile).getName());
                    if (!retFile.equals("")) {
                        paletteFile = retFile;
                    } else {
                        // could not locate the palette file so go with a default palette.
                        paletteFile = paletteDirectory + "spectrum.pal";
                    }
                }
                
                VectorLayerInfo vli = new VectorLayerInfo(fileName, paletteDirectory, alpha, overlayNumber);
                vli.setFillAttribute(fillAttribute);
                vli.setLineAttribute(lineAttribute);
                vli.setLineThickness(lineThickness);
                vli.setLayerTitle(layerTitle);
                vli.setMarkerSize(markerSize);
                vli.setPaletteFile(paletteFile);
                //marker style
                vli.setMarkerStyle(PointMarkers.findMarkerStyleFromString(jo.getAsJsonPrimitive("markerStyle").getAsString()));
                vli.setXYUnits(xyUnits);
                vli.setDashed(isDashed);
                vli.setFilled(isFilled);
                vli.setFilledWithOneColour(isFilledWithOneColour);
                vli.setOutlined(isOutlined);
                vli.setOutlinedWithOneColour(isOutlinedWithOneColour);
                vli.setPaletteScaled(isPaletteScaled);
                
                clr = gson.fromJson(jo.get("fillColour"), clrType);
                vli.setFillColour(clr);
                clr = gson.fromJson(jo.get("lineColour"), clrType);
                vli.setLineColour(clr);
                
                
                return vli;
            default:
                return null;
        }
        
    }
    
    private static String retFile = "";
    private boolean flag = true;
    private void findFile(File dir, String fileName) {
        if (flag) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    findFile(files[i], fileName);
                } else if (files[i].getName().equals(fileName)) {
                    retFile = files[i].toString();
                    flag = false;
                    break;
                }
            }
        }
    }
}
