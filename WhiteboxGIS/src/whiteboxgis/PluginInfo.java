package whiteboxgis;

import java.util.Date;

/**
 *
 * @author johnlindsay
 */
public class PluginInfo implements Comparable<PluginInfo> {
    private String name = null;
    private String descriptiveName = null;
    private int numTimesUsed = 0;
    private Date lastUsed = new Date(Long.MIN_VALUE);
    private byte sortMode = 0;
    
    public static final byte SORT_MODE_USAGE = 0;
    public static final byte SORT_MODE_RECENT = 1;
    public static final byte SORT_MODE_NAMES = 2;
    
    public PluginInfo(String name, byte sortMode) {
        this.name = name;
        this.sortMode = sortMode;
    }
    
    public PluginInfo(String name, String descriptiveName, byte sortMode) {
        this.name = name;
        this.descriptiveName = descriptiveName;
        this.sortMode = sortMode;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescriptiveName() {
        return descriptiveName;
    }
    
    public void setDescriptiveName(String descriptiveName) {
        this.descriptiveName = descriptiveName;
    }
    
    public int getNumTimesUsed() {
        return numTimesUsed;
    }
    
    public void setNumTimesUsed(int numTimesUsed) {
        this.numTimesUsed = numTimesUsed;
    }
    
    public void incrementNumTimesUsed() {
        numTimesUsed++;
    }
    
    public Date getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public void setLastUsedToNow() {
        lastUsed = new Date();
    }
    
    public byte getSortMode() {
        return sortMode;
    }
    
    public void setSortMode(byte sortMode) {
        this.sortMode = sortMode;
    }
    
    public int compareTo(PluginInfo other) {
        final int BEFORE = 1;
        final int EQUAL = 0;
        final int AFTER = -1;
        
        // make sure that you're comparing the two PluginInfos based on the same criterion.
        if (this.sortMode != other.getSortMode()) {
            other.setSortMode(this.sortMode);
        }
        
        if (this.sortMode == SORT_MODE_USAGE) {
            if (this.numTimesUsed < other.numTimesUsed) {
                return BEFORE;
            } else if (this.numTimesUsed > other.numTimesUsed) {
                return AFTER;
            }
            
        } else if (this.sortMode == SORT_MODE_RECENT) {
            if (this.lastUsed.compareTo(other.lastUsed) < 0) {
                return BEFORE;
            } else if (this.lastUsed.compareTo(other.lastUsed) > 0) {
                return AFTER;
            }
        }
        
        // else compare them based on their names.
        String str1 = this.descriptiveName.toLowerCase();
        String str2 = other.descriptiveName.toLowerCase();
        
        if (str1.compareTo(str2) < 0) {
            return AFTER;
        } else if (str1.compareTo(str2) > 0) {
            return BEFORE;
        }
//        
//        if (this.descriptiveName.compareTo(other.descriptiveName) < 0) {
//            return AFTER;
//        } else if (this.descriptiveName.compareTo(other.descriptiveName) > 0) {
//            return BEFORE;
//        }

        return EQUAL;
    }
    
}
