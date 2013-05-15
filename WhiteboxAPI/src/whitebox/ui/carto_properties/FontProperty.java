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
package whitebox.ui.carto_properties;

import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import whitebox.ui.JFontChooser;
/**
 *
 * @author jlindsay
 */
public class FontProperty extends JComponent implements MouseListener {
    private String labelText;
    private Font value;
    private int leftMargin = 10;
    private int rightMargin  = 10;
    private int preferredWidth = 180;
    private int preferredHeight = 24;
    private Color backColour = Color.WHITE;
    private JFontChooser fontChooser;
    private JTextField fontName = new JTextField();
    private int textboxWidth = 15;
    
    public FontProperty() {
        this.setOpaque(true);
        revalidate();
    }
    
    public FontProperty(String labelText, Font font) {
        this.labelText = labelText;
        this.value = font;
        this.setOpaque(true);
        revalidate();
    }

    public Font getValue() {
        return value;
    }

    public void setValue(Font font) {
        Font oldFont = this.value;
        this.value = font;
        firePropertyChange("value", oldFont, font);
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
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

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }
    
    public int getTextboxWidth() {
        return textboxWidth;
    }

    public void setTextboxWidth(int textboxWidth) {
        this.textboxWidth = textboxWidth;
    }
    
    
    @Override
    public final void revalidate() {
        this.removeAll();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBackground(backColour);
        this.add(Box.createHorizontalStrut(leftMargin));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        this.add(label);
        this.add(Box.createHorizontalGlue());
        fontName = new JTextField(value.getName(), textboxWidth);
        fontName.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                fontName.getPreferredSize().height));
        fontName.setToolTipText("Click to select new font.");
        fontName.addMouseListener(this);
        this.add(fontName);
        this.add(Box.createHorizontalStrut(rightMargin));
        fontChooser = new JFontChooser(value);
        super.revalidate();
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
        Object source = e.getSource();
        if (source == fontName) {
            Font newFont = fontChooser.showDialog(this, "Choose a font");
            if (newFont != null) {
                this.setValue(newFont);
                fontName.setText(newFont.getName());
            }
            
        }
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
    
    private class SampleColour extends JPanel {
        Color backColour;
        
        protected SampleColour(int width, int height, Color clr) {
            this.setMaximumSize(new Dimension(width, height));
            this.setPreferredSize(new Dimension(width, height));
            backColour = clr;
        }
        
        protected void setBackColour(Color clr) {
            backColour = clr;
            repaint();
        }
        
        @Override
        public void paint (Graphics g) {
            g.setColor(backColour);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            
            g.setColor(Color.black);
            g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
            
        }
    }
}
