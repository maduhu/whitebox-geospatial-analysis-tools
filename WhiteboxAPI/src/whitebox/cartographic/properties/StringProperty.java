
package whitebox.cartographic.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

/**
 *
 * @author johnlindsay
 */
public class StringProperty extends JComponent implements MouseListener  {
    private String labelText;
    private String value;
    private Color backColour = Color.WHITE;
    private int leftMargin = 10;
    private int rightMargin  = 10;
    private int preferredWidth = 200;
    private int preferredHeight = 24;
    private int textboxWidth = 15;
    
    private JTextField text = new JTextField();
    
    public StringProperty() {
        setOpaque(true);
        revalidate();
    }
    
    public StringProperty(String labelText, String value) {
        setOpaque(true);
        this.labelText = labelText;
        this.value = value;
        revalidate();
    }

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
        text = new JTextField(value, textboxWidth);
        text.setText(value);
        // Listen for changes in the text
        text.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void update() {
                setValue(text.getText());
//                if (Integer.parseInt(text.getText()) <= 0) {
//                    JOptionPane.showMessageDialog(null,
//                            "Error: Please enter number bigger than 0", "Error Massage",
//                            JOptionPane.ERROR_MESSAGE);
//                }
            }
        });
        this.add(text);
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
