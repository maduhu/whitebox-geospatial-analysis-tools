/**
 * Copyright 2009 Rednaxela
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 
 *    2. This notice may not be removed or altered from any source
 *    distribution.
 */
 
package whitebox.structures;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
 
/**
 * An efficient well-optimized kd-tree
 * 
 * @author Rednaxela
 */
public abstract class KdTreeFloat<T> {
    // Static variables
    private static final int           bucketSize = 24;
 
    // All types
    private final int                  dimensions;
    private final KdTreeFloat<T>            parent;
 
    // Root only
    private final LinkedList<float[]> locationStack;
    private final Integer              sizeLimit;
 
    // Leaf only
    private float[][]                 locations;
    private Object[]                   data;
    private int                        locationCount;
 
    // Stem only
    private KdTreeFloat<T>                  left, right;
    private int                        splitDimension;
    private float                     splitValue;
 
    // Bounds
    private float[]                   minLimit, maxLimit;
    private boolean                    singularity;
 
    // Temporary
    private Status                     status;
 
    /**
     * Construct a KdTree with a given number of dimensions and a limit on
     * maxiumum size (after which it throws away old points)
     */
    private KdTreeFloat(int dimensions, Integer sizeLimit) {
        this.dimensions = dimensions;
 
        // Init as leaf
        this.locations = new float[bucketSize][];
        this.data = new Object[bucketSize];
        this.locationCount = 0;
        this.singularity = true;
 
        // Init as root
        this.parent = null;
        this.sizeLimit = sizeLimit;
        if (sizeLimit != null) {
            this.locationStack = new LinkedList<>();
        }
        else {
            this.locationStack = null;
        }
    }
 
    /**
     * Constructor for child nodes. Internal use only.
     */
    private KdTreeFloat(KdTreeFloat<T> parent, boolean right) {
        this.dimensions = parent.dimensions;
 
        // Init as leaf
        this.locations = new float[Math.max(bucketSize, parent.locationCount)][];
        this.data = new Object[Math.max(bucketSize, parent.locationCount)];
        this.locationCount = 0;
        this.singularity = true;
 
        // Init as non-root
        this.parent = parent;
        this.locationStack = null;
        this.sizeLimit = null;
    }
 
