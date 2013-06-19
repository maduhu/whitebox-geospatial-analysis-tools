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
package whitebox.internationalization;

import java.util.Locale;
import java.util.ResourceBundle;
/**
 *
 * @author johnlindsay
 */
public final class WhiteboxInternationalizationTools {
    private static Locale currentLocale = new Locale("en", "CA");
    
    public static Locale getLocale() {
        return currentLocale;
    }
    
    public static void setLocale(String languageCode, String countryCode) {
        currentLocale = new Locale(languageCode, countryCode);
    }
    
    public static ResourceBundle getGuiLabelsBundle() {
        return ResourceBundle.getBundle("whitebox.internationalization.GuiLabelsBundle", currentLocale);
            
    }
    
    public static ResourceBundle getMessagesBundle() {
        return ResourceBundle.getBundle("whitebox.internationalization.messages", currentLocale);
            
    }
}
