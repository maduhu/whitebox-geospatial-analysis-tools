package whiteboxgis;

/**
 *
 * @author johnlindsay
 */
import whitebox.interfaces.WhiteboxPlugin;
import java.util.Iterator;
import java.util.ArrayList;

public interface PluginService {
    Iterator<WhiteboxPlugin> getPlugins();
    void initPlugins();
    WhiteboxPlugin getPlugin(String pluginName, int nameType);
    int getNumberOfPlugins();
    ArrayList getPluginList();
}