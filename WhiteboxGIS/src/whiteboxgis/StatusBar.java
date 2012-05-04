/*
 *  Copyright (C) 2011 John Lindsay
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whiteboxgis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import whitebox.interfaces.WhiteboxPluginHost;


/**
 *
 * @author John Lindsay
 */
public class StatusBar extends JPanel implements MouseListener {

    private JLabel label = new JLabel();
    private JProgressBar progress = new JProgressBar();
    private JLabel progressLabel = new JLabel();
    private JLabel cancelOp = new JLabel("cancel");
    private WhiteboxPluginHost myHost = null;
    private boolean progressVisible = true;

    public StatusBar(WhiteboxPluginHost host) {
        myHost = host;
        createGui();
    }
    
    public StatusBar(WhiteboxPluginHost host, boolean progressVisible) {
        myHost = host;
        this.progressVisible = progressVisible;
        createGui();
    }
    
    private void createGui() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(10, 23));

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        progressLabel.setText("Progress: ");
        rightPanel.add(progressLabel);
        rightPanel.add(progress);
        setProgressVisible(progressVisible);
        rightPanel.add(Box.createHorizontalStrut(5));
        cancelOp.setForeground(Color.BLUE.darker());
        cancelOp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelOp.addMouseListener(this);
        rightPanel.add(cancelOp);
        rightPanel.add(Box.createHorizontalStrut(12));
        rightPanel.setOpaque(false);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(600, 23));
        label.setText("   Ready");
        leftPanel.add(label, BorderLayout.CENTER);
        leftPanel.setOpaque(false);

        add(rightPanel, BorderLayout.EAST);
        add(leftPanel, BorderLayout.WEST);
    }
    
    public boolean isProgressVisible() {
        return progressVisible;
    }

    public void setProgressVisible(boolean progressVisible) {
        this.progressVisible = progressVisible;
        progressLabel.setVisible(progressVisible);
        progress.setVisible(progressVisible);
    }
    
    
    public void setMessage(String message) {
        label.setText(" " + message);
    }

    public void setProgressLabel(String message) {
        progressLabel.setText(message + " ");
    }

    public void setProgress(int progressValue) {
        progress.setValue(progressValue);
        if (progressValue != 0) {
            progress.setStringPainted(true);
        } else {
            progress.setStringPainted(false);
            progressLabel.setText("Progress: ");
        }
    }

    // the following 5 methods must be defined if you
    // implements MouseListener
    @Override
    public void mouseClicked(MouseEvent arg0) {
        myHost.cancelOperation();
    }
    // mouse entered the JLabel increment count and display it

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }
    // mouse was presssed (cliked and released)
    // increment counter and display it

    @Override
    public void mousePressed(MouseEvent arg0) {
        cancelOp.setForeground(Color.RED.darker());
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
        cancelOp.setForeground(Color.BLUE.darker());
    }
//  protected void paintComponent(Graphics g) {
//    super.paintComponent(g);
//
//    int y = 0;
//    g.setColor(new Color(156, 154, 140));
//    g.drawLine(0, y, getWidth(), y);
//    y++;
//    g.setColor(new Color(196, 194, 183));
//    g.drawLine(0, y, getWidth(), y);
//    y++;
//    g.setColor(new Color(218, 215, 201));
//    g.drawLine(0, y, getWidth(), y);
//    y++;
//    g.setColor(new Color(233, 231, 217));
//    g.drawLine(0, y, getWidth(), y);
//
//    y = getHeight() - 3;
//    g.setColor(new Color(233, 232, 218));
//    g.drawLine(0, y, getWidth(), y);
//    y++;
//    g.setColor(new Color(233, 231, 216));
//    g.drawLine(0, y, getWidth(), y);
//    y = getHeight() - 1;
//    g.setColor(new Color(221, 221, 220));
//    g.drawLine(0, y, getWidth(), y);
//
//  }
}
class AngledLinesWindowsCornerIcon implements Icon {

    private static final Color WHITE_LINE_COLOR = new Color(255, 255, 255);
    private static final Color GRAY_LINE_COLOR = new Color(172, 168, 153);
    private static final int WIDTH = 13;
    private static final int HEIGHT = 13;

    @Override
    public int getIconHeight() {
        return WIDTH;
    }

    @Override
    public int getIconWidth() {
        return HEIGHT;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {

        g.setColor(WHITE_LINE_COLOR);
        g.drawLine(0, 12, 12, 0);
        g.drawLine(5, 12, 12, 5);
        g.drawLine(10, 12, 12, 10);

        g.setColor(GRAY_LINE_COLOR);
        g.drawLine(1, 12, 12, 1);
        g.drawLine(2, 12, 12, 2);
        g.drawLine(3, 12, 12, 3);

        g.drawLine(6, 12, 12, 6);
        g.drawLine(7, 12, 12, 7);
        g.drawLine(8, 12, 12, 8);

        g.drawLine(11, 12, 12, 11);
        g.drawLine(12, 12, 12, 12);

    }
}
