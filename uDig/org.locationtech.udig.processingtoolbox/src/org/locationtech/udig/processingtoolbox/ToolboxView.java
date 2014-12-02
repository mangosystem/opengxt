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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.ViewPart;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ProcessInformation.Category;
import org.locationtech.udig.processingtoolbox.ProcessInformation.SubCategory;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.ProcessExecutionDialog;
import org.locationtech.udig.processingtoolbox.internal.ui.SettingsDialog;
import org.locationtech.udig.processingtoolbox.tools.TextfileToPointDialog;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.opengis.feature.type.Name;

/**
 * ToolboxView
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */

public class ToolboxView extends ViewPart implements ISetSelectionTarget {
    protected static final Logger LOGGER = Logging.getLogger(ToolboxView.class);

    private static Boolean showLog = Boolean.TRUE;

    public static Boolean getShowLog() {
        return showLog;
    }

    public static void setShowLog(Boolean showLog) {
        ToolboxView.showLog = showLog;
    }

    /** Temporary workspace */
    private static String workspace = null;

    public static String getWorkspace() {
        if (ToolboxView.workspace == null) {
            String userhome = System.getProperty("user.home"); //$NON-NLS-1$
            String separator = System.getProperty("file.separator"); //$NON-NLS-1$
            File file = new File(userhome + separator + "gxt"); //$NON-NLS-1$

            if (file.exists() && file.isDirectory()) {
                ToolboxView.workspace = file.getPath();
            } else {
                if (file.mkdir()) {
                    ToolboxView.workspace = file.getPath();
                }
            }
        }
        return ToolboxView.workspace;
    }

    public static void setWorkspace(String workspace) {
        ToolboxView.workspace = workspace;
    }

    private TreeViewer viewer;

    private boolean loadAll = true;

    private IAction actionEnv;

    private String envIcon = "icons/applications-system-3.png"; //$NON-NLS-1$

    public ToolboxView() {
        // nothing to do
    }

