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
import java.awt.Font;
import java.awt.print.PageFormat;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import whitebox.cartographic.*;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapInfoDeserializer implements JsonDeserializer<MapInfo> {

    private String workingDirectory = null;
    private String paletteDirectory = null;

    public MapInfoDeserializer(String currentWorkingDirectory, String paletteDirectory) {
        this.workingDirectory = currentWorkingDirectory;
        this.paletteDirectory = paletteDirectory;
    }

    @Override
    public MapInfo deserialize(JsonElement je, Type type,
            JsonDeserializationContext jdc)
            throws JsonParseException {
        try {
            if (workingDirectory == null || paletteDirectory == null) {
                throw new JsonParseException("The current working directory or palette directory must be set.");
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(PageFormat.class, new PageFormatDeserializer());
            gsonBuilder.registerTypeAdapter(CartographicElement.class,
                    new CartographicElementDeserializer(workingDirectory, paletteDirectory));
            Gson gson = gsonBuilder.create();
            
            Type fontType = new TypeToken<Font>() {}.getType();
            Font font;

            JsonObject jo = je.getAsJsonObject();
            MapInfo mi = new MapInfo();
            mi.setMapName(jo.getAsJsonPrimitive("mapName").getAsString());
            mi.setFileName(jo.getAsJsonPrimitive("fileName").getAsString());
            mi.setPageVisible(jo.getAsJsonPrimitive("pageVisible").getAsBoolean());
            mi.setMargin(jo.getAsJsonPrimitive("margin").getAsDouble());
            PageFormat pf = gson.fromJson(jo.getAsJsonObject("pageFormat"), PageFormat.class);
            mi.setPageFormat(pf);
            if (jo.has("defaultFont")) {
                font = gson.fromJson(jo.get("defaultFont"), fontType);
                mi.setDefaultFont(font);
            }

            Type listOfCartographicElementsObject = 
                    new TypeToken<List<CartographicElement>>() {}.getType();
            JsonElement je2 = jo.get("cartographicElementList");
            ArrayList<CartographicElement> cartoElementList = gson.fromJson(je2, listOfCartographicElementsObject);
            ArrayList<MapArea> mapAreas = new ArrayList<>();
            for (CartographicElement ce : cartoElementList) {
                if (ce instanceof MapArea) {
                    mapAreas.add((MapArea) ce);
                }
            }
            for (int elementNumber = 0; elementNumber < cartoElementList.size(); elementNumber++) {
                for (CartographicElement ce : cartoElementList) {
                    if (ce.getElementNumber() == elementNumber) {
                        if (ce instanceof MapScale) {
                            MapScale ms = (MapScale) ce;
                            int mapAreaElementNumber = ms.getMapAreaElementNumber();
                            for (MapArea ma : mapAreas) {
                                if (ma.getElementNumber() == mapAreaElementNumber) {
                                    ms.setMapArea(ma);
                                    mi.addNewCartographicElement(ms);
                                }
                            }
                        } else if (ce instanceof Legend) {
                            Legend l = (Legend) ce;
                            for (MapArea ma : mapAreas) {
                                l.addMapArea(ma);
                            }
                            // check to see if any MapAreas are contained in any carto element groups
                            for (CartographicElement ce2 : cartoElementList) {
                                if (ce instanceof CartographicElementGroup) {
                                    CartographicElementGroup ceg = (CartographicElementGroup) ce;
                                    List<CartographicElement> elementList = ceg.getElementList();
                                    
                                }
                            }
                            mi.addNewCartographicElement(l);
                        } else {
                            mi.addNewCartographicElement(ce);
                        }
                    }
                }
            }
            //mi.removeCartographicElement(0); // removes the default maparea

            return mi;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
}
