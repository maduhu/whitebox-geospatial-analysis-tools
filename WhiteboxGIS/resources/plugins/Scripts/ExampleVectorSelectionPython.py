# This script shows how to select features in the active map
# layer (vector) based on attributes within the attribute table.
from whitebox.geospatialfiles.shapefile.attributes import AttributeTable
from  whitebox.interfaces import MapLayer
from whitebox.geospatialfiles import VectorLayerInfo
from whitebox.interfaces.MapLayer import MapLayerType
from java.util import ArrayList

# Make sure that the 'select features' button is active, otherwise
# the selected features won't be displayed.
pluginHost.setSelectFeature()
# Retrieve the active map layer and see if it is a vector
activeLayer = pluginHost.getActiveMapLayer()
if activeLayer.getLayerType() == MapLayerType.VECTOR:
	# activeLayer is a VectorLayerInfo
	activeLayer.clearSelectedFeatures()
	table = activeLayer.getShapefile().getAttributeTable()
	for i in xrange(0, table.getNumberOfRecords()):
		rec = table.getRecord(i)
		# Get some attribute values, in this case a description 
		# field and an area field.
		description = rec[1]
		area = float(rec[3])
		# Select the feature if it meets the specified criteria.
		if (description.strip() == "Fields" and area > 250000.0):
			activeLayer.selectFeature(i + 1) # uses the base-1 record number

	pluginHost.refreshMap(False)
else:
	pluginHost.showFeedback("The active layer is not a vector")

print("Done!")
