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
package whitebox.utilities;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class FileUtilities {

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }

        destFile.createNewFile();
        
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
    
    public static ArrayList<String> findAllFilesWithExtension(String dir, String extension, boolean searchSubDirectories) {
        return findAllFilesWithExtension(new File(dir), extension, searchSubDirectories);
    }
    
    private static ArrayList<String> foundFiles = new ArrayList<>();
    private static boolean recursive;
    public static ArrayList<String> findAllFilesWithExtension(File dir, String extension, boolean searchSubDirectories) {
        foundFiles.clear();
        recursive = searchSubDirectories;
        findAllFilesWithExtension2(dir, extension);
        ArrayList<String> ret = new ArrayList<>();
        for (String str : foundFiles) {
            ret.add(str);
        }
        return ret;
    }
    
    private static ArrayList<String> findAllFilesWithExtension2(File dir, String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory() && recursive) {
                findAllFilesWithExtension2(files[i], extension);
            } else if (files[i].getName().contains(extension)) {
                foundFiles.add(files[i].toString());
            }
        }
        return foundFiles;
        
    }
    
    public static String readFileAsString(String fileName) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(fileName).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(fileName));
            f.read(buffer);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ignored) {
                }
            }
        }
        return new String(buffer);
    }
    
    public static void fillFileWithString(String fileName, String str) throws java.io.IOException {
        File file = new File(fileName);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            out.print(str);
            
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }

        }
    }
    
    public static String removeFileExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1) {
            return filename;
        }

        return filename.substring(0, extensionIndex);
    }
    
    public static String getFileExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1) {
            return filename;
        }

        return filename.substring(extensionIndex + 1);
    }
    
    public static boolean fileExists(String fileName) {
        return (new File(fileName)).exists();
    }
    
}
