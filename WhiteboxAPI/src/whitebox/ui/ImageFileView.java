/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
package whitebox.ui;
 
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;
 
/* ImageFileView.java is used by FileChooserDemo2.java. */
public class ImageFileView extends FileView {
//    ImageIcon jpgIcon = Utils.createImageIcon("images/jpgIcon.gif");
//    ImageIcon gifIcon = Utils.createImageIcon("images/gifIcon.gif");
//    ImageIcon tiffIcon = Utils.createImageIcon("images/tiffIcon.gif");
//    ImageIcon pngIcon = Utils.createImageIcon("images/pngIcon.png");
 
    @Override
    public String getName(File f) {
        return null; //let the L&F FileView figure this out
    }
 
    @Override
    public String getDescription(File f) {
        return null; //let the L&F FileView figure this out
    }
 
    @Override
    public Boolean isTraversable(File f) {
        return null; //let the L&F FileView figure this out
    }
 
    @Override
    public String getTypeDescription(File f) {
        String extension = Utils.getExtension(f);
        String type = null;
 
        if (extension != null) {
            switch (extension) {
                case Utils.jpeg:
                case Utils.jpg:
                    type = "JPEG Image";
                    break;
                case Utils.gif:
                    type = "GIF Image";
                    break;
                case Utils.tiff:
                case Utils.tif:
                    type = "TIFF Image";
                    break;
                case Utils.png:
                    type = "PNG Image";
                    break;
            }
        }
        return type;
    }
 
    @Override
    public Icon getIcon(File f) {
        String extension = Utils.getExtension(f);
        Icon icon = null;
 
        if (extension != null) {
//            switch (extension) {
//                case Utils.jpeg:
//                case Utils.jpg:
//                    icon = jpgIcon;
//                    break;
//                case Utils.gif:
//                    icon = gifIcon;
//                    break;
//                case Utils.tiff:
//                case Utils.tif:
//                    icon = tiffIcon;
//                    break;
//                case Utils.png:
//                    icon = pngIcon;
//                    break;
//            }
        }
        return icon;
    }
}
