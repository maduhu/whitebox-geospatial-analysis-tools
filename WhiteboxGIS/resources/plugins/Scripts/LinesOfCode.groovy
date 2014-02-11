import whitebox.utilities.FileUtilities

def pathSep = File.separator
def sd = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "Scripts" + pathSep
//println sd

def groovyScriptFiles = FileUtilities.findAllFilesWithExtension(sd, "groovy", true)
def pythonScriptFiles = FileUtilities.findAllFilesWithExtension(sd, "py", true)

def numLinesGroovy = 0
def commentLines = 0
def numFiles = 0
boolean notComment = true
for (String file : groovyScriptFiles) {
	numFiles++
	def scriptFile = new File(file)
	scriptFile.eachLine { line ->
		String str = line.trim().replace("\t", "")
		if (str.contains("/*")) {
			notComment = false
		}
		if (!str.isEmpty() && !str.startsWith("//") && notComment) { 
			numLinesGroovy++ 
		} else if (!line.isEmpty()) {
			commentLines++
		}
		if (str.contains("*/")) {
			notComment = true
		}
	}
}

def numLinesPython = 0
notComment = true
boolean openComment = false
for (String file : pythonScriptFiles) {
	numFiles++
	def scriptFile = new File(file)
	scriptFile.eachLine { line -> 
		String str = line.trim().replace("\t", "")
		if (str.contains("'''")) {
			notComment = false
		}
		if (!str.isEmpty() && !str.startsWith("#") && notComment) { 
			numLinesPython++ 
		} else if (!line.isEmpty()) {
			commentLines++
			if (str.contains("'''") && !openComment) {
				openComment = true
			} else if (str.contains("'''") && openComment) {
				openComment = false
				notComment = true
			}
		}
	}
}


sd = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "source_files" + pathSep

def javaFiles = FileUtilities.findAllFilesWithExtension(sd, "java", true)
def numLinesJava = 0
for (String file : javaFiles) {
	numFiles++
	def scriptFile = new File(file)
	scriptFile.eachLine { line ->
		String str = line.trim().replace("\t", "")
		if (str.contains("/*")) {
			notComment = false
		}
		if (!str.isEmpty() && !str.startsWith("//") && notComment) { 
			numLinesJava++ 
		} else if (!line.isEmpty()) {
			commentLines++
		}
		if (str.contains("*/")) {
			notComment = true
		}
	}
}


sd = "/Users/johnlindsay/Documents/Programming/Whitebox/trunk/WhiteboxAPI/src/"

javaFiles = FileUtilities.findAllFilesWithExtension(sd, "java", true)
for (String file : javaFiles) {
	numFiles++
	def scriptFile = new File(file)
	scriptFile.eachLine { line ->
		String str = line.trim().replace("\t", "")
		if (str.contains("/*")) {
			notComment = false
		}
		if (!str.isEmpty() && !str.startsWith("//") && notComment) { 
			numLinesJava++ 
		} else if (!line.isEmpty()) {
			commentLines++
		}
		if (str.contains("*/")) {
			notComment = true
		}
	}
}

sd = "/Users/johnlindsay/Documents/Programming/Whitebox/trunk/WhiteboxGIS/src/"

javaFiles = FileUtilities.findAllFilesWithExtension(sd, "java", true)
for (String file : javaFiles) {
	numFiles++
	def scriptFile = new File(file)
	scriptFile.eachLine { line ->
		String str = line.trim().replace("\t", "")
		if (str.contains("/*")) {
			notComment = false
		}
		if (!str.isEmpty() && !str.startsWith("//") && notComment) { 
			numLinesJava++ 
		} else if (!line.isEmpty()) {
			commentLines++
		}
		if (str.contains("*/")) {
			notComment = true
		}
	}
}

println("There are ${numLinesGroovy} lines of Groovy, ${numLinesJava} lines of Java, ${numLinesPython} lines of Python, and ${numLinesJava + numLinesGroovy + numLinesPython} lines overall in ${numFiles} script files.")
//println("There were also ${commentLines} lines of comments excluded.")