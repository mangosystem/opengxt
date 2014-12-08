/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.geotools.process.spatialstatistics.storage.DataStoreFactory;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.internal.ui.UiPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.ui.preferences.PreferenceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("restriction")
public class ToolboxPlugin extends AbstractUIPlugin {
    protected static final Logger LOGGER = Logging.getLogger(ToolboxPlugin.class);

    // The plug-in ID
    public static final String PLUGIN_ID = "org.locationtech.udig.processingtoolbox"; //$NON-NLS-1$

    // The shared instance
    private static ToolboxPlugin plugin;

    /**
     * The constructor
     */
    public ToolboxPlugin() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // setup default charset for shapefile
        DataStoreFactory.DEFAULT_CHARSET = defaultCharset();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static ToolboxPlugin getDefault() {
        return plugin;
    }

    // Console
    private DateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] "); //$NON-NLS-1$

    private PrintStream internal;

    private MessageConsole console;

    public void print(Object message) {
        if (console == null) {
            console = new MessageConsole(Messages.ToolboxView_Title, null);

            IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
            manager.addConsoles(new IConsole[] { console });
            manager.showConsoleView(console);

            MessageConsoleStream internalStream = console.newMessageStream();
            internal = new PrintStream(internalStream, true);
        }

        console.activate();
        internal.println(dateFormat.format(new java.util.Date()) + message.toString());
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path.
     * 
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(plugin.getBundle().getSymbolicName(),
                path);
    }

    public static URL urlFromPlugin(String filePath) {
        // if the bundle is not ready then there is no image
        Bundle bundle = Platform.getBundle(plugin.getBundle().getSymbolicName());
        if (!BundleUtility.isReady(bundle)) {
            return null;
        }

        // look for the image (this will check both the plugin and fragment folders
        URL fullPathString = BundleUtility.find(bundle, filePath);
        if (fullPathString == null) {
            try {
                fullPathString = new URL(filePath);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        return fullPathString;
    }

    public static String defaultCharset() {
        return UiPlugin.getDefault().getPreferenceStore()
                .getString(PreferenceConstants.P_DEFAULT_CHARSET);
    }

    public static void log(Object message) {
        if (ToolboxView.getShowLog()) {
            getDefault().print(message);
        }
    }

}
