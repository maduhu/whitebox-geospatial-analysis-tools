import whitebox.utilities.FileUtilities

def pathSep = File.separator
//def helpDir = "/Users/johnlindsay/Documents/programming/Whitebox/whitebox-geospatial-analysis-tools/WhiteboxGIS/resources/Help/" //pluginHost.getResourcesDirectory() + "Help" + pathSep
def helpDir = "/Users/johnlindsay/Documents/webpages/Hydrogeomatics/"
println helpDir

def helpFiles = FileUtilities.findAllFilesWithExtension(helpDir, "html", true)

int i = 0
int j = 0;
for (String str : helpFiles) {
	def helpFile = new File(str)
//	def text = helpFile.text
//	if (text.contains('SeeAlso">Scripting:</h2>')) {
//		i++
//	} else {
//		println str
//		j++
//	}
	processFileInplace(helpFile) { text ->
		def str1 = 'content="application/xhtml+xml'
		def str2 = 'content="text/html'
		text = text.replaceAll(str1, str2)
    	//text = text.replaceAll('<html>', '<html lang="en">')
    	//text = text.replaceAll('<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">', "<!DOCTYPE html>")
    	//text = text.replaceAll('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">', '<!DOCTYPE html>')
    	//text.replaceAll('<b><i>NoData</b></i>', '<b><i>NoData</i></b>')
		//text.replaceAll('SeeAlso">Scripting Usage:', 'SeeAlso">Scripting:')
	}
}

//println i + " completed and " + j + " incomplete"

def processFileInplace(file, Closure processText) {
    def text = file.text
    def text2 = processText(text)
    if (!text.equals(text2)) {
    	file.write(processText(text2))
    }
}

println "done"
