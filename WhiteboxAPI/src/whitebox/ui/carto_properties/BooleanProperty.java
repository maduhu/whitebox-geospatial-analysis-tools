
package whitebox.ui.carto_properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;
import javax.swing.*;
/**
 *
 * @author johnlindsay
 */
public class BooleanProperty extends JComponent implements MouseListener  {
    private String labelText;
    private Boolean value = false;
    private Color backColour = Color.WHITE;
    private int leftMargin = 10;
    private int rightMargin  = 10;
    private int preferredWidth = 200;
    private int preferredHeight = 24;
    
    private JCheckBox check = new JCheckBox();
    
    public BooleanProperty() {
        setOpaque(true);
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        revalidate();
    }
    
    public BooleanProperty(String labelText, Boolean value) {
        setOpaque(true);
        this.labelText = labelText;
        this.value = value;
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        revalidate();
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
        //createUI();
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
        //createUI();
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        if (!value.equals(this.value)) {
            Boolean oldValue = this.value;
            this.value = value;
            firePropertyChange("value", oldValue, value);
        }
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
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

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
        //createUI();
    }
    
    @Override
    public final void revalidate() {
        this.removeAll();
        
        this.setBackground(backColour);
        this.add(Box.createHorizontalStrut(leftMargin));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        this.add(label);
        this.add(Box.createHorizontalGlue());
        check.setSelected(value);
        check.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setValue(check.isSelected());
            }
        });
        this.add(check);
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
