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
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.awt.print.PageFormat;
import java.lang.reflect.Type;
import java.util.List;
import whitebox.cartographic.MapInfo;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapInfoSerializer implements JsonSerializer<MapInfo> { 
    @Override
    public JsonElement serialize(MapInfo t, Type type, JsonSerializationContext jsc) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PageFormat.class, new PageFormatSerializer());
        gsonBuilder.registerTypeAdapter(CartographicElement.class, new CartographicElementSerializer());
        Gson gson = gsonBuilder.create();
        
        JsonObject jo = new JsonObject();
        
        jo.addProperty("mapName", t.getMapName());
        jo.addProperty("fileName", t.getFileName());
        jo.addProperty("pageVisible", t.isPageVisible());
        jo.addProperty("margin", t.getMargin());
        jo.add("defaultFont", gson.toJsonTree(t.getDefaultFont()));
        jo.add("pageFormat", gson.toJsonTree(t.getPageFormat()));
        Type listOfCartographicElementsObject = new TypeToken<List<CartographicElement>>(){}.getType();
        jo.add("cartographicElementList", gson.toJsonTree(t.getCartographicElementList(), listOfCartographicElementsObject));
         
        return jo;
    }
    
}
