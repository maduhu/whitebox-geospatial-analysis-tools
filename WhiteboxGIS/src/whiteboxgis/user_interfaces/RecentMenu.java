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
package whiteboxgis.user_interfaces;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author johnlindsay
 */
public class RecentMenu extends JMenu {

    private int numItemsToStore = 8;
    // The problem with using a LinkedHashSet is that you can't move a 
    // newly added duplicate to the top of the list...it stays in it's 
    // originally added order.
    private List<String> list = new ArrayList<>();
    
    // constructors
    public RecentMenu() {
        setText("Recent");
        createMenuItems();
    }

    public RecentMenu(String label) {
        setText(label);
        createMenuItems();
    }

    // properties
    public int getNumItemsToStore() {
        return numItemsToStore;
    }

    public void setNumItemsToStore(int numItemsToStore) {
        this.numItemsToStore = numItemsToStore;
    }
    
    public List<String> getList() {
        return list;
    }
    
    // methods
    public void addMenuItem(String item) {
        list.add(0, item);
        
        // remove duplicates, while leaving the top item
        String str;
        if (list.size() > 1) {
            // scan the list from the last entry to the first.
            for (int i = list.size() - 1; i > 0; i--) {
                str = list.get(i);
                if (str.equals(item)) {
                    list.remove(i);
                }
            }
        }
        
        // don't allow the list to get larger than numItemsToStore
        if (list.size() > numItemsToStore) {
            list.remove(list.size() - 1);
        }
        createMenuItems();
    }
    
    public void removeAllMenuItems() {
        list.clear();
        createMenuItems();
    }

    private void createMenuItems() {
        this.removeAll();
        if (list.isEmpty()) {
            JMenuItem mi = new JMenuItem("Empty");
            this.add(mi);
        } else {
            for (String str : list) {
                JMenuItem mi = new JMenuItem(str);
                mi.addActionListener(this.actionListener);
                mi.setActionCommand(str);
                this.add(mi);
            }
        }
    }    
}
