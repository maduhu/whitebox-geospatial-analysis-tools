import whitebox.geospatialfiles.WhiteboxRaster
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType
import java.util.Collections.*

def wd = pluginHost.getWorkingDirectory()
def inputFile = wd + "Vermont DEM.dep"
//def inputFile = wd + "bare earth filled.dep"
//def inputFile = wd + "band4.dep"
def outputFile = wd + "tmp4.dep"

WhiteboxRaster input = new WhiteboxRaster(inputFile, "r")
int rows = input.getNumberRows()
int cols = input.getNumberColumns()
double nodata = input.getNoDataValue()
		
WhiteboxRaster output = new WhiteboxRaster(outputFile, "rw", 
	inputFile, DataType.FLOAT, nodata)

double[] data, data2, data3

for (int row in 0..(rows - 1)) {
	data = input.getRowValues(row)
	Arrays.sort(data)
	output.setRowValues(row, data)
}

for (int col in 0..(cols - 1)) {
	data = output.getColumnValues(col)
	Arrays.sort(data)
	for (int row in 0..(rows - 1)) {
		output.setValue(row, col, data[row])
	}
}

output.flush()

int n = 0
while (n < 4) {
for (int row = 0; row < (rows - 2); row += 2) {
	data = output.getRowValues(row)
	data2 = output.getRowValues(row + 1)
	data3 = new double[cols * 2]
	for (int i in 0..(cols - 1)) {
		data3[i] = data[i]
	}
	for (int i in 0..(cols - 1)) {
		data3[cols + i] = data2[i]
	}
	
	Arrays.sort(data3)

	for (int col in 0..(cols - 1)) {
		output.setValue(row, col, data3[col])
		output.setValue(row + 1, col, data3[cols + col])
		
	}

}

output.flush()

for (int col in 0..(cols - 1)) {
	data = output.getColumnValues(col)
	Arrays.sort(data)
	for (int row in 0..(rows - 1)) {
		output.setValue(row, col, data[row])
	}
}

output.flush()
n++
println n
//    // check to see if the user has requested a cancellation
if (pluginHost.isRequestForOperationCancelSet()) {
	pluginHost.showFeedback("Operation cancelled")
	break
}
}


output.close()

pluginHost.returnData(outputFile)
