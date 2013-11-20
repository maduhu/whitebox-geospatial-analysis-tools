import java.util.Collections
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.geospatialfiles.shapefile.attributes.*
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable
import whitebox.geospatialfiles.shapefile.attributes.DBFField

//String dataFile = "/Users/johnlindsay/Documents/Data/Boundaries/WBUsage.csv"
String dataFile = "/Users/johnlindsay/Documents/Data/Boundaries/country_pop.csv"
def file = new File(dataFile)
//def text = file.text

HashMap<String, Double> hm = new HashMap<String, Double>()
file.eachLine {
	String[] entry = it.split(",")
	String country = entry[0].trim()
	String str = entry[1].replace("\"", "").replace(",", "")
	Double value = Double.parseDouble(str)
	hm.put(country, value)
}

HashMap<String, Double> hm2 = new HashMap<String, Double>()
file.eachLine {
	String[] entry = it.split(",")
	String country = entry[0].trim()
	String str = entry[2].replace("\"", "").replace(",", "")
	Double value = Double.parseDouble(str)
	hm2.put(country, value)
}

String databaseFile = "/Users/johnlindsay/Documents/Data/Boundaries/Countries3.dbf"
AttributeTable table = new AttributeTable(databaseFile)
numFeatures = table.getNumberOfRecords()
Object[] rec;
for (i = 0; i < numFeatures; i++) {
	rec = table.getRecord(i);
	String country = rec[1].toString().trim()
	if (hm.containsKey(country)) {
		rec[4] = hm.get(country)
		rec[5] = hm2.get(country)
		table.updateRecord(i, rec)
	} else {
		rec[4] = new Double(0.0)
		rec[5] = new Double(0.0)
		table.updateRecord(i, rec)
	}
}


println("done!")
