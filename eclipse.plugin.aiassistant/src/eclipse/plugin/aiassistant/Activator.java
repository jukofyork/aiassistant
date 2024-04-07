package eclipse.plugin.aiassistant;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    /**
     * The plug-in ID
     */
    public static final String PLUGIN_ID = "eclipse.plugin.aiassistant";

    /**
     * The shared instance
     */
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /**
     * This method is called upon plug-in activation
     * @param context the bundle context
     * @throws Exception if an error occurs
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /**
     * This method is called when the plug-in is stopped
     * @param context the bundle context
     * @throws Exception if an error occurs
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

}