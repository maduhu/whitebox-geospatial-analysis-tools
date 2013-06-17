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
package whitebox.ui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author johnlindsay
 */
public abstract class SupportedLanguageChooser {
    private static WhiteboxPluginHost myHost = null;
    
    public SupportedLanguageChooser() {
        
    }
    
    public static ComboBoxProperty getLanguageChooser(WhiteboxPluginHost host, 
            boolean suppressResartWarning) {
        myHost = host;
        ResourceBundle bundle = myHost.getGuiLabelsBundle();
        
        final boolean supressWarning = suppressResartWarning;
        
//        String[] languages = { "Chinese (China)", "Dutch (Netherlands)", "English (Canada)", "English (UK)", 
//            "English (US)", "Greek (Greece)", "Persian (Iran)" };
//        String[] codes = { "zh_CN", "nl_NL", "en_CA", "en_GB", "en_US", "el_GR", "fa_IR" };
        String[] languages = { "English (Canada)", "English (UK)", 
            "English (US)", "German (Germany)", "Greek (Greece)", "Persian (Iran)" };
        String[] codes = { "en_CA", "en_GB", "en_US", "de_DE", "el_GR", "fa_IR" };
        
        int selectedIndex = 0;
        for (int a = 0; a < codes.length; a++) {
            if (codes[a].equals(myHost.getLanguageCountryCode())) {
                selectedIndex = a;
            }
        }
        
        ComboBoxProperty languageChooser = new ComboBoxProperty(
                bundle.getString("Language") + ":", languages, selectedIndex);
        languageChooser.setName("languageChooser");
        ItemListener il = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    String code;
                    switch (item.toString().toLowerCase()) {
                        case "chinese (china)":
                            code = "zh_CN";
                            break;
                        case "dutch (netherlands)":
                            code = "fa_IR";
                            break;
                        case "english (canada)":
                            code = "en_CA";
                            break;
                        case "english (uk)":
                            code = "en_GB";
                            break;
                        case "english (us)":
                            code = "en_US";
                            break;
                        case "greek (greece)":
                            code = "el_GR";
                            break;
                        case "persian (iran)":
                            code = "fa_IR";
                            break;
                        case "german (germany)":
                            code = "de_DE";
                            break;
                        default:
                            code = "en_CA";
                    }
                    
                    myHost.setLanguageCountryCode(code);
                    if (!supressWarning) {
                        myHost.showFeedback(myHost.getMessageBundle()
                                .getString("CloseWhiteboxToTakeEffect"));
                    }
                }
            }
        };
        languageChooser.setParentListener(il);

        //languageChooser.addPropertyChangeListener("value", this);
        
        return languageChooser;
    }
}
