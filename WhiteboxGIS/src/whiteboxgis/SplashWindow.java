/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whiteboxgis;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.File;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class SplashWindow extends JWindow {

    String fileName = "";
    int waitTime = 2000;
    String versionNumber = "";
    int width = 0;
    int height = 0;
    Image image;

    public SplashWindow(String filename, int waitTime, String version) { //Frame f, 
        //super(f);
        this.fileName = filename;
        this.waitTime = waitTime;
        this.versionNumber = version;
        createGui();
    }
    private boolean value = true;

    public boolean getValue() {
        return value;
    }

    private void createGui() {
        try {
            if (System.getProperty("os.name").contains("Mac")) {
                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }

            File file = new File(fileName);
            image = ImageIO.read(file);
            
            width = 200;
            height = 200;
            
            JLabel l = new JLabel(new ImageIcon(fileName));
            width = l.getWidth();
            height = l.getHeight();

            getContentPane().add(l, BorderLayout.CENTER);
            pack();
            Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width / 2 - (labelSize.width / 2),
                    screenSize.height / 2 - (labelSize.height / 2));
            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    setVisible(false);
                    value = false;
                    //dispose();
                }
            });
            final int pause = waitTime;
            final Runnable closerRunner = new Runnable() {

                @Override
                public void run() {
                    setVisible(false);
                    value = false;
                    //dispose();
                }
            };
            Runnable waitRunner = new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(pause);
                        SwingUtilities.invokeAndWait(closerRunner);
                    } catch (Exception e) {
                        System.out.println(e);
                        // can catch InvocationTargetException
                        // can catch InterruptedException
                    }
                }
            };
            setVisible(true);
            Thread splashThread = new Thread(waitRunner, "SplashThread");
            splashThread.start();
        } catch (Exception e) {
        }
    }

    @Override
    public void paint(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;

            g2d.drawImage(image, 0, 0, this);
            FontMetrics metrics = g2d.getFontMetrics();
            int x = 185;
            int y = 320;
            int dY = metrics.getHeight() + 1;
            
            String str = "Version " + versionNumber + " released 2013";
            g2d.drawString(str, x, y);
            str = "Dr. John Lindsay (Lead Developer)";
            g2d.drawString(str, x, y + 2 * dY);
            //str = "The Department of Geography";
            //g2d.drawString(str, x, y + 3 * dY);
            str = "The University of Guelph, Canada";
            g2d.drawString(str, x, y + 3 * dY);
            str = "e-mail: jlindsay@uoguelph.ca";
            g2d.drawString(str, x, y + 4 * dY);
        } catch (Exception e) {
        }
    }
}