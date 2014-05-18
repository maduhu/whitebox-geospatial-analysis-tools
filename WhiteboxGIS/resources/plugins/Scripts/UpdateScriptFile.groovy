import java.io.File
import java.net.URL
import javax.swing.*
import groovy.io.FileType
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.ui.plugin_dialog.*
import whitebox.utilities.FileUtilities
import whiteboxgis.WhiteboxGui

public class Downloader {
	String site = ""
	String filename = ""
	WhiteboxGui pluginHost;
	
	public Downloader(WhiteboxGui pluginHost, String filename, String site) {
		this.pluginHost = pluginHost
		this.site = site
		this.filename = filename
	}
	
    public int download(String fn, long localLastModified) throws Exception {
    
	    try {
	    	
	        URL url=new URL(site);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setReadTimeout(100000)

			long lastModified = connection.getLastModified()

			if (localLastModified > lastModified) {
				if (pluginHost.showFeedback("The locally modifed file is newer than the file in the code repository." +
				"\nAre you sure you want to replace the local file?", 
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                	return
				}
			}
	        
	        int filesize = connection.getContentLength();
	        float totalDataRead = 0;
            java.io.BufferedInputStream inStream = new java.io.BufferedInputStream(connection.getInputStream());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(filename);
            java.io.BufferedOutputStream bout = new BufferedOutputStream(fos,1024);
            byte[] data = new byte[1024];
            int i = 0;
            while((i = inStream.read(data, 0, 1024)) >= 0) {
	            totalDataRead=totalDataRead+i;
	            bout.write(data,0,i);
	            float percent = (totalDataRead * 100f) / filesize;
	            pluginHost.updateProgress("Downloading ${fn}...", (int) percent)
	            // check to see if the user has requested a cancellation
				if (pluginHost.isRequestForOperationCancelSet()) {
					pluginHost.showFeedback("Operation cancelled")
					return false
				}
            }
            bout.close();
            inStream.close();
            pluginHost.updateProgress("Progress:", 0)
            return 0
	    } catch (FileNotFoundException e) {
		    return 1
	    } catch(SocketTimeoutException e) {
		    return 2
	    } catch(IOException e) {
		    return 3
	    } catch(Exception e) {
	    	pluginHost.logException("Error in " + descriptiveName, e)
		    return 4
	    }
	}
}

def updateScript = { scriptName ->
	
	WhiteboxGui wg = (WhiteboxGui)(pluginHost)

	// First get the existing file. In case something goes wrong 
	// with the download, you want to be able to bring back the 
	// old file
	String pathSep = File.separator
	String scriptDir = "${pluginHost.resourcesDirectory}plugins${pathSep}Scripts${pathSep}"
	String localFileName = "${scriptDir}${scriptName}"
	def localFile = new File(localFileName)
	long localFileModifiedTime = localFile.lastModified()
	String fileContents = localFile.text
	
	// Figure out the name of the remote file
	String remoteDir = "https://whitebox-geospatial-analysis-tools.googlecode.com/svn/trunk/WhiteboxGIS/resources/plugins/Scripts/"
	String downloadArtifact = "${remoteDir}/${scriptName}"

	// Download the remote file
	//String outFileName = "/Users/johnlindsay/Documents/${scriptName}" 
	Downloader d = new Downloader(wg, localFileName, downloadArtifact) 
	int ret = d.download(scriptName, localFileModifiedTime)
	if (ret == 0) {
		pluginHost.showFeedback("Successful update")
	} else {
		pluginHost.showFeedback("The update was unsuccessful")
		pluginHost.returnData(fileContents)
		localFile.delete()
		localFile.withWriter { out ->
		    out.write(fileContents)
		}
	}
}

def localScriptList = {
	def list = []
	String pathSep = File.separator
	String scriptDir = "${pluginHost.resourcesDirectory}plugins${pathSep}Scripts${pathSep}"
	def localDir = new File(scriptDir)
	localDir.eachFileRecurse(FileType.FILES) { file ->
	  list << file.getName()
	}
	list
}

def updateAllScripts = {

	println "LOCAL SCRIPTS"
	def k = localScriptList()
	k.each() { j -> println j }
	
	String dir = "https://whitebox-geospatial-analysis-tools.googlecode.com/svn/trunk/WhiteboxGIS/resources/plugins/Scripts/"
	
	URL url=new URL(dir);
	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	long lastModified = connection.getLastModified()
	connection.setReadTimeout(100000)
	int filesize = connection.getContentLength();
	//java.io.BufferedInputStream inStream = new java.io.BufferedInputStream(connection.getInputStream());
	BufferedReader inStream = new BufferedReader(new InputStreamReader(url.openStream()));
	
	def scriptNames = new ArrayList<String>()
	String line;
	while ((line = inStream.readLine()) != null) {
	    if (line.contains("a href=")) {
	    	int i = line.indexOf(">", line.indexOf(">") + 1)
	    	int j = line.indexOf("<", i)
	    	def content = line.substring(i + 1, j).trim()
	    	if (!content.equals("..")) {
	    		scriptNames.add(content)
	    	}
	    }
	}
	inStream.close()
	
	println "\nREMOTE SCRIPTS"
	scriptNames.each() { n -> println n }
}

if (args.length >= 1) {
	updateScript(args[0])
} else {
	updateAllScripts() //pluginHost.showFeedback("No script file was specified")
}
						
