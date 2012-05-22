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

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PluginServiceFactory {
    private static String pluginDir = null;

    public static PluginService createPluginService(String pluginDirectory) {
        pluginDir = pluginDirectory;
        addPluginJarsToClasspath();
        return StandardPluginService.getInstance();
    }

    private static void addPluginJarsToClasspath() {
        try {
            //add the plugin directory to classpath
            ClasspathUtils.addDirToClasspath(new File(pluginDir));
            //ClasspathUtils.addDirToClasspath(new File("plugins"));
        } catch (IOException ex) {
            System.out.println(PluginServiceFactory.class.getName() + " " + ex.getMessage());
        }
    }
}