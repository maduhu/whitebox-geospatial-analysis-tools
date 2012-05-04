package whiteboxgis;

/**
 *
 * @author johnlindsay
 */
import java.io.File;
import java.io.IOException;

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