import whitebox.utilities.FileUtilities

def pathSep = File.separator
def helpDir = "/Users/johnlindsay/Documents/programming/Whitebox/whitebox-geospatial-analysis-tools/WhiteboxGIS/resources/Help/" //pluginHost.getResourcesDirectory() + "Help" + pathSep
println helpDir

def helpFiles = FileUtilities.findAllFilesWithExtension(helpDir, "html", false)

int i = 0
int j = 0;
for (String str : helpFiles) {
	def helpFile = new File(str)
	def text = helpFile.text
	if (text.contains('SeeAlso">Scripting:</h2>')) {
		i++
	} else {
		println str
		j++
	}
//	processFileInplace(helpFile) { text ->
//    	text = text.replaceAll('<br><h2', '<h2')
//    	text.replaceAll('<b><i>NoData</b></i>', '<b><i>NoData</i></b>')
//		text.replaceAll('SeeAlso">Scripting Usage:', 'SeeAlso">Scripting:')
//	}
}

println i + " completed and " + j + " incomplete"

def processFileInplace(file, Closure processText) {
    def text = file.text
    file.write(processText(text))
}

println "done"
