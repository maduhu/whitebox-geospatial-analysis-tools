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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author johnlindsay
 */
public class ComboBoxProperty extends JComponent implements MouseListener {
    private String labelText;
    private String value;
    private Color backColour = Color.WHITE;
    private int leftMargin = 10;
    private int rightMargin = 10;
    private int preferredWidth = 200;
    private int preferredHeight = 24;
    private String[] listItems;
    private int defaultItem = 0;
    private JComboBox combo = new JComboBox();
    private ItemListener parentListener;
    
    // constructors
    public ComboBoxProperty() {
        setOpaque(true);
        revalidate();
    }

    public ComboBoxProperty(String labelText, String[] listItems, int defaultItem) {
        setOpaque(true);
        this.labelText = labelText;
        this.listItems = listItems;
        this.defaultItem = defaultItem;
        revalidate();
    }

    // properties
    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        String oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
    }

    public ItemListener getParentListener() {
        return parentListener;
    }

    public void setParentListener(ItemListener parentListener) {
        this.parentListener = parentListener;
        revalidate();
    }
    
    public String[] getListItems() {
        return listItems;
    }

    public void setListItems(String[] listItems) {
        this.listItems = listItems;
    }

    public int getDefaultItem() {
        return defaultItem;
    }

    public void setDefaultItem(int defaultItem) {
        this.defaultItem = defaultItem;
    }

    // methods
    @Override
    public final void revalidate() {
        try {
        this.removeAll();

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBackground(backColour);
        this.add(Box.createHorizontalStrut(leftMargin));

        if (listItems == null) {
            return;
        }

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        this.add(label);
        this.add(Box.createHorizontalGlue());
        
        
        
        
        for (int i = 0; i < listItems.length; i++) {
            listItems[i] = listItems[i].trim();
        }
        combo = new JComboBox(listItems);
        combo.setSelectedIndex(defaultItem);
        value = (String) combo.getSelectedItem();
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                combo.getPreferredSize().height));
        combo.addItemListener(parentListener);
//        combo.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                if (e.getStateChange() == ItemEvent.SELECTED) {
//                    Object item = e.getItem();
//                    setValue(item.toString());
//                }
//            }
//        });
//        combo.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                setValue((String) combo.getSelectedItem());
//            }
//        });
        this.add(combo);
        this.add(Box.createHorizontalStrut(rightMargin));
        super.revalidate();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(getForeground());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}