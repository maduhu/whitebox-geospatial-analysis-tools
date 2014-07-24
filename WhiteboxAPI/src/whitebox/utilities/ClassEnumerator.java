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
package whitebox.utilities;

//import java.util.zip.ZipInputStream;
//import java.util.zip.ZipEntry;
//import java.net.MalformedURLException;
//import java.net.URLDecoder;
//import java.util.TreeSet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class can be used to identify the classes within a jar file or
 * directory.
 *
 * @author Dave Dopson, modified by johnlindsay
 */
public class ClassEnumerator {

    private static void log(String msg) {
        System.out.println("ClassDiscovery: " + msg);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
        }
    }

    private static void processDirectory(File directory, String pkgname, ArrayList<Class<?>> classes) {
        log("Reading Directory '" + directory + "'");
        // Get the list of the files contained in the package
        String[] files = directory.list();
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            String className = null;
            // we are only interested in .class files
            if (fileName.endsWith(".class")) {
                // removes the .class extension
                className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
            }
            log("FileName '" + fileName + "'  =>  class '" + className + "'");
            if (className != null) {
                classes.add(loadClass(className));
            }
            File subdir = new File(directory, fileName);
            if (subdir.isDirectory()) {
                processDirectory(subdir, pkgname + '.' + fileName, classes);
            }
        }
    }

    private static void processJarfile(URL resource, String pkgname, ArrayList<Class<?>> classes) {
        String relPath = pkgname.replace('.', '/');
        String resPath = resource.getPath();
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        log("Reading JAR file: '" + jarPath + "'");
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            return;
            //throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String className = null;
            if (entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
            }
            log("JarEntry '" + entryName + "'  =>  class '" + className + "'");
            if (className != null) {
                classes.add(loadClass(className));
            }
        }
    }

    private static void processDirectoryForClassNames(File directory, String pkgname, ArrayList<String> classes) {
        //log("Reading Directory '" + directory + "'");
        // Get the list of the files contained in the package
        String[] files = directory.list();
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            String className = null;
            // we are only interested in .class files
            if (fileName.endsWith(".class")) {
                // removes the .class extension
                className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
            }
            //log("FileName '" + fileName + "'  =>  class '" + className + "'");
            if (className != null && !className.contains("$")) {
                classes.add(className);
            } //else if (className == null && !fileName.contains("$") && !fileName.endsWith(".properties")) {
//                fileName = fileName.replace('/', '.').replace('\\', '.').replace(".class", "");
//                if (fileName.endsWith(".")) {
//                    fileName = fileName.substring(0, fileName.length() - 1);
//                }
//                classes.add(fileName);
//            }
            File subdir = new File(directory, fileName);
            if (subdir.isDirectory()) {
                processDirectoryForClassNames(subdir, pkgname + '.' + fileName, classes);
            }
        }
    }

    private static void processJarfileForClassNames(URL resource, String pkgname, ArrayList<String> classes) {
        String relPath = pkgname.replace('.', '/');
        String resPath = resource.getPath();
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        //log("Reading JAR file: '" + jarPath + "'");
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            return;
            //throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String className = null;
            if (entryName.startsWith(relPath)) { // && entryName.length() > (relPath.length() + 1)) {
                if (entryName.endsWith(".class")) {
                    className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                }
                if (className != null && !className.contains("$")) {
                    classes.add(className);
                } else if (className == null && !entryName.contains("$") && !entryName.endsWith(".properties")) {
                    entryName = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                    if (entryName.endsWith(".")) {
                        entryName = entryName.substring(0, entryName.length() - 1);
                    }
                    classes.add(entryName);
                }
            }
        }
    }

    public static ArrayList<String> getClassNamesForPackage(Package pkg) {
        ArrayList<String> classes = new ArrayList<>();

        String pkgname = pkg.getName();
        String relPath = pkgname.replace('.', '/');

        // Get a File object for the package
        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
        if (resource == null) {
            return null;
            //throw new RuntimeException("Unexpected problem: No resource for " + relPath);
        }
        //log("Package: '" + pkgname + "' becomes Resource: '" + resource.toString() + "'");

        resource.getPath();
        if (resource.toString().startsWith("jar:")) {
            processJarfileForClassNames(resource, pkgname, classes);
        } else {
            processDirectoryForClassNames(new File(resource.getPath()), pkgname, classes);
        }

        return classes;
    }

    public static ArrayList<String> getClassNamesForPackage(String pkgname) {
        ArrayList<String> classes = new ArrayList<>();

        String relPath = pkgname.replace('.', '/');
        
        // Get a File object for the package
        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
        if (resource == null) {
            return null;
            //throw new RuntimeException("Unexpected problem: No resource for " + relPath);
        }
        //log("Package: '" + pkgname + "' becomes Resource: '" + resource.toString() + "'");

        resource.getPath();
        if (resource.toString().startsWith("jar:")) {
            processJarfileForClassNames(resource, pkgname, classes);
        } else {
            processDirectoryForClassNames(new File(resource.getPath()), pkgname, classes);
        }

        return classes;

//        try {
//            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//            assert classLoader != null;
//            String path = pkgname.replace('.', '/');
//            Enumeration resources = classLoader.getResources(path);
//            ArrayList<String> dirs = new ArrayList();
//            while (resources.hasMoreElements()) {
//                URL resource = (URL) resources.nextElement();
//                dirs.add(URLDecoder.decode(resource.getFile(), "UTF-8"));
//            }
//            TreeSet classes = new TreeSet();
//            for (String directory : dirs) {
//                classes.addAll(findClasses(directory, pkgname));
//            }
//            ArrayList classList = new ArrayList();
//            for (Object clazz : classes) {
//                classList.add(Class.forName((String) clazz));
//            }
//            return classList;
//        } catch (Exception e) {
//            return null;
//            // do nothing
//        }
//////    }

//    private static TreeSet findClasses(String path, String packageName) throws MalformedURLException, IOException {
//        TreeSet classes = new TreeSet();
//        if (path.startsWith("file:") && path.contains("!")) {
//            String[] split = path.split("!");
//            URL jar = new URL(split[0]);
//            ZipInputStream zip = new ZipInputStream(jar.openStream());
//            ZipEntry entry;
//            while ((entry = zip.getNextEntry()) != null) {
//                if (entry.getName().endsWith(".class")) {
//                    String className = entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "").replace('/', '.');
//                    if (className.startsWith(packageName)) {
//                        classes.add(className);
//                    }
//                }
//            }
//        }
//        File dir = new File(path);
//        if (!dir.exists()) {
//            return classes;
//        }
//        File[] files = dir.listFiles();
//        for (File file : files) {
//            if (file.isDirectory()) {
//                assert !file.getName().contains(".");
//                classes.addAll(findClasses(file.getAbsolutePath(), packageName + "." + file.getName()));
//            } else if (file.getName().endsWith(".class")) {
//                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
//                classes.add(className);
//            }
//        }
//        return classes;
    }

    public static ArrayList<Class<?>> getClassesForPackage(Package pkg) {
        ArrayList<Class<?>> classes = new ArrayList<>();

        String pkgname = pkg.getName();
        String relPath = pkgname.replace('.', '/');

        // Get a File object for the package
        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
        if (resource == null) {
            throw new RuntimeException("Unexpected problem: No resource for " + relPath);
        }
        log("Package: '" + pkgname + "' becomes Resource: '" + resource.toString() + "'");

        resource.getPath();
        if (resource.toString().startsWith("jar:")) {
            processJarfile(resource, pkgname, classes);
        } else {
            processDirectory(new File(resource.getPath()), pkgname, classes);
        }

        return classes;
    }
}
