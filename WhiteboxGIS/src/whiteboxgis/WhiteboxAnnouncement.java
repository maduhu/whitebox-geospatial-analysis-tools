/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whiteboxgis;

/**
 *
 * @author johnlindsay
 */
public class WhiteboxAnnouncement {
    String date;
    String title = "";
    String message = "";
    
    // constructors
    public WhiteboxAnnouncement() {
        
    }
    
    public WhiteboxAnnouncement(String message) {
        this.message = message;
    }
    
    public WhiteboxAnnouncement(String message, String title) {
        this.message = message;
        this.title = title;
    }
    
    public WhiteboxAnnouncement(String message, String title, String date) {
        this.message = message;
        this.title = title;
        this.date = date;
    }
    
    // properties
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    
    // methods
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!title.trim().isEmpty()) {
            sb.append(title.toUpperCase()).append("\n");
        }
        if (!message.trim().isEmpty()) {
            sb.append(message).append("\n");
        }
        if (!date.trim().isEmpty()) {
            sb.append("(").append(date).append(")");
        }
        return sb.toString();
    }
}
