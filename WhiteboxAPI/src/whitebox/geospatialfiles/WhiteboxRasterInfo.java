package whitebox.geospatialfiles;
 

/**
 * The WhiteboxRasterInfo is a lightweight alternative to the WhiteboxRaster class and can be
 * used to manipulate Whitebox GAT raster header files (.dep). If you need to read or write data
 * to the grid cells contained in the raster, you should use the WhiteboxRaster class instead.
 * @author John Lindsay
 */
public class WhiteboxRasterInfo extends WhiteboxRasterBase {


    // ************************
    // Fields
    // ************************
    private boolean isDirty = false;

    // ************************
    // Constructors
    // ************************


    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param HeaderFile The name of the WhiteboxRaster header file.
     */
    public WhiteboxRasterInfo(String HeaderFile)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess("rw");
        readHeaderFile();
        
    }
    
    /**
     * Used to perform closing functionality when a whiteboxRaster is no longer needed.
     */
    @Override
    public void close() {
        if (saveChanges) {
            if (isDirty) {
                writeHeaderFile();
            }
        }
    }

}