    /**
     * Get the number of points in the tree
     */
    public int size() {
        return locationCount;
    }
 
    
    /**
     * Add a point and associated value to the tree
     */
    public void addPoint(float[] location, T value) {
        try {
        KdTreeFloat<T> cursor = this;
 
        while (cursor.locations == null || cursor.locationCount >= cursor.locations.length) {
            if (cursor.locations != null) {
                cursor.splitDimension = cursor.findWidestAxis();
                cursor.splitValue = (cursor.minLimit[cursor.splitDimension] + cursor.maxLimit[cursor.splitDimension]) * 0.5f;
 
                // Never split on infinity or NaN
                if (cursor.splitValue == Float.POSITIVE_INFINITY) {
                    cursor.splitValue = Float.MAX_VALUE;
                }
                else if (cursor.splitValue == Float.NEGATIVE_INFINITY) {
                    cursor.splitValue = -Float.MAX_VALUE;
                }
                else if (Float.isNaN(cursor.splitValue)) {
                    cursor.splitValue = 0;
                }
 
                // Don't split node if it has no width in any axis. Float the
                // bucket size instead
                if (cursor.minLimit[cursor.splitDimension] == cursor.maxLimit[cursor.splitDimension]) {
                    float[][] newLocations = new float[cursor.locations.length * 2][];
                    System.arraycopy(cursor.locations, 0, newLocations, 0, cursor.locationCount);
                    cursor.locations = newLocations;
                    Object[] newData = new Object[newLocations.length];
                    System.arraycopy(cursor.data, 0, newData, 0, cursor.locationCount);
                    cursor.data = newData;
                    break;
                }
 
                // Don't let the split value be the same as the upper value as
                // can happen due to rounding errors!
                if (cursor.splitValue == cursor.maxLimit[cursor.splitDimension]) {
                    cursor.splitValue = cursor.minLimit[cursor.splitDimension];
                }
 
                // Create child leaves
                KdTreeFloat<T> left = new ChildNode(cursor, false);
                KdTreeFloat<T> right = new ChildNode(cursor, true);
 
                // Move locations into children
                for (int i = 0; i < cursor.locationCount; i++) {
                    float[] oldLocation = cursor.locations[i];
                    Object oldData = cursor.data[i];
                    if (oldLocation[cursor.splitDimension] > cursor.splitValue) {
                        // Right
                        right.locations[right.locationCount] = oldLocation;
                        right.data[right.locationCount] = oldData;
                        right.locationCount++;
                        right.extendBounds(oldLocation);
                    }
                    else {
                        // Left
                        left.locations[left.locationCount] = oldLocation;
                        left.data[left.locationCount] = oldData;
                        left.locationCount++;
                        left.extendBounds(oldLocation);
                    }
                }
 
                // Make into stem
                cursor.left = left;
                cursor.right = right;
                cursor.locations = null;
                cursor.data = null;
            }
 
            cursor.locationCount++;
            cursor.extendBounds(location);
 
            if (location[cursor.splitDimension] > cursor.splitValue) {
                cursor = cursor.right;
            }
            else {
                cursor = cursor.left;
            }
        }
 
        cursor.locations[cursor.locationCount] = location;
        cursor.data[cursor.locationCount] = value;
        cursor.locationCount++;
        cursor.extendBounds(location);
 
        if (this.sizeLimit != null) {
            this.locationStack.add(location);
            if (this.locationCount > this.sizeLimit) {
                this.removeOld();
            }
        }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
 
    /**
     * Extends the bounds of this node to include a new location
     */
    private final void extendBounds(float[] location) {
        if (minLimit == null) {
            minLimit = new float[dimensions];
            System.arraycopy(location, 0, minLimit, 0, dimensions);
            maxLimit = new float[dimensions];
            System.arraycopy(location, 0, maxLimit, 0, dimensions);
            return;
        }
 
        for (int i = 0; i < dimensions; i++) {
            if (Float.isNaN(location[i])) {
                minLimit[i] = Float.NaN;
                maxLimit[i] = Float.NaN;
                singularity = false;
            }
            else if (minLimit[i] > location[i]) {
                minLimit[i] = location[i];
                singularity = false;
            }
            else if (maxLimit[i] < location[i]) {
                maxLimit[i] = location[i];
                singularity = false;
            }
        }
    }
 
    /**
     * Find the widest axis of the bounds of this node
     */
    private final int findWidestAxis() {
        int widest = 0;
        float width = (maxLimit[0] - minLimit[0]) * getAxisWeightHint(0);
        if (Float.isNaN(width)) width = 0;
        for (int i = 1; i < dimensions; i++) {
            float nwidth = (maxLimit[i] - minLimit[i]) * getAxisWeightHint(i);
            if (Float.isNaN(nwidth)) nwidth = 0;
            if (nwidth > width) {
                widest = i;
                width = nwidth;
            }
        }
        return widest;
    }
 
    /**
     * Remove the oldest value from the tree. Note: This cannot trim the bounds
     * of nodes, nor empty nodes, and thus you can't expect it to perfectly
     * preserve the speed of the tree as you keep adding.
     */
    private void removeOld() {
        float[] location = this.locationStack.removeFirst();
        KdTreeFloat<T> cursor = this;
 
        // Find the node where the point is
        while (cursor.locations == null) {
            if (location[cursor.splitDimension] > cursor.splitValue) {
                cursor = cursor.right;
            }
            else {
                cursor = cursor.left;
            }
        }
 
        for (int i = 0; i < cursor.locationCount; i++) {
            if (cursor.locations[i] == location) {
                System.arraycopy(cursor.locations, i + 1, cursor.locations, i, cursor.locationCount - i - 1);
                cursor.locations[cursor.locationCount-1] = null;
                System.arraycopy(cursor.data, i + 1, cursor.data, i, cursor.locationCount - i - 1);
                cursor.data[cursor.locationCount-1] = null;
                do {
                    cursor.locationCount--;
                    cursor = cursor.parent;
                } while (cursor.parent != null);
                return;
            }
        }
        // If we got here... we couldn't find the value to remove. Weird...
    }
 
    /**
     * Enumeration representing the status of a node during the running
     */
    private static enum Status {
        NONE, LEFTVISITED, RIGHTVISITED, ALLVISITED
    }
 
    /**
     * Stores a distance and value to output
     */
    public static class Entry<T> {
        public final float distance;
        public final T      value;
 
        private Entry(float distance, T value) {
            this.distance = distance;
            this.value = value;
        }
        
    }
    
    /**
     * finds all points within 'range' distance to 'location'
     */
    @SuppressWarnings("unchecked")
    public List<Entry<T>> neighborsWithinRange(float[] location, float range) {
        KdTreeFloat<T> cursor = this;
        cursor.status = Status.NONE;
        range = range * range; 
        ArrayList<Entry<T>> results = new ArrayList<Entry<T>>();
        
        do {
            if (cursor.status == Status.ALLVISITED) {
                // At a fully visited part. Move up the tree
                cursor = cursor.parent;
                continue;
            }
 
            if (cursor.status == Status.NONE && cursor.locations != null) {
                // At a leaf. Use the data.
                if (cursor.locationCount > 0) {
                    if (cursor.singularity) {
                        float dist = pointDist(cursor.locations[0], location);
                        if (dist <= range) {
                            for (int i = 0; i < cursor.locationCount; i++) {
                                results.add(new Entry<T>(dist, (T)cursor.data[i]));
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < cursor.locationCount; i++) {
                            float dist = pointDist(cursor.locations[i], location);
                            if (dist <= range) {
                                results.add(new Entry<T>(dist, (T)cursor.data[i]));
                            }
                        }
                    }
                }
 
                if (cursor.parent == null) {
                    break;
                }
                cursor = cursor.parent;
                continue;
            }
 
            // Going to descend
            KdTreeFloat<T> nextCursor = null;
            if (cursor.status == Status.NONE) {
                // At a fresh node, descend the most probably useful direction
                if (location[cursor.splitDimension] > cursor.splitValue) {
                    // Descend right
                    nextCursor = cursor.right;
                    cursor.status = Status.RIGHTVISITED;
                }
                else {
                    // Descend left;
                    nextCursor = cursor.left;
                    cursor.status = Status.LEFTVISITED;
                }
            }
            else if (cursor.status == Status.LEFTVISITED) {
                // Left node visited, descend right.
                nextCursor = cursor.right;
                cursor.status = Status.ALLVISITED;
            }
            else if (cursor.status == Status.RIGHTVISITED) {
                // Right node visited, descend left.
                nextCursor = cursor.left;
                cursor.status = Status.ALLVISITED;
            }
 
            // Check if it's worth descending. Assume it is if it's sibling has
            // not been visited yet.
            if (cursor.status == Status.ALLVISITED) {
                if (nextCursor.locationCount == 0
                        || (!nextCursor.singularity && pointRegionDist(location, nextCursor.minLimit,
                                nextCursor.maxLimit) > range)) {
                    continue;
                }
            }
 
            // Descend down the tree
            cursor = nextCursor;
            cursor.status = Status.NONE;
        } while (cursor.parent != null || cursor.status != Status.ALLVISITED);
 
        return results;
    }
    
    /**
     * Calculates the nearest 'count' points to 'location'
     */
    @SuppressWarnings("unchecked")
    public List<Entry<T>> nearestNeighbor(float[] location, int count, boolean sequentialSorting) {
        KdTreeFloat<T> cursor = this;
        cursor.status = Status.NONE;
        float range = Float.POSITIVE_INFINITY;
        ResultHeap resultHeap = new ResultHeap(count);
 
        do {
            if (cursor.status == Status.ALLVISITED) {
                // At a fully visited part. Move up the tree
                cursor = cursor.parent;
                continue;
            }
 
            if (cursor.status == Status.NONE && cursor.locations != null) {
                // At a leaf. Use the data.
                if (cursor.locationCount > 0) {
                    if (cursor.singularity) {
                        float dist = pointDist(cursor.locations[0], location);
                        if (dist <= range) {
                            for (int i = 0; i < cursor.locationCount; i++) {
                                resultHeap.addValue(dist, cursor.data[i]);
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < cursor.locationCount; i++) {
                            float dist = pointDist(cursor.locations[i], location);
                            resultHeap.addValue(dist, cursor.data[i]);
                        }
                    }
                    range = resultHeap.getMaxDist();
                }
 
                if (cursor.parent == null) {
                    break;
                }
                cursor = cursor.parent;
                continue;
            }
 
            // Going to descend
            KdTreeFloat<T> nextCursor = null;
            if (cursor.status == Status.NONE) {
                // At a fresh node, descend the most probably useful direction
                if (location[cursor.splitDimension] > cursor.splitValue) {
                    // Descend right
                    nextCursor = cursor.right;
                    cursor.status = Status.RIGHTVISITED;
                }
                else {
                    // Descend left;
                    nextCursor = cursor.left;
                    cursor.status = Status.LEFTVISITED;
                }
            }
            else if (cursor.status == Status.LEFTVISITED) {
                // Left node visited, descend right.
                nextCursor = cursor.right;
                cursor.status = Status.ALLVISITED;
            }
            else if (cursor.status == Status.RIGHTVISITED) {
                // Right node visited, descend left.
                nextCursor = cursor.left;
                cursor.status = Status.ALLVISITED;
            }
 
            // Check if it's worth descending. Assume it is if it's sibling has
            // not been visited yet.
            if (cursor.status == Status.ALLVISITED) {
                if (nextCursor.locationCount == 0
                        || (!nextCursor.singularity && pointRegionDist(location, nextCursor.minLimit,
                                nextCursor.maxLimit) > range)) {
                    continue;
                }
            }
 
            // Descend down the tree
            cursor = nextCursor;
            cursor.status = Status.NONE;
        } while (cursor.parent != null || cursor.status != Status.ALLVISITED);
 
        ArrayList<Entry<T>> results = new ArrayList<Entry<T>>(resultHeap.values);
        if (sequentialSorting) {
            while (resultHeap.values > 0) {
                resultHeap.removeLargest();
                results.add(new Entry<T>(resultHeap.removedDist, (T)resultHeap.removedData));
            }
        }
        else {
            for (int i = 0; i < resultHeap.values; i++) {
                results.add(new Entry<T>(resultHeap.distance[i], (T)resultHeap.data[i]));
            }
        }
 
        return results;
    }
 
    // Override in subclasses
    protected abstract float pointDist(float[] p1, float[] p2);
 
    protected abstract float pointRegionDist(float[] point, float[] min, float[] max);
 
    protected float getAxisWeightHint(int i) {
        return 1.0f;
    }
 
    /**
     * Internal class for child nodes
     */
    private class ChildNode extends KdTreeFloat<T> {
        private ChildNode(KdTreeFloat<T> parent, boolean right) {
            super(parent, right);
        }
 
        // Distance measurements are always called from the root node
        @Override
        protected float pointDist(float[] p1, float[] p2) {
            throw new IllegalStateException();
        }
 
        @Override
        protected float pointRegionDist(float[] point, float[] min, float[] max) {
            throw new IllegalStateException();
        }
    }
 
    /**
     * Class for tree with Weighted Squared Euclidean distancing
     */
    public static class WeightedSqrEuclid<T> extends KdTreeFloat<T> {
        private float[] weights;
 
        public WeightedSqrEuclid(int dimensions, Integer sizeLimit) {
            super(dimensions, sizeLimit);
            this.weights = new float[dimensions];
            Arrays.fill(this.weights, 1.0f);
        }
 
        public void setWeights(float[] weights) {
            this.weights = weights;
        }
 
        @Override
        protected float getAxisWeightHint(int i) {
            return weights[i];
        }
 
        @Override
        protected float pointDist(float[] p1, float[] p2) {
            float d = 0;
 
            for (int i = 0; i < p1.length; i++) {
                float diff = (p1[i] - p2[i]) * weights[i];
                if (!Float.isNaN(diff)) {
                    d += diff * diff;
                }
            }
 
            return d;
        }
 
        @Override
        protected float pointRegionDist(float[] point, float[] min, float[] max) {
            float d = 0;
 
            for (int i = 0; i < point.length; i++) {
                float diff = 0;
                if (point[i] > max[i]) {
                    diff = (point[i] - max[i]) * weights[i];
                }
                else if (point[i] < min[i]) {
                    diff = (point[i] - min[i]) * weights[i];
                }
 
                if (!Float.isNaN(diff)) {
                    d += diff * diff;
                }
            }
 
            return d;
        }
    }
 
    /**
     * Class for tree with Unweighted Squared Euclidean distancing
     */
    public static class SqrEuclid<T> extends KdTreeFloat<T> {
        public SqrEuclid(int dimensions, Integer sizeLimit) {
            super(dimensions, sizeLimit);
        }
 
        @Override
        protected float pointDist(float[] p1, float[] p2) {
            float d = 0;
 
            for (int i = 0; i < p1.length; i++) {
                float diff = (p1[i] - p2[i]);
                if (!Float.isNaN(diff)) {
                    d += diff * diff;
                }
            }
 
            return d;
        }
 
        @Override
        protected float pointRegionDist(float[] point, float[] min, float[] max) {
            float d = 0;
 
            for (int i = 0; i < point.length; i++) {
                float diff = 0;
                if (point[i] > max[i]) {
                    diff = (point[i] - max[i]);
                }
                else if (point[i] < min[i]) {
                    diff = (point[i] - min[i]);
                }
 
                if (!Float.isNaN(diff)) {
                    d += diff * diff;
                }
            }
 
            return d;
        }
    }
 
    /**
     * Class for tree with Weighted Manhattan distancing
     */
    public static class WeightedManhattan<T> extends KdTreeFloat<T> {
        private float[] weights;
 
        public WeightedManhattan(int dimensions, Integer sizeLimit) {
            super(dimensions, sizeLimit);
            this.weights = new float[dimensions];
            Arrays.fill(this.weights, 1.0f);
        }
 
        public void setWeights(float[] weights) {
            this.weights = weights;
        }
 
        @Override
        protected float getAxisWeightHint(int i) {
            return weights[i];
        }
 
        @Override
        protected float pointDist(float[] p1, float[] p2) {
            float d = 0;
 
            for (int i = 0; i < p1.length; i++) {
                float diff = (p1[i] - p2[i]);
                if (!Float.isNaN(diff)) {
                    d += ((diff < 0) ? -diff : diff) * weights[i];
                }
            }
 
            return d;
        }
 
        @Override
        protected float pointRegionDist(float[] point, float[] min, float[] max) {
            float d = 0;
 
            for (int i = 0; i < point.length; i++) {
                float diff = 0;
                if (point[i] > max[i]) {
                    diff = (point[i] - max[i]);
                }
                else if (point[i] < min[i]) {
                    diff = (min[i] - point[i]);
                }
 
                if (!Float.isNaN(diff)) {
                    d += diff * weights[i];
                }
            }
 
            return d;
        }
    }
 
    /**
     * Class for tree with Manhattan distancing
     */
    public static class Manhattan<T> extends KdTreeFloat<T> {
        public Manhattan(int dimensions, Integer sizeLimit) {
            super(dimensions, sizeLimit);
        }
 
        @Override
        protected float pointDist(float[] p1, float[] p2) {
            float d = 0;
 
            for (int i = 0; i < p1.length; i++) {
                float diff = (p1[i] - p2[i]);
                if (!Float.isNaN(diff)) {
                    d += (diff < 0) ? -diff : diff;
                }
            }
 
            return d;
        }
 
        @Override
        protected float pointRegionDist(float[] point, float[] min, float[] max) {
            float d = 0;
 
            for (int i = 0; i < point.length; i++) {
                float diff = 0;
                if (point[i] > max[i]) {
                    diff = (point[i] - max[i]);
                }
                else if (point[i] < min[i]) {
                    diff = (min[i] - point[i]);
                }
 
                if (!Float.isNaN(diff)) {
                    d += diff;
                }
            }
 
            return d;
        }
    }
 
    /**
     * Class for tracking up to 'size' closest values
     */
    private static class ResultHeap {
        private final Object[] data;
        private final float[] distance;
        private final int      size;
        private int            values;
        public Object          removedData;
        public float          removedDist;
 
        public ResultHeap(int size) {
            this.data = new Object[size];
            this.distance = new float[size];
            this.size = size;
            this.values = 0;
        }
 
        public void addValue(float dist, Object value) {
            // If there is still room in the heap
            if (values < size) {
                // Insert new value at the end
                data[values] = value;
                distance[values] = dist;
                upHeapify(values);
                values++;
            }
            // If there is no room left in the heap, and the new entry is lower
            // than the max entry
            else if (dist < distance[0]) {
                // Replace the max entry with the new entry
                data[0] = value;
                distance[0] = dist;
                downHeapify(0);
            }
        }
 
        public void removeLargest() {
            if (values == 0) {
                throw new IllegalStateException();
            }
 
            removedData = data[0];
            removedDist = distance[0];
            values--;
            data[0] = data[values];
            distance[0] = distance[values];
            downHeapify(0);
        }
 
        private void upHeapify(int c) {
            for (int p = (c - 1) / 2; c != 0 && distance[c] > distance[p]; c = p, p = (c - 1) / 2) {
                Object pData = data[p];
                float pDist = distance[p];
                data[p] = data[c];
                distance[p] = distance[c];
                data[c] = pData;
                distance[c] = pDist;
            }
        }
 
        private void downHeapify(int p) {
            for (int c = p * 2 + 1; c < values; p = c, c = p * 2 + 1) {
                if (c + 1 < values && distance[c] < distance[c + 1]) {
                    c++;
                }
                if (distance[p] < distance[c]) {
                    // Swap the points
                    Object pData = data[p];
                    float pDist = distance[p];
                    data[p] = data[c];
                    distance[p] = distance[c];
                    data[c] = pData;
                    distance[c] = pDist;
                }
                else {
                    break;
                }
            }
        }
 
        public float getMaxDist() {
            if (values < size) {
                return Float.POSITIVE_INFINITY;
            }
            return distance[0];
        }
    }
}