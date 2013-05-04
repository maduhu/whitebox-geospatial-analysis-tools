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

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonObject;
import java.awt.print.Paper;
import java.lang.reflect.Type;
/**
 *
 * @author johnlindsay
 */
public class PaperSerializer implements JsonSerializer<Paper> { 
    @Override
    public JsonElement serialize(Paper t, Type type, JsonSerializationContext jsc)
    {
        JsonObject jo = new JsonObject();
        jo.addProperty("mHeight", t.getHeight());
        jo.addProperty("mWidth", t.getWidth());
        jo.addProperty("mImageableHeight", t.getImageableHeight());
        jo.addProperty("mImageableWidth", t.getImageableWidth());
        jo.addProperty("mImageableX", t.getImageableX());
        jo.addProperty("mImageableY", t.getImageableY());
        return jo;
    }
    
}
