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
package whitebox.plugins;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClasspathUtils {

    // Parameters
    private static final Class[] parameters = new Class[]{
        URL.class
    };

    /**
     * Adds the jars in the given directory to classpath
     * @param directory
     * @throws IOException
     */
    public static void addDirToClasspath(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                addURL(file.toURI().toURL());
            }
        } else {
            System.err.println("The directory \"" + directory + "\" does not exist!");
        }
    }

    /**
     * Add URL to CLASSPATH
     * @param u URL
     * @throws IOException IOException
     */
    public static void addURL(URL u) throws IOException {
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL urls[] = sysLoader.getURLs();
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString().equalsIgnoreCase(u.toString())) {
                return;
            }
        }
        Class sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[]{u});
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException t) {
            throw new IOException("Error, could not add URL to system classloader");
        }
    }
}
