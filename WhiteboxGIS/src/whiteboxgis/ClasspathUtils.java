package whiteboxgis;

/**
 *
 * @author johnlindsay
 */
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClasspathUtils {

 // Parameters
 private static final Class[] parameters = new Class[]
 {
     URL.class
 };

 /**
 * Adds the jars in the given directory to classpath
 * @param directory
 * @throws IOException
 */
 public static void addDirToClasspath(File directory) throws IOException
 {
     if (directory.exists())
     {
         File[] files = directory.listFiles();
         for (int i = 0; i < files.length; i++)
         {
             File file = files[i];
             addURL(file.toURI().toURL());
         }
     }
     else
     {
         System.err.println("The directory \"" + directory + "\" does not exist!");
     }
}

 /**
 * Add URL to CLASSPATH
 * @param u URL
 * @throws IOException IOException
 */
 public static void addURL(URL u) throws IOException
 {
     URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
     URL urls[] = sysLoader.getURLs();
     for (int i = 0; i < urls.length; i++)
     {
         if (urls[i].toString().equalsIgnoreCase(u.toString()))
         {
             System.err.println("URL " + u + " is already in the CLASSPATH");
             return;
         }
     }
     Class sysclass = URLClassLoader.class;
     try
     {
         Method method = sysclass.getDeclaredMethod("addURL", parameters);
         method.setAccessible(true);
         method.invoke(sysLoader, new Object[] { u });
     } catch (Throwable t)
     {
         t.printStackTrace();
         throw new IOException("Error, could not add URL to system classloader");
     }
  }
}
