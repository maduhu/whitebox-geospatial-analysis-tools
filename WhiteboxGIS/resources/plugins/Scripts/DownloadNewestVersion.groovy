import groovy.time.*

try {
	def timeStart = new Date()
	URL website = new URL("http://www.uoguelph.ca/~hydrogeo/Whitebox/WhiteboxGAT.zip")
	File file = new File("/Users/johnlindsay/Documents/WhiteboxGAT.zip")
	if (file.exists()) {
		file.delete()
		println "File already exists. Deleting old version..."
	}
	println "Downloading file..."
	org.apache.commons.io.FileUtils.copyURLToFile(website, file, 10000, 10000)
	TimeDuration duration = TimeCategory.minus(new Date(), timeStart)
	println "Download complete. Operation took " + duration + " seconds"
} catch (IOException ioe) {
	println "IO Error downloading file"
} catch (Exception e) {
	println "Error downloading file"
}
