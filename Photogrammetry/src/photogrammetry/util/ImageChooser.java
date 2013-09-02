package photogrammetry.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import photogrammetry.util.model.Feature;
import photogrammetry.util.model.Image;
import photogrammetry.util.model.SceneView;

/**
 * Class for choosing image pairs for reconstruction using multiple views.
 *
 * @author Piotr
 */
public class ImageChooser {

    public List<Pair<SceneView, SceneView>> pairs;

    public ImageChooser(List<SceneView> views) {
        double n = getAverageNumberOfCommonFeatures(views);
        pairs = imagesWithCommonFeatures(n, views);
    }

    /**
     * Returns list of pairs of images with common features
     *
     * @param n average number of common features
     * @param imgList list of all images
     * @return List of images with common features
     */
    private List<Pair<SceneView, SceneView>> imagesWithCommonFeatures(double n,
            List<SceneView> imgList) {
        List<Pair<SceneView, SceneView>> images = new ArrayList<>();

        for (int i = 0; i < imgList.size(); i++) {
            SceneView imgI = imgList.get(i);
            for (int j = 0; j < imgList.size(); j++) {
                if (j > i) {
                    SceneView imgJ = imgList.get(j);
                    Collection<Feature> f = imgJ.getCommonFeatures(imgI);
                    if (f.size() >= n) {
                        images.add(new Pair<>(imgI, imgJ));
                    }
                }
            }
        }
        return images;
    }

    /**
     * Computes average number of common features for all images
     *
     * @param imgList the list of all images
     * @return average number of common features
     */
    private double getAverageNumberOfCommonFeatures(List<SceneView> imgList) {
        int numberOfFeatures = 0;
        for (int i = 0; i < imgList.size(); i++) {
            SceneView imgI = imgList.get(i);
            for (int j = 0; j < imgList.size(); j++) {
                if (j > i) {
                    SceneView imgJ = imgList.get(j);
                    Collection<Feature> f = imgJ.getCommonFeatures(imgI);
                    numberOfFeatures += f.size();
                }
            }
        }
        return numberOfFeatures / (imgList.size() / 2.0 * (imgList.size() - 1));
    }

    /**
     * Return the selected pairs of images.
     *
     * @return list of pairs of images.
     */
    public List<Pair<SceneView, SceneView>> getPairs() {
        return pairs;
    }
}
