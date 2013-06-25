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

import whitebox.interfaces.WhiteboxPlugin;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.ArrayList;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class StandardPluginService implements PluginService
{
    private static StandardPluginService pluginService;
    private ServiceLoader<WhiteboxPlugin> serviceLoader;
    private int numberOfPlugins = 0;
    
    public final static int SIMPLE_NAME = 0;
    public final static int DESCRIPTIVE_NAME = 1;
    
    private StandardPluginService()
    {
        //load all the classes in the classpath that have implemented the interface
        serviceLoader = ServiceLoader.load(WhiteboxPlugin.class);
    }

    public static StandardPluginService getInstance()
    {
        if(pluginService == null)
        {
            pluginService = new StandardPluginService();
        }
        return pluginService;
    }

    @Override
    public Iterator<WhiteboxPlugin> getPlugins()
    {
        return serviceLoader.iterator();
    }

    @Override
    public void initPlugins()
    {
        Iterator<WhiteboxPlugin> iterator = getPlugins();
        if(!iterator.hasNext())
        {
            System.err.println("No plugins were found!");
        }
        while(iterator.hasNext())
        {
            WhiteboxPlugin plugin = iterator.next();
            //System.out.println("Initializing the plugin " + plugin.getName());
            numberOfPlugins++;
        }
    }

    @Override
    public WhiteboxPlugin getPlugin(String pluginName, int nameType) {
        ServiceLoader<WhiteboxPlugin> serviceLoader = ServiceLoader.load(WhiteboxPlugin.class);
        Iterator<WhiteboxPlugin> iterator = serviceLoader.iterator(); //getPlugins();
        if (nameType == DESCRIPTIVE_NAME) {
            while (iterator.hasNext()) {
                WhiteboxPlugin plugin = iterator.next();
                if (plugin.getDescriptiveName().equals(pluginName)) {
                    return plugin;
                }
            }
        } else {
            while (iterator.hasNext()) {
                WhiteboxPlugin plugin = iterator.next();
                if (plugin.getName().equals(pluginName)) {
                    return plugin;
                }
            }
        }

        // no plugin by that name has been located. Return null.
        return null;
    }

    @Override
    public int getNumberOfPlugins() {
        return numberOfPlugins;
    }
    
    @Override
    public ArrayList getPluginList() {
        ArrayList<PluginInfo> plugInfo = new ArrayList<>();
        
        Iterator<WhiteboxPlugin> iterator = getPlugins();
        while(iterator.hasNext())
        {
            WhiteboxPlugin plugin = iterator.next();
            plugInfo.add(new PluginInfo(plugin.getName(), 
                    plugin.getDescriptiveName(), plugin.getToolDescription(),
                    PluginInfo.SORT_MODE_USAGE));
        }
        
        return plugInfo;
    }
}