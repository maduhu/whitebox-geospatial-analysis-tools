package whitebox.interfaces;

/**
 *
 * @author johnlindsay
 */
public interface DialogComponent {
    public String[] getArgsDescriptors();
    public String getComponentName();
    public boolean getOptionalStatus();
    public String getValue();
    public boolean setArgs(String[] args);
}
