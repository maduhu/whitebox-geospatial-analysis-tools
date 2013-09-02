/*
 * Modified by John Lindsay 2013
 */
package jopensurf;
/*
 This work was derived from Chris Evan's opensurf project and re-licensed as the
 3 clause BSD license with permission of the original author. Thank you Chris! 

 Copyright (c) 2010, Andrew Stromberg
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither Andrew Stromberg nor the
 names of its contributors may be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL Andrew Stromberg BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.parallel.*;
import whitebox.structures.KdTreeFloat;

/**
 * A class to calculate the upright or free oriented interest points of an image
 * lazily (will not calculate until you ask for them)
 *
 * @author astromberg
 *
 */
public class Surf implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HESSIAN_OCTAVES = 5;
    private static final int HESSIAN_INIT_SAMPLE = 2;
    private static final float HESSIAN_THRESHOLD = 0.0085F;
    private static final float HESSIAN_BALANCE_VALUE = 0.81F;
//    private transient BufferedImage mOriginalImage;
    private FastHessian mHessian;
    private List<SURFInterestPoint> mFreeOrientedPoints;
    private List<SURFInterestPoint> mUprightPoints;
    private List<SURFInterestPoint> mDescriptorFreeInterestPoints;
    private int mNumOctaves = HESSIAN_OCTAVES;
    private float mThreshold = HESSIAN_THRESHOLD;
    private float mBalanceValue = HESSIAN_BALANCE_VALUE;
    private IntegralImage mIntegralImage;

    public Surf(BufferedImage image) {
        this(image, HESSIAN_BALANCE_VALUE, HESSIAN_THRESHOLD, HESSIAN_OCTAVES);
    }

    public Surf(BufferedImage image, float balanceValue, float threshold, int octaves) {
//        mOriginalImage = image;
        mNumOctaves = octaves;
        mBalanceValue = balanceValue;
        mThreshold = threshold;

        //Calculate the integral image
        mIntegralImage = new IntegralImage(image);

        //Calculate the fast hessian
        mHessian = new FastHessian(mIntegralImage, mNumOctaves, HESSIAN_INIT_SAMPLE, mThreshold, mBalanceValue);

        //Calculate the descriptor and orientation free interest points
        mDescriptorFreeInterestPoints = mHessian.getIPoints();
    }

    public Surf(WhiteboxRaster image) {
        this(image, HESSIAN_BALANCE_VALUE, HESSIAN_THRESHOLD, HESSIAN_OCTAVES);
    }

    public Surf(WhiteboxRaster image, float balanceValue, float threshold, int octaves) {
//        mOriginalImage = image;
        mNumOctaves = octaves;
        mBalanceValue = balanceValue;
        mThreshold = threshold;

        //Calculate the integral image
        mIntegralImage = new IntegralImage(image);

        //Calculate the fast hessian
        mHessian = new FastHessian(mIntegralImage, mNumOctaves, HESSIAN_INIT_SAMPLE, mThreshold, mBalanceValue);

        //Calculate the descriptor and orientation free interest points
        mDescriptorFreeInterestPoints = mHessian.getIPoints();
    }

    public List<SURFInterestPoint> getUprightInterestPoints() {
        return getPoints(true);
    }

    public List<SURFInterestPoint> getFreeOrientedInterestPoints() {
        return getPoints(false);
    }

    public int getNumberOfPoints() {
        return mDescriptorFreeInterestPoints.size();
    }

    private List<SURFInterestPoint> getPoints(final boolean upright) {
//        List<SURFInterestPoint> points = upright ? mUprightPoints : mFreeOrientedPoints;
        if ((upright ? mUprightPoints : mFreeOrientedPoints) == null) {
            final List<SURFInterestPoint> points = getDescriptorFreeInterestPoints();
            // cache for next time through
            if (upright) {
                mUprightPoints = points;
            } else {
                mFreeOrientedPoints = points;
            }

            final int numPoints = points.size();
            System.out.println("There are " + numPoints + " points to match");
            Parallel.ForEach(points, new CallableLoopBody<SURFInterestPoint>() {
                @Override
                public Boolean call(SURFInterestPoint point) {
                    try {
                        getOrientation(point);
                        getMDescriptor(point, upright);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            return points;
        }
        return (upright ? mUprightPoints : mFreeOrientedPoints);
    }

    private List<SURFInterestPoint> getDescriptorFreeInterestPoints() {
        List<SURFInterestPoint> points = new ArrayList<>(mDescriptorFreeInterestPoints.size());
        for (SURFInterestPoint point : mDescriptorFreeInterestPoints) {
            try {
                points.add((SURFInterestPoint) point.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return points;
    }

    private float haarX(int row, int column, int s) {
        return ImageTransformUtils.BoxIntegral(mIntegralImage, row - s / 2, column, s, s / 2)
                - ImageTransformUtils.BoxIntegral(mIntegralImage, row - s / 2, column - s / 2, s, s / 2);
    }

    private float haarY(int row, int column, int s) {
        return ImageTransformUtils.BoxIntegral(mIntegralImage, row, column - s / 2, s / 2, s)
                - ImageTransformUtils.BoxIntegral(mIntegralImage, row - s / 2, column - s / 2, s / 2, s);
    }

    private void getOrientation(SURFInterestPoint input) {
        double gauss;
        float scale = input.getScale();

        int s = (int) Math.round(scale);
        int r = (int) Math.round(input.getY());
        int c = (int) Math.round(input.getX());

        List<Double> xHaarResponses = new ArrayList<>();
        List<Double> yHaarResponses = new ArrayList<>();
        List<Double> angles = new ArrayList<>();
        //System.out.println("s = " + s + ", r = " + r + ", c = " + c + ", scale = " + scale);

        //calculate haar responses for points within radius of 6*scale
        for (int i = -6; i <= 6; ++i) {
            for (int j = -6; j <= 6; ++j) {
                if (i * i + j * j < 36) {
                    gauss = GaussianConstants.Gauss25[Math.abs(i)][Math.abs(j)];
                    //System.out.println("i = " + i + ", j = " + j + ", gauss = " + gauss);
                    double xHaarResponse = gauss * haarX(r + j * s, c + i * s, 4 * s);
                    double yHaarResponse = gauss * haarY(r + j * s, c + i * s, 4 * s);
                    xHaarResponses.add(xHaarResponse);
                    yHaarResponses.add(yHaarResponse);
                    angles.add(getAngle(xHaarResponse, yHaarResponse));
                    //System.out.format("gauss = %.8f, haarX = %.8f, haarY = %.8f, getAngle(x,y) = %.8f",gauss,xHaarResponse,yHaarResponse,getAngle(xHaarResponse,yHaarResponse));
                    //System.out.println();
                }
            }
        }

        // calculate the dominant direction
        float sumX = 0, sumY = 0;
        float ang1, ang2, ang;
        float max = 0;
        float orientation = 0;

        // loop slides pi/3 window around feature point
        for (ang1 = 0; ang1 < 2 * Math.PI; ang1 += 0.15f) {
            ang2 = (float) (ang1 + Math.PI / 3.0f > 2 * Math.PI ? ang1 - 5.0f * Math.PI / 3.0f : ang1 + Math.PI / 3.0f);
            sumX = sumY = 0;
            for (int k = 0; k < angles.size(); k++) {
                ang = angles.get(k).floatValue();

                if (ang1 < ang2 && ang1 < ang && ang < ang2) {
                    sumX += xHaarResponses.get(k).floatValue();
                    sumY += yHaarResponses.get(k).floatValue();
                } else if (ang2 < ang1 && ((ang > 0 && ang < ang2) || (ang > ang1 && ang < 2 * Math.PI))) {
                    sumX += xHaarResponses.get(k).floatValue();
                    sumY += yHaarResponses.get(k).floatValue();
                }
            }
            // if the vector produced from this window is longer than all
            // previous vectors then this forms the new dominant direction
            if (sumX * sumX + sumY * sumY > max) {
                // store largest orientation
                max = sumX * sumX + sumY * sumY;
                orientation = (float) getAngle(sumX, sumY);
            }
        }
        input.setOrientation(orientation);
        //System.out.println("orientation = " + orientation);
        //return input;
    }

    private void getMDescriptor(SURFInterestPoint point, boolean upright) {
        int y, x, count = 0;
        int sample_x, sample_y;
        double scale, dx, dy, mdx, mdy, co = 1F, si = 0F;
        float desc[] = new float[64];
        double gauss_s1 = 0.0D, gauss_s2 = 0.0D, xs = 0.0D, ys = 0.0D;
        double rx = 0.0D, ry = 0.0D, rrx = 0.0D, rry = 0.0D, len = 0.0D;
        int i = 0, ix = 0, j = 0, jx = 0;

        float cx = -0.5f, cy = 0.0f; //Subregion centers for the 4x4 gaussian weighting

        scale = point.getScale();
        x = Math.round(point.getX());
        y = Math.round(point.getY());
        //System.out.println("x = " + point.getX() + ", y = " + point.getY());
        //System.out.println("x = " + x + ", y = " + y);
        if (!upright) {
            co = Math.cos(point.getOrientation());
            si = Math.sin(point.getOrientation());
        }
        //System.out.println("co = " + co + ", sin = " + si);
        i = -8;
        //Calculate descriptor for this interest point
        //Area of size 24 s x 24 s
        //***********************************************
        while (i < 12) {
            j = -8;
            i = i - 4;

            cx += 1.0F;
            cy = -0.5F;

            while (j < 12) {
                dx = dy = mdx = mdy = 0.0F;
                cy += 1.0F;

                j = j - 4;

                ix = i + 5;
                jx = j + 5;

                xs = Math.round(x + (-jx * scale * si + ix * scale * co));
                ys = Math.round(y + (jx * scale * co + ix * scale * si));

                for (int k = i; k < i + 9; ++k) {
                    for (int l = j; l < j + 9; ++l) {
                        //Get coords of sample point on the rotated axis
                        sample_x = (int) Math.round(x + (-1D * l * scale * si + k * scale * co));
                        sample_y = (int) Math.round(y + (l * scale * co + k * scale * si));

                        //Get the gaussian weighted x and y responses
                        gauss_s1 = gaussian(xs - sample_x, ys - sample_y, 2.5F * scale);

                        rx = haarX(sample_y, sample_x, (int) (2 * Math.round(scale)));
                        ry = haarY(sample_y, sample_x, (int) (2 * Math.round(scale)));

                        //Get the gaussian weighted x and y responses on rotated axis
                        rrx = gauss_s1 * (-rx * si + ry * co);
                        rry = gauss_s1 * (rx * co + ry * si);

                        dx += rrx;
                        dy += rry;

                        mdx += Math.abs(rrx);
                        mdy += Math.abs(rry);
                    }
                }

                //Add the values to the descriptor vector
                gauss_s2 = gaussian(cx - 2.0f, cy - 2.0f, 1.5f);

                //Casting from a double to a float, might be a terrible idea
                //but doubles are expensive
                desc[count++] = (float) (dx * gauss_s2);
                desc[count++] = (float) (dy * gauss_s2);

                desc[count++] = (float) (mdx * gauss_s2);
                desc[count++] = (float) (mdy * gauss_s2);

                //Accumulate length for vector normalisation
                len += (dx * dx + dy * dy + mdx * mdx + mdy * mdy) * (gauss_s2 * gauss_s2);

                j += 9;
            }
            i += 9;
        }

        len = Math.sqrt(len);

        for (i = 0; i < 64; i++) {
            desc[i] /= len;
        }

        point.setDescriptor(desc);
    }

    private double getAngle(double xHaarResponse, double yHaarResponse) {
        if (xHaarResponse >= 0 && yHaarResponse >= 0) {
            return Math.atan(yHaarResponse / xHaarResponse);
        }

        if (xHaarResponse < 0 && yHaarResponse >= 0) {
            return Math.PI - Math.atan(-yHaarResponse / xHaarResponse);
        }

        if (xHaarResponse < 0 && yHaarResponse < 0) {
            return Math.PI + Math.atan(yHaarResponse / xHaarResponse);
        }

        if (xHaarResponse >= 0 && yHaarResponse < 0) {
            return 2 * Math.PI - Math.atan(-yHaarResponse / xHaarResponse);
        }

        return 0;
    }

    public Map<SURFInterestPoint, SURFInterestPoint> getMatchingPoints(Surf other,
            double matchThreshold, boolean upright) {
        if (matchThreshold < 0.05) {
            matchThreshold = 0.05;
        } else if (matchThreshold > 0.99) {
            matchThreshold = 0.99;
        }

        final List<SURFInterestPoint> myPoints = upright ? getUprightInterestPoints() : getFreeOrientedInterestPoints();
        final List<SURFInterestPoint> otherPoints = upright ? other.getUprightInterestPoints() : other.getFreeOrientedInterestPoints();

        final Map<SURFInterestPoint, SURFInterestPoint> matchingPoints = new HashMap<>((int) (myPoints.size() * 0.1));

        /* I've parallelized the point matching. The simple linear solution was
         * far too slow. I tried a kd tree but it also yielded poor performance 
         * due to its high dimensionality (64). There are more performant 
         * approximate solutions, but this is the best compromise for an exact
         * solution.
         */

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        ArrayList<Callable<InterestPointPair>> tasks = new ArrayList<>();

//        for (SURFInterestPoint a : myPoints) {
//            tasks.add(new InterestPointMatcher(a, otherPoints, matchThreshold));
//        }

        for (int i = 0; i < myPoints.size(); i++) {
            tasks.add(new InterestPointMatcher(i, myPoints, otherPoints, matchThreshold));
        }

        //ArrayList<InterestPointPair> possibleMatches = new ArrayList<>();

        try {
            List<Future<InterestPointPair>> futures = executor.invokeAll(tasks);
            for (Future<InterestPointPair> fut : futures) {
                InterestPointPair pair = fut.get();
                if (pair != null) {
                    matchingPoints.put(pair.getPoint1(), pair.getPoint2());
                    //possibleMatches.add(pair);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e.getMessage());
        } finally {
            executor.shutdown();
        }
//        
//        Collections.sort(possibleMatches);
//        
//        int maxReturnNumber = (possibleMatches.size() < 100) ? possibleMatches.size() : 100;
//        for (int i = 0; i < maxReturnNumber; i++) {
//            InterestPointPair pair = possibleMatches.get(i);
//            matchingPoints.put(pair.getPoint1(), pair.getPoint2());
//        }

        return matchingPoints;
    }

    public String getStringRepresentation(boolean freeOriented) {
        StringBuilder buffer = new StringBuilder();
        for (SURFInterestPoint point : freeOriented ? getFreeOrientedInterestPoints() : getUprightInterestPoints()) {
            for (double val : point.getDescriptor()) {
                buffer.append(Double.doubleToLongBits(val)).append(",");
            }
        }
        buffer.substring(0, buffer.length() - 1);
        return buffer.toString();
    }

    public void setStringRepresentation(String str) {
    }

//	private double gaussian(int x, int y, double sig){
//		return (1.0f/(2.0f*Math.PI*sig*sig)) * Math.exp( -(x*x+y*y)/(2.0f*sig*sig));
//	}
    private double gaussian(double x, double y, double sig) {
        return (1.0f / (2.0f * Math.PI * sig * sig)) * Math.exp(-(x * x + y * y) / (2.0f * sig * sig));
    }

//	public static void main(String[] args){
//		try {
//			BufferedImage image = ImageIO.read(new File("/home/astromberg/test_images/cover.jpg"));
//			Surf board = new Surf(image);
//			List<InterestPoint> matchingPoints = board.getMatchingPoints(board,false);
//			System.out.println("between the first image and itself there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//			
//			image = ImageIO.read(new File("/home/astromberg/test_images/photo.jpg"));
//			Surf board2 = new Surf(image);
//			matchingPoints = board.getMatchingPoints(board2,false);
//			System.out.println("between the first and second image there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//			
//			image = ImageIO.read(new File("/data/work/OpenSURF/Images/img2.jpg"));
//			Surf board3 = new Surf(image);
//			matchingPoints = board.getMatchingPoints(board3,false);
//			System.out.println("between the first and third image there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//
//			image = ImageIO.read(new File("/home/astromberg/test_images/photo2.jpg"));
//			Surf board4 = new Surf(image);
//			matchingPoints = board.getMatchingPoints(board4,false);
//			System.out.println("between the first and third image there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//
//			image = ImageIO.read(new File("/home/astromberg/test_images/photo3.jpg"));
//			Surf board5 = new Surf(image);
//			matchingPoints = board.getMatchingPoints(board5,false);
//			System.out.println("between the first and third image there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//			
//			image = ImageIO.read(new File("/home/astromberg/test_images/photo4.jpg"));
//			Surf board6 = new Surf(image);
//			matchingPoints = board.getMatchingPoints(board6,false);
//			System.out.println("between the first and third image there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//			
//			image = ImageIO.read(new File("/home/astromberg/test_images/cover2.jpg"));
//			Surf board7 = new Surf(image);
//			matchingPoints = board.getMatchingPoints(board7,false);
//			System.out.println("between the first and third image there are " + matchingPoints.size() + " of " + board.getFreeOrientedInterestPoints().size() + " possible matching points");
//			
//			matchingPoints = board6.getMatchingPoints(board7,false);
//			System.out.println("between the first and third image there are " + matchingPoints.size() + " of " + board6.getFreeOrientedInterestPoints().size() + " possible matching points");
//		} catch (Exception e){
//			e.printStackTrace();
//		}
//	}
    public boolean isEquivalentTo(Surf surf) {
        List<SURFInterestPoint> pointsA = surf.getFreeOrientedInterestPoints();
        List<SURFInterestPoint> pointsB = getFreeOrientedInterestPoints();
        if (pointsA.size() != pointsB.size()) {
            return false;
        }
        for (int i = 0; i < pointsA.size(); i++) {
            SURFInterestPoint pointA = pointsA.get(i);
            SURFInterestPoint pointB = pointsB.get(i);
            if (!pointA.isEquivalentTo(pointB)) {
                return false;
            }
        }
        return true;
    }

    public static void saveToFile(Surf surf, String file) {
        try {
            ObjectOutputStream stream = new ObjectOutputStream(
                    new FileOutputStream(file));
            stream.writeObject(surf);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Surf readFromFile(String location) {
        File file = new File(location);
        if (file != null && file.exists()) {
            try {
                ObjectInputStream str = new ObjectInputStream(
                        new FileInputStream(file));
                return (Surf) str.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (mFreeOrientedPoints == null) {
            getFreeOrientedInterestPoints();
        }
        if (mUprightPoints == null) {
            getUprightInterestPoints();
        }
        out.defaultWriteObject();
    }

    public static void main(String args[]) {
        try {
            BufferedImage image = ImageIO.read(new File("/Users/johnlindsay/NetBeansProjects/JOpenSurf/trunk/example/graffiti.png"));
            Surf board = new Surf(image);
            saveToFile(board, "/Users/johnlindsay/NetBeansProjects/JOpenSurf/trunk/example/surf_test.bin");
            Surf boarder = readFromFile("/Users/johnlindsay/NetBeansProjects/JOpenSurf/trunk/example/surf_test.bin");
            List<SURFInterestPoint> points = boarder.getFreeOrientedInterestPoints();
            System.out.println("Found " + points.size() + " interest points");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
