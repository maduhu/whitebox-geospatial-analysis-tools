package whitebox.geospatialfiles;

public class CreatePixelsContinuous {

    private int[] pixelData;
    private int[] mPaletteData;
    private int mStartRow;
    private int mStartCol;
    private int mEndRow;
    private int mEndCol;
    private WhiteboxRasterInfo mWB;
    private int resolutionFactor;
    private double noDataValue;
    private int numPaletteEntriesLessOne;
    private double range;
    private double minVal;
    private double maxVal;
    private double mGamma;
    private double[] data;
    private int mBackgroundColour;
    private int imageWidth;
    private int imageHeight;
    
    public CreatePixelsContinuous(WhiteboxRasterInfo wri, int startRow, 
            int endRow, int startCol, int endCol, int resFactor, double minValue, 
            double maxValue, double gamma, int[] palette, int backgroundColour) {
        imageHeight = 0;
        imageWidth = 0;
        for (int row = startRow; row < endRow; row += resFactor) {
            imageHeight++;
        }
        for (int col = startCol; col < endCol; col += resFactor) {
            imageWidth++;
        }
        int numCells = imageHeight * imageWidth;

        pixelData = new int[numCells];
        data = new double[numCells];
        
        mPaletteData = palette;
        numPaletteEntriesLessOne = palette.length - 1;
        mStartRow = startRow;
        mStartCol = startCol;
        mEndCol = endCol;
        mEndRow = endRow;
        mWB = wri;
        noDataValue = mWB.getNoDataValue();
        resolutionFactor = resFactor;
        minVal = minValue;
        maxVal = maxValue;
        range = maxVal - minVal;
        mGamma = gamma;
        mBackgroundColour = backgroundColour;
    }
    
    public void createPixels() {
        int i = 0;
        for (int row = mStartRow; row < mEndRow; row += resolutionFactor) {
            double[] rawData = mWB.getRowValues(row);
            double value;
            int entryNum;
            int j;
            for (int col = mStartCol; col < mEndCol; col += resolutionFactor) {
                value = rawData[col]; //sourceData.getValue(row, col);
                j = row * imageHeight + (col - mStartCol) / resolutionFactor;
                if (value != noDataValue) {
                    entryNum = (int) (Math.pow(((value - minVal) / range), mGamma) * numPaletteEntriesLessOne);
                    if (entryNum < 0) {
                        entryNum = 0;
                    }
                    if (entryNum > numPaletteEntriesLessOne) {
                        entryNum = numPaletteEntriesLessOne;
                    }
                    pixelData[i] = mPaletteData[entryNum];
                } else {
                    pixelData[i] = mBackgroundColour;
                }
                data[i] = value;
                i++;
            }
        }
    }
    
    public int[] getPixels() {
        return pixelData;
    }
}