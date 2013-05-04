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
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.lang.reflect.Type;
/**
 *
 * @author johnlindsay
 */
public class PageFormatDeserializer implements JsonDeserializer<PageFormat> {
    @Override
    public PageFormat deserialize(JsonElement je, Type type, 
                                JsonDeserializationContext jdc)
                           throws JsonParseException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Paper.class, new PaperDeserializer());
        gsonBuilder.registerTypeAdapter(Paper.class, new PaperSerializer());
        Gson gson = gsonBuilder.create(); 
        
        JsonObject jo = je.getAsJsonObject();
        PageFormat p = new PageFormat();
        p.setOrientation(jo.getAsJsonPrimitive("mOrientation").getAsInt());
        Paper paper = gson.fromJson(jo.getAsJsonObject("mPaper"), Paper.class);
        p.setPaper(paper);
        return p;
    }
}
    
