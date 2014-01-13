import java.io.File
import whitebox.utilities.FileUtilities

try {
	// The following lines are necessary for the script 
	// to be recognized as a menu extension.
	def parentMenu = "Help"
	def menuLabel = "Report An Error"
	
	def logDirectory = pluginHost.getLogDirectory()
	
	def logFiles = FileUtilities.findAllFilesWithExtension(logDirectory, "xml", false)
	
	StringBuilder errorLogs = new StringBuilder()
	for (String str : logFiles) {
		def logFile = new File(str)
		errorLogs.append("\nLOG FILE: " + logFile.getName() + "\n")
		new File(str).eachLine { line -> errorLogs.append(line.replace("<","!LT!").replace(">","!GT!").replace("\"", "!QUOTE!") + "\n") }
		errorLogs.append("\n")
	}
	
	String username = System.getProperty("user.name")
	
	StringBuilder sb = new StringBuilder()
	sb.append("<!DOCTYPE html>")
	sb.append("<html>")
	sb.append("<head><meta charset=\"UTF-8\">")
	sb.append("<title>Error Report</title><style media=\"screen\" type=\"text/css\">")
	sb.append("h2 { color: #000000; font-size : 12px; font-family: Georgia, \"Times New Roman\", Times, serif; font-weight:bold; }")
	sb.append("p { color: #000000; font-size: 11px; font-family: Verdana, Geneva, Arial, Helvetica, sans-serif; text-align: left; line-height: 115%; margin-left: 20px; margin-right: 20px; }")
	sb.append("label { color: #000000; font-size: 11px; font-family: Verdana, Geneva, Arial, Helvetica, sans-serif; line-height: 115%; margin-left: 20px; margin-right: 20px; }")
	sb.append("</style> </head> <body>")
	sb.append("<form onsubmit=\"return true\" action=\"http://www.uoguelph.ca/cgi-bin/FormMail.pl\"")
	sb.append("method=\"post\" name=\"form1\" id=\"form1\">")
	sb.append("<div align=\"center\"><h2>Error Report</h2>")
	sb.append("<p>Describe the details of what you were doing when you encountered the error (e.g. What tool were you using? What type of data were you processing? Was there an error message, and if so, what did it say? etc.) in the text box below:</p><br>")
	sb.append("<textarea name=\"errorDescription\" form=\"form1\" rows=\"10\" cols=\"40\"></textarea><br>")
	sb.append("<p>The contents of your log files will be attached.</p><br>")
	sb.append("<input value=\"").append(errorLogs.toString()).append("\" name=\"errorlogs\" type=\"hidden\">")
	sb.append("<input value=\"Clear\" type=\"reset\"><input value=\"Submit\" type=\"submit\">") 
	sb.append("<input value=\"jlindsay@uoguelph.ca\" name=\"recipient\" type=\"hidden\">")
	sb.append("<input value=\"Whitebox GAT error report\" name=\"subject\" type=\"hidden\">")
	sb.append("<input name=\"realname\" value=\"").append(username).append("\" type=\"hidden\">")
	sb.append("<input value=\"").append(username).append("\" name=\"username\" type=\"hidden\">")
	sb.append("<input name=\"required\" value=\"errorDescription, realname\" type=\"hidden\"> </div>")
	sb.append("</div></form></body></html>")
	
	String fileName = pluginHost.getWorkingDirectory() + "ErrorReport.html"
	File htmlFile = new File(fileName)
	if (htmlFile.exists()) {
		htmlFile.delete()
	}
	new File(fileName).withWriter { out ->
	    out.writeLine(sb.toString())
	}
	pluginHost.returnData(fileName)

} catch (Exception e) {
    pluginHost.showFeedback(e.getMessage())
    pluginHost.logException("Error in ReportError", e)
}
