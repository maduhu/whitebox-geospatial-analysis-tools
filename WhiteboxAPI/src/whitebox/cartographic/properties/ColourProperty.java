
package whitebox.cartographic.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;

/**
 *
 * @author johnlindsay
 */
public class ColourProperty extends JComponent implements MouseListener {
    private String labelText;
    private Color value;
    private int leftMargin = 10;
    private int rightMargin  = 10;
    private int preferredWidth = 180;
    private int preferredHeight = 24;
    private Color backColour = Color.WHITE;
    
    private SampleColour colourPanel;
    private int sampleWidth = 30;
    private int sampleHeight = 15;
    
    public ColourProperty() {
        this.setOpaque(true);
        revalidate();
    }
    
    public ColourProperty(String labelText, Color colour) {
        this.labelText = labelText;
        this.value = colour;
        this.setOpaque(true);
        revalidate();
    }

    public Color getValue() {
        return value;
    }

    public void setValue(Color colour) {
        Color oldColour = this.value;
        this.value = colour;
        firePropertyChange("value", oldColour, colour);
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
        //createUI();
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
        //createUI();
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
        //createUI();
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
        //createUI();
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
        //createUI();
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
        colourPanel = new SampleColour(sampleWidth, sampleHeight, value);
        colourPanel.setToolTipText("Click to select new color.");
        colourPanel.addMouseListener(this);
        this.add(colourPanel);
        this.add(Box.createHorizontalStrut(rightMargin));
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
        if (source == colourPanel) {
            Color newColour = JColorChooser.showDialog(this, "Choose Color", value);
            if (newColour != null) {
                //colour = newColour;
                this.setValue(newColour);
                colourPanel.setBackColour(newColour);
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