    @Override
    public void dispose() {
        viewer = null;
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        // create tree viewer
        PatternFilter patternFilter = new PatternFilter();
        FilteredTree filter = new FilteredTree(parent, SWT.SINGLE | SWT.BORDER, patternFilter, true);

        viewer = filter.getViewer();
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setAutoExpandLevel(2);
        viewer.setInput(buildTree());

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                final TreeObject node = (TreeObject) selection.getFirstElement();
                if (TreeParent.class.isAssignableFrom(node.getClass())) {
                    return;
                }

                final Shell shell = Display.getCurrent().getActiveShell();
                final IMap map = ApplicationGIS.getActiveMap();

                Display.getCurrent().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (map != ApplicationGIS.NO_MAP) {
                            Dialog dialog = null;
                            if (node.getFactory() == null) {
                                if (node.getProcessName().getLocalPart()
                                        .equalsIgnoreCase("TextfileToPointDialog")) {
                                    dialog = new TextfileToPointDialog(shell, map);
                                }
                            } else {
                                dialog = new ProcessExecutionDialog(shell, map, node.getFactory(),
                                        node.getProcessName());
                            }

                            if (dialog != null) {
                                dialog.setBlockOnOpen(true);
                                dialog.open();
                            }
                        } else {
                            MessageDialog.openInformation(shell, "Confirm", "No Active Map"); //$NON-NLS-1$//$NON-NLS-2$
                        }
                    }
                });
            }
        });

        // action bar
        IToolBarManager toolbarMgr = getViewSite().getActionBars().getToolBarManager();
        toolbarMgr.add(getEnvironmentAction());
    }

    public IAction getEnvironmentAction() {
        if (actionEnv == null) {
            actionEnv = new Action() {
                public void run() {
                    SettingsDialog dialog = new SettingsDialog(Display.getCurrent()
                            .getActiveShell());
                    dialog.setBlockOnOpen(true);
                    if (dialog.open() == Window.OK) {
                        return;
                    }

                }
            };

            actionEnv.setEnabled(true);
            actionEnv.setText(Messages.SettingsDialog_title);
            actionEnv.setImageDescriptor(ToolboxPlugin.getImageDescriptor(envIcon));
            actionEnv.setDisabledImageDescriptor(ToolboxPlugin.getImageDescriptor(envIcon));
            actionEnv.setToolTipText(Messages.SettingsDialog_title);
        }
        return actionEnv;
    }

    private TreeObject buildTree() {
        // find all the process factories and print out their names
        TreeParent root = new TreeParent("Toolbox", null, null); //$NON-NLS-1$

        // 0. general operation dialog
        TreeParent generalTool = new TreeParent("General Tools", null, null);
        generalTool.addChild(new TreeObject(Messages.TextfileToPointDialog_title, null,
                new NameImpl(null, "TextfileToPointDialog")));
        root.addChild(generalTool);

        // 1. gt-process-spatialstatistics process
        InputStream inputStream = null;
        try {
            URL url = ToolboxPlugin.urlFromPlugin("ProcessFactory.xml"); //$NON-NLS-1$
            inputStream = url.openStream();
            ProcessInformation pi = ProcessInformation.decode(inputStream);

            ClassLoader classLoader = ToolboxView.class.getClassLoader();
            for (Category category : pi.getItems()) {
                TreeParent to1 = new TreeParent(category.Name, null, null);
                for (SubCategory subCat : category.getItems()) {
                    TreeParent to2 = new TreeParent(subCat.Name, null, null);

                    for (String facName : subCat.getItems()) {
                        try {
                            final Class<?> aClass = classLoader.loadClass(facName);
                            final ProcessFactory factory = (ProcessFactory) aClass.newInstance();

                            Iterator<Name> nameIter = factory.getNames().iterator();
                            while (nameIter.hasNext()) {
                                final Name processName = nameIter.next();
                                String name = factory.getTitle(processName).toString();
                                TreeObject to3 = new TreeObject(name, factory, processName);
                                to2.addChild(to3);
                            }
                        } catch (ClassNotFoundException e) {
                            LOGGER.log(Level.FINEST, e.getMessage(), e);
                        } catch (InstantiationException e) {
                            LOGGER.log(Level.FINEST, e.getMessage(), e);
                        } catch (IllegalAccessException e) {
                            LOGGER.log(Level.FINEST, e.getMessage(), e);
                        } catch (Throwable t) {
                            LOGGER.log(Level.FINEST, t.getMessage(), t);
                            t.printStackTrace();
                        }
                    }
                    to1.addChild(to2);
                }
                root.addChild(to1);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        // 2. GeoTools process
        if (loadAll) {
            TreeParent parent = new TreeParent(Messages.ToolboxView_GeoTools, null, null);
            Set<ProcessFactory> processFactories = Processors.getProcessFactories();
            Iterator<ProcessFactory> iterator = processFactories.iterator();
            while (iterator.hasNext()) {
                ProcessFactory factory = iterator.next();
                String category = getWindowTitle(factory.getTitle().toString());
                TreeParent to1 = new TreeParent(category, factory, null);
                Iterator<Name> nameIter = factory.getNames().iterator();
                while (nameIter.hasNext()) {
                    Name processName = nameIter.next();
                    String name = getWindowTitle(processName.getLocalPart());
                    TreeObject to2 = new TreeObject(name, factory, processName);
                    to1.addChild(to2);
                }
                parent.addChild(to1);
            }
            root.addChild(parent);
        }
        return root;
    }

    @SuppressWarnings("nls")
    private String getWindowTitle(String processName) {
        String windowTitle = Character.toUpperCase(processName.charAt(0))
                + processName.substring(1);
        if (!processName.contains("ST_")) {
            if (windowTitle.substring(2, 3).equalsIgnoreCase("_")) {
                windowTitle = windowTitle.substring(3);
            }

            StringBuffer sb = new StringBuffer();
            for (int index = 0; index < windowTitle.length(); index++) {
                char cat = windowTitle.charAt(index);
                if (index > 0 && Character.isUpperCase(cat)) {
                    sb.append(" ").append(cat);
                } else {
                    sb.append(cat);
                }
            }
            return sb.toString();
        } else {
            return windowTitle;
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void selectReveal(ISelection selection) {
        viewer.setSelection(selection);
    }

    static class TreeObject {

        private String name;

        private ProcessFactory factory;

        private Name processName;

        private TreeParent parent;

        public TreeObject(String name, ProcessFactory factory, Name processName) {
            this.name = name;
            this.factory = factory;
            this.processName = processName;
        }

        public String getName() {
            return name;
        }

        public ProcessFactory getFactory() {
            return factory;
        }

        public Name getProcessName() {
            return processName;
        }

        public void setParent(TreeParent parent) {
            this.parent = parent;
        }

        public TreeParent getParent() {
            return parent;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    static class TreeParent extends TreeObject {
        private ArrayList<TreeObject> children;

        public TreeParent(String name, ProcessFactory factory, Name processName) {
            super(name, factory, processName);
            children = new ArrayList<TreeObject>();
        }

        public void addChild(TreeObject child) {
            children.add(child);
            child.setParent(this);
        }

        public void removeChild(TreeObject child) {
            children.remove(child);
            child.setParent(null);
        }

        public TreeObject[] getChildren() {
            return children.toArray(new TreeObject[children.size()]);
        }

        public boolean hasChildren() {
            return children.size() > 0;
        }
    }

    static class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child) {
            if (child instanceof TreeObject) {
                return ((TreeObject) child).getParent();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof TreeParent) {
                return ((TreeParent) parent).getChildren();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeParent)
                return ((TreeParent) parent).hasChildren();
            return false;
        }
    }

    static class ViewLabelProvider extends LabelProvider {

        @Override
        public String getText(Object obj) {
            return obj.toString();
        }

        @Override
        public Image getImage(Object obj) {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
            if (obj instanceof TreeParent) {
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            }
            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
    }

}
