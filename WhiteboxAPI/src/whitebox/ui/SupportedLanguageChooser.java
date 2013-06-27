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
        
        String[] languages = { "Català (Spain)", "Chinese (Simplified)", 
            "Chinese (Traditional)", "Deutsch (Germany)", "Elliniká (Greece)", 
            "English (Canada)", "English (UK)", "English (US)", "Español (Spain)", 
            "French (Canada)", "Italiano (Italy)", "Persian (Iran)", "Polski (Poland)" };
        String[] codes = { "ca_ES", "zh_CN", "zh_TW", "de_DE", "el_GR", "en_CA", 
            "en_GB", "en_US", "es_ES", "fr_CA", "it_IT", "fa_IR", "pl_PL" };
        
        int selectedIndex = 0;
        String hostLanguageCode = myHost.getLanguageCountryCode();
        for (int a = 0; a < codes.length; a++) {
            if (codes[a].equals(hostLanguageCode)) {
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
                        case "chinese (simplified)":
                            code = "zh_CN";
                            break;
                        case "chinese (traditional)":
                            code = "zh_TW";
                            break;
//                        case "dutch (netherlands)":
//                            code = "fa_IR";
//                            break;
                        case "english (canada)":
                            code = "en_CA";
                            break;
                        case "english (uk)":
                            code = "en_GB";
                            break;
                        case "english (us)":
                            code = "en_US";
                            break;
                        case "elliniká (greece)":
                            code = "el_GR";
                            break;
                        case "persian (iran)":
                            code = "fa_IR";
                            break;
                        case "deutsch (germany)":
                            code = "de_DE";
                            break;
                        case "polski (poland)":
                            code = "pl_PL";
                            break;
                        case "català (spain)":
                            code = "ca_ES";
                            break;
                        case "español (spain)":
                            code = "es_ES";
                            break;
                        case ("italiano (italy)"):
                            code = "it_IT";
                            break;
                        case ("french (canada)"):
                            code = "fr_CA";
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
