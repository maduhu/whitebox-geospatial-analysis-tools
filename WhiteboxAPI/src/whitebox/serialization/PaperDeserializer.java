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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.awt.print.Paper;
import java.lang.reflect.Type;
/**
 *
 * @author johnlindsay
 */
public class PaperDeserializer implements JsonDeserializer<Paper> {
    @Override
    public Paper deserialize(JsonElement je, Type type, 
                                JsonDeserializationContext jdc)
                           throws JsonParseException
    {
        JsonObject jo = je.getAsJsonObject();
        Paper p = new Paper();
        p.setSize(jo.getAsJsonPrimitive("mWidth").getAsDouble(), 
                jo.getAsJsonPrimitive("mHeight").getAsDouble());
        p.setImageableArea(jo.getAsJsonPrimitive("mImageableX").getAsDouble(), 
                jo.getAsJsonPrimitive("mImageableY").getAsDouble(), 
                jo.getAsJsonPrimitive("mImageableWidth").getAsDouble(), 
                jo.getAsJsonPrimitive("mImageableHeight").getAsDouble());
        return p;
    }
}
