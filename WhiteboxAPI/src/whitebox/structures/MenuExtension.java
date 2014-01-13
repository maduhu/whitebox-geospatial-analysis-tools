/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

package whitebox.structures;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * This class is used to point create a menu item that points to a script.
 * @author johnlindsay
 */
public class MenuExtension {
    private String scriptFile;
    private ParentMenu menu;
    private String label;
    private char keyStrokeChar;
    private boolean hasKeyStroke = false;
    private MenuType mt = MenuType.MENUITEM;
    
    public MenuExtension(String label, ParentMenu menu, String scriptFile) {
        this.label = label;
        this.menu = menu;
        this.scriptFile = scriptFile;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public ParentMenu getMenu() {
        return menu;
    }

    public void setMenu(ParentMenu menu) {
        this.menu = menu;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setAcceleratorKeyStroke(char strokeCharcter) {
        keyStrokeChar = strokeCharcter;
        hasKeyStroke = true;
    }
    
    public KeyStroke getAcceleratorKeyStroke() {
        if (hasKeyStroke) {
            return KeyStroke.getKeyStroke(keyStrokeChar, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        } else {
            return null;
        }
    }
    
    public enum ParentMenu {
        FILE, LAYERS, VIEW, CARTOGRAPHIC, TOOLS, HELP;
    }
    
    public enum MenuType {
        MENUITEM, MENU;
    }
}
