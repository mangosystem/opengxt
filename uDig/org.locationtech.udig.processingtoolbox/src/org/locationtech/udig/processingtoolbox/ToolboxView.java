/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.ProcessExecutionDialog;
import org.locationtech.udig.processingtoolbox.internal.ui.SettingsDialog;
import org.locationtech.udig.processingtoolbox.tools.AmoebaWizard;
import org.locationtech.udig.processingtoolbox.tools.AmoebaWizardDialog;
import org.locationtech.udig.processingtoolbox.tools.BoxPlotDialog;
import org.locationtech.udig.processingtoolbox.tools.BubbleChartDialog;
import org.locationtech.udig.processingtoolbox.tools.ExcelToPointDialog;
import org.locationtech.udig.processingtoolbox.tools.ExportStyleDialog;
import org.locationtech.udig.processingtoolbox.tools.FieldCalculatorDialog;
import org.locationtech.udig.processingtoolbox.tools.FormatConversionDialog;
import org.locationtech.udig.processingtoolbox.tools.GeometryToFeaturesDialog;
import org.locationtech.udig.processingtoolbox.tools.HistogramDialog;
import org.locationtech.udig.processingtoolbox.tools.MergeFeaturesDialog;
import org.locationtech.udig.processingtoolbox.tools.MoranScatterPlotDialog;
import org.locationtech.udig.processingtoolbox.tools.ScatterPlotDialog;
import org.locationtech.udig.processingtoolbox.tools.SpatialWeightsMatrixDialog;
import org.locationtech.udig.processingtoolbox.tools.SplitByAttributesDialog;
import org.locationtech.udig.processingtoolbox.tools.SplitByFeaturesDialog;
import org.locationtech.udig.processingtoolbox.tools.TextfileToPointDialog;
import org.locationtech.udig.processingtoolbox.tools.ThematicMapDialog;
import org.locationtech.udig.processingtoolbox.tools.ThematicMapRasterDialog;
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

    private static Boolean showLog = Boolean.FALSE;

    private static Boolean selectedOnly = Boolean.TRUE;

    private static Boolean useDefaultStyle = Boolean.TRUE;

    private static Boolean loadGeoToolsProcess = Boolean.TRUE;

    private static Boolean addLayerAutomatically = Boolean.TRUE;

    private static Boolean mandatoryParameterOnly = Boolean.FALSE;

    private TreeViewer viewer;

    private IAction actionEnv;

    private String envIcon = "icons/applications-system-3.png"; //$NON-NLS-1$

    private static String workspace = null;

    public ToolboxView() {
        // nothing to do
    }

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

    public static Boolean getShowLog() {
        return ToolboxView.showLog;
    }

    public static void setShowLog(Boolean showLog) {
        ToolboxView.showLog = showLog;
    }

    public static Boolean isLoadGeoToolsProcess() {
        return ToolboxView.loadGeoToolsProcess;
    }

    public static void setLoadGeoToolsProcess(Boolean loadGeoToolsProcess) {
        ToolboxView.loadGeoToolsProcess = loadGeoToolsProcess;
    }

    public static Boolean getSelectedOnly() {
        return selectedOnly;
    }

    public static void setSelectedOnly(Boolean selectedOnly) {
        ToolboxView.selectedOnly = selectedOnly;
    }

    public static Boolean getUseDefaultStyle() {
        return useDefaultStyle;
    }

    public static void setUseDefaultStyle(Boolean useDefaultStyle) {
        ToolboxView.useDefaultStyle = useDefaultStyle;
    }

    public static Boolean getAddLayerAutomatically() {
        return addLayerAutomatically;
    }

    public static void setAddLayerAutomatically(Boolean addLayerAutomatically) {
        ToolboxView.addLayerAutomatically = addLayerAutomatically;
    }

    public static Boolean getMandatoryParameterOnly() {
        return mandatoryParameterOnly;
    }

    public static void setMandatoryParameterOnly(Boolean mandatoryParameterOnly) {
        ToolboxView.mandatoryParameterOnly = mandatoryParameterOnly;
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
                    @SuppressWarnings("nls")
                    @Override
                    public void run() {
                        if (map != ApplicationGIS.NO_MAP) {
                            Dialog dialog = null;
                            if (node.getFactory() == null) {
                                String nodeName = node.getProcessName().getLocalPart();
                                if (nodeName.equalsIgnoreCase("BoxPlotDialog")) {
                                    dialog = new BoxPlotDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("BubbleChartDialog")) {
                                    dialog = new BubbleChartDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("FieldCalculatorDialog")) {
                                    dialog = new FieldCalculatorDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("FormatConversionDialog")) {
                                    dialog = new FormatConversionDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("HistogramDialog")) {
                                    dialog = new HistogramDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("MoranScatterPlotDialog")) {
                                    dialog = new MoranScatterPlotDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("ScatterPlotDialog")) {
                                    dialog = new ScatterPlotDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("TextfileToPointDialog")) {
                                    dialog = new TextfileToPointDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("ExcelToPointDialog")) {
                                    dialog = new ExcelToPointDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("ThematicMapDialog")) {
                                    dialog = new ThematicMapDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("ThematicMapRasterDialog")) {
                                    dialog = new ThematicMapRasterDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("GeometryToFeaturesDialog")) {
                                    dialog = new GeometryToFeaturesDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("SpatialWeightsMatrixDialog")) {
                                    dialog = new SpatialWeightsMatrixDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("MergeFeaturesDialog")) {
                                    dialog = new MergeFeaturesDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("SplitByAttributesDialog")) {
                                    dialog = new SplitByAttributesDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("SplitByFeaturesDialog")) {
                                    dialog = new SplitByFeaturesDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("ExportStyleDialog")) {
                                    dialog = new ExportStyleDialog(shell, map);
                                } else if (nodeName.equalsIgnoreCase("AmoebaWizardDialog")) {
                                    dialog = new AmoebaWizardDialog(shell, new AmoebaWizard(map));
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
                            MessageDialog.openInformation(shell, Messages.ToolboxView_Title,
                                    Messages.ToolboxView_NoActiveMap);
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
                @Override
                public void run() {
                    Shell parentShell = Display.getCurrent().getActiveShell();
                    SettingsDialog dialog = new SettingsDialog(parentShell);
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
        // find all the process factories and print out their name
        TreeParent root = new TreeParent(Messages.ToolboxView_Title, null, null);

        // 1. build general tools
        buildGeneralTools(root);

        // 2. build spatial statistics tools
        buildStatisticsTools(root);

        // 3. build gridcoverage tools
        buildGridCoverageTools(root);

        // 4. build GeoTools process
        if (loadGeoToolsProcess) {
            buildGeoTools(root);
        }

        return root;
    }

    @SuppressWarnings("nls")
    private void buildStatisticsTools(TreeParent root) {
        TreeParent ssTools = new TreeParent(Messages.ToolboxView_SpatialStatistics, null, null);
        root.addChild(ssTools);

        // Descriptive Statistics
        TreeParent desTools = new TreeParent(Messages.ToolboxView_DescriptiveStatistics, null, null);
        ssTools.addChild(desTools);
        buildTool(desTools, "org.geotools.process.spatialstatistics.CountFeaturesProcessFactory");
        buildTool(desTools, "org.geotools.process.spatialstatistics.AreaProcessFactory");
        buildTool(desTools, "org.geotools.process.spatialstatistics.StatisticsFeaturesProcessFactory");
        buildTool(desTools, "org.geotools.process.spatialstatistics.StandardizedScoresProcessFactory");
        buildTool(desTools, "org.geotools.process.spatialstatistics.FocalLQProcessFactory");

        // Point Pattern Analysis
        TreeParent patternTools = new TreeParent(Messages.ToolboxView_PointPattern, null, null);
        ssTools.addChild(patternTools);
        buildTool(patternTools, "org.geotools.process.spatialstatistics.NearestNeighborProcessFactory");
        buildTool(patternTools, "org.geotools.process.spatialstatistics.QuadratAnalysisProcessFactory");
        buildTool(patternTools, "org.geotools.process.spatialstatistics.KNearestNeighborMapProcessFactory");
        buildTool(patternTools, "org.geotools.process.spatialstatistics.KMeansClusteringProcessFactory");
        
        // Global Spatial Autocorrelation
        TreeParent autoTools = new TreeParent(Messages.ToolboxView_Autocorrelation, null, null);
        ssTools.addChild(autoTools);
        buildTool(autoTools, "org.geotools.process.spatialstatistics.JoinCountStatisticsProcessFactory");
        buildTool(autoTools, "org.geotools.process.spatialstatistics.GlobalMoransIProcessFactory");
        buildTool(autoTools, "org.geotools.process.spatialstatistics.GlobalGStatisticsProcessFactory");
        buildTool(autoTools, "org.geotools.process.spatialstatistics.GlobalGearysCProcessFactory");
        buildTool(autoTools, "org.geotools.process.spatialstatistics.GlobalLeesSProcessFactory");
        buildTool(autoTools, "org.geotools.process.spatialstatistics.GlobalLeesLProcessFactory");
        buildTool(autoTools, "org.geotools.process.spatialstatistics.GlobalRogersonRProcessFactory");

        // Local Spatial Autocorrelation: Spatial Cluster and Outlier
        TreeParent clusterTools = new TreeParent(Messages.ToolboxView_Cluster, null, null);
        ssTools.addChild(clusterTools);
        buildTool(clusterTools, "org.geotools.process.spatialstatistics.LocalMoransIProcessFactory");
        buildTool(clusterTools, Messages.MoranScatterPlotDialog_title, "MoranScatterPlotDialog");
        buildTool(clusterTools, "org.geotools.process.spatialstatistics.LocalGStatisticsProcessFactory");
        buildTool(clusterTools, "org.geotools.process.spatialstatistics.LocalGearysCProcessFactory");
        buildTool(clusterTools, "org.geotools.process.spatialstatistics.LocalLeesSProcessFactory");
        buildTool(clusterTools, "org.geotools.process.spatialstatistics.LocalLeesLProcessFactory");
        buildTool(clusterTools, "org.geotools.process.spatialstatistics.LocalRogersonRProcessFactory");
        // buildTool(clusterTools, Messages.AmoebaWizardDialog_title, "AmoebaWizardDialog");
        
        // Spatial Relationships
        TreeParent relTools = new TreeParent(Messages.ToolboxView_Relationsips, null, null);
        ssTools.addChild(relTools);
        buildTool(relTools, Messages.SpatialWeightsMatrixDialog_title, "SpatialWeightsMatrixDialog");
        buildTool(relTools, "org.geotools.process.spatialstatistics.PearsonCorrelationProcessFactory");
        buildTool(relTools, "org.geotools.process.spatialstatistics.OLSProcessFactory");

        // Spatial Distribution
        TreeParent distributionTools = new TreeParent(Messages.ToolboxView_Distribution, null, null);
        ssTools.addChild(distributionTools);
        buildTool(distributionTools, "org.geotools.process.spatialstatistics.MeanCenterProcessFactory");
        buildTool(distributionTools, "org.geotools.process.spatialstatistics.MedianCenterProcessFactory");
        buildTool(distributionTools, "org.geotools.process.spatialstatistics.CentralFeatureProcessFactory");
        buildTool(distributionTools, "org.geotools.process.spatialstatistics.SDProcessFactory");
        buildTool(distributionTools, "org.geotools.process.spatialstatistics.SDEProcessFactory");
        buildTool(distributionTools, "org.geotools.process.spatialstatistics.DirectionalMeanProcessFactory");
    }

    private void buildTool(TreeParent ssTools, String className) {
        try {
            Class<?> factoryClass = ToolboxView.class.getClassLoader().loadClass(className);
            ProcessFactory factory = (ProcessFactory) factoryClass.newInstance();

            Iterator<Name> nameIter = factory.getNames().iterator();
            while (nameIter.hasNext()) {
                Name processName = nameIter.next();
                String name = factory.getTitle(processName).toString();
                TreeObject node = new TreeObject(name, factory, processName);
                ssTools.addChild(node);
            }
        } catch (ClassNotFoundException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (InstantiationException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (IllegalAccessException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (Throwable t) {
            ToolboxPlugin.log(t.getMessage());
        }
    }

    private void buildGeoTools(TreeParent root) {
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

    @SuppressWarnings("nls")
    private void buildGeneralTools(TreeParent root) {
        // 0. general operation dialog
        TreeParent generalTool = new TreeParent(Messages.ToolboxView_GeneralTools, null, null);
        root.addChild(generalTool);
        
        buildTool(generalTool, Messages.ThematicMapDialog_title, "ThematicMapDialog");
        buildTool(generalTool, Messages.ThematicMapRasterDialog_title, "ThematicMapRasterDialog");
        // buildTool(generalTool, Messages.FieldCalculatorDialog_title, "FieldCalculatorDialog");

        // import
        TreeParent importTool = new TreeParent(Messages.ToolboxView_Import, null, null);
        generalTool.addChild(importTool);
        buildTool(importTool, Messages.TextfileToPointDialog_title, "TextfileToPointDialog");
        buildTool(importTool, Messages.ExcelToPointDialog_title, "ExcelToPointDialog");
        buildTool(importTool, Messages.GeometryToFeaturesDialog_title, "GeometryToFeaturesDialog");
        buildTool(importTool, "org.geotools.process.spatialstatistics.GeometryToFeaturesProcessFactory");

        // export
        TreeParent exportTool = new TreeParent(Messages.ToolboxView_Export, null, null);
        generalTool.addChild(exportTool);
        buildTool(exportTool, Messages.ExportStyleDialog_title, "ExportStyleDialog");
        buildTool(exportTool, Messages.FormatConversionDialog_title, "FormatConversionDialog");
        buildTool(exportTool, Messages.SplitByAttributesDialog_title, "SplitByAttributesDialog");
        buildTool(exportTool, Messages.SplitByFeaturesDialog_title, "SplitByFeaturesDialog");
        buildTool(exportTool, Messages.MergeFeaturesDialog_title, "MergeFeaturesDialog");

        // Creation
        TreeParent createTool = new TreeParent(Messages.ToolboxView_DataCreation, null, null);
        generalTool.addChild(createTool);
        buildTool(createTool, "org.geotools.process.spatialstatistics.RandomPointsProcessFactory");
        buildTool(createTool, "org.geotools.process.spatialstatistics.RandomPointsPerFeaturesProcessFactory");
        buildTool(createTool, "org.geotools.process.spatialstatistics.FishnetCountProcessFactory");
        buildTool(createTool, "org.geotools.process.spatialstatistics.FishnetSizeProcessFactory");
        buildTool(createTool, "org.geotools.process.spatialstatistics.HexagonProcessFactory");
        buildTool(createTool, "org.geotools.process.spatialstatistics.TriangularGridProcessFactory");
        buildTool(createTool, "org.geotools.process.spatialstatistics.CircularGridProcessFactory");

        // Calculation
        TreeParent calcTool = new TreeParent(Messages.ToolboxView_Calculation, null, null);
        generalTool.addChild(calcTool);
        buildTool(calcTool, "org.geotools.process.spatialstatistics.CountFeaturesProcessFactory");
        buildTool(calcTool, "org.geotools.process.spatialstatistics.AreaProcessFactory");
        buildTool(calcTool, "org.geotools.process.spatialstatistics.CalculateFieldProcessFactory");
        buildTool(calcTool, "org.geotools.process.spatialstatistics.CalculateAreaProcessFactory");
        buildTool(calcTool, "org.geotools.process.spatialstatistics.CalculateLengthProcessFactory");
        buildTool(calcTool, "org.geotools.process.spatialstatistics.CalculateXYCoordinateProcessFactory");
        buildTool(calcTool, "org.geotools.process.spatialstatistics.ExtractValuesToPointsProcessFactory");

        // Graph
        TreeParent graphTool = new TreeParent(Messages.ToolboxView_Graph, null, null);
        generalTool.addChild(graphTool);
        buildTool(graphTool, Messages.HistogramDialog_title, "HistogramDialog");
        buildTool(graphTool, Messages.BoxPlotDialog_title, "BoxPlotDialog");
        buildTool(graphTool, Messages.BubbleChartDialog_title, "BubbleChartDialog");
        buildTool(graphTool, Messages.ScatterPlotDialog_title, "ScatterPlotDialog");
        buildTool(graphTool, Messages.MoranScatterPlotDialog_title, "MoranScatterPlotDialog");

        // Proximity
        TreeParent proxTool = new TreeParent(Messages.ToolboxView_Proximity, null, null);
        generalTool.addChild(proxTool);
        buildTool(proxTool, "org.geotools.process.spatialstatistics.BufferExpressionProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.SingleSidedBufferProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.WedgeBufferProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.MultipleRingBufferProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.PolarGridsFromFeaturesProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.PolarGridsFromGeometryProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.ThiessenPolygonProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.DelaunayTriangulationProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.HubLinesByIDProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.HubLinesByDistanceProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.NearProcessFactory");
        buildTool(proxTool, "org.geotools.process.spatialstatistics.NearestNeighborCountProcessFactory");

        // Aggregation
        TreeParent aggreTool = new TreeParent(Messages.ToolboxView_Aggregation, null, null);
        generalTool.addChild(aggreTool);
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.CollectEventsProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.BufferStatisticsProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.DissolveProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.UnionPolygonProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.AttributeJoinProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.RingMapProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.WindRoseMapProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.SpatialClumpMapProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.RectangularBinningProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.CircularBinningProcessFactory");
        buildTool(aggreTool, "org.geotools.process.spatialstatistics.HexagonalBinningProcessFactory");

        // Extract
        TreeParent extractTool = new TreeParent(Messages.ToolboxView_Extract, null, null);
        generalTool.addChild(extractTool);
        buildTool(extractTool, "org.geotools.process.spatialstatistics.SelectFeaturesProcessFactory");
        buildTool(extractTool, "org.geotools.process.spatialstatistics.ClipWithGeometryProcessFactory");
        buildTool(extractTool, "org.geotools.process.spatialstatistics.ClipWithFeaturesProcessFactory");

        // Overlay
        TreeParent overlayTool = new TreeParent(Messages.ToolboxView_Overlays, null, null);
        generalTool.addChild(overlayTool);
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.UnionProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.IntersectProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.DifferenceProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.SymDifferenceProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.IdentityProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.UpdateProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.IntersectionPointsProcessFactory");

        buildTool(overlayTool, "org.geotools.process.spatialstatistics.SpatialJoinProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.PointStatisticsProcessFactory");
        buildTool(overlayTool, "org.geotools.process.spatialstatistics.SumLineLengthProcessFactory");

        // Editing
        TreeParent editTool = new TreeParent(Messages.ToolboxView_Editing, null, null);
        generalTool.addChild(editTool);
        buildTool(editTool, "org.geotools.process.spatialstatistics.RepairGeometryProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.OffsetFeaturesProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.SnapPointsToLinesProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.SimplifyProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.DensifyProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.FlipLineProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.ExtendLineProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.TrimLineProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.RemoveHolesProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.RemovePartsProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.EliminateProcessFactory");
        buildTool(editTool, "org.geotools.process.spatialstatistics.DeleteDuplicatesProcessFactory");

        // Utilities
        TreeParent utilTool = new TreeParent(Messages.ToolboxView_Utilities, null, null);
        generalTool.addChild(utilTool);
        buildTool(utilTool, "org.geotools.process.spatialstatistics.MultipartToSinglepartProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.SinglepartToMultipartProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToPointProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToLineProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToPolygonProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.VerticesToPointsProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.PointsAlongLinesProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.PointsToLineProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.SplitLineByDistanceProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.SplitLineAtVerticesProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.SplitLineAtPointProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureEnvelopeToPolygonProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToConvexHullProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToMinimumBoundingCircleProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToMinimumRectangleProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FeatureToOctagonalEnvelopeProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.FlowMapProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.MergeFeaturesProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.ReprojectProcessFactory");
    }

    @SuppressWarnings("nls")
    private void buildGridCoverageTools(TreeParent root) {
        // 0. general operation dialog
        TreeParent generalTool = new TreeParent(Messages.ToolboxView_RasterTools, null, null);
        root.addChild(generalTool);

        // Descriptive Statistics
        TreeParent desTools = new TreeParent(Messages.ToolboxView_DescriptiveStatistics, null, null);
        generalTool.addChild(desTools);
        buildTool(desTools, "org.geotools.process.spatialstatistics.StatisticsGridCoverageProcessFactory");
        buildTool(desTools, "org.geotools.process.spatialstatistics.HistogramGridCoverageProcessFactory");

        // Conversion
        TreeParent conversionTool = new TreeParent(Messages.ToolboxView_Conversion, null, null);
        generalTool.addChild(conversionTool);
        buildTool(conversionTool, "org.geotools.process.spatialstatistics.FeaturesToRasterProcessFactory");
        buildTool(conversionTool, "org.geotools.process.spatialstatistics.PointsToRasterProcessFactory");
        buildTool(conversionTool, "org.geotools.process.spatialstatistics.GeometryToRasterProcessFactory");
        buildTool(conversionTool, "org.geotools.process.spatialstatistics.RasterToPointProcessFactory");
        buildTool(conversionTool, "org.geotools.process.spatialstatistics.RasterToPolygonProcessFactory");

        // Reclass
        TreeParent reclassTool = new TreeParent(Messages.ToolboxView_Reclass, null, null);
        generalTool.addChild(reclassTool);
        buildTool(reclassTool, "org.geotools.process.spatialstatistics.RasterReclassProcessFactory");

        // Extraction
        TreeParent extractionTool = new TreeParent(Messages.ToolboxView_Extract, null, null);
        generalTool.addChild(extractionTool);
        buildTool(extractionTool, "org.geotools.process.spatialstatistics.RasterExtractionProcessFactory");
        buildTool(extractionTool, "org.geotools.process.spatialstatistics.RasterClipByExtentProcessFactory");
        buildTool(extractionTool, "org.geotools.process.spatialstatistics.RasterClipByGeometryProcessFactory");
        buildTool(extractionTool, "org.geotools.process.spatialstatistics.RasterClipByCircleProcessFactory");
        buildTool(extractionTool, "org.geotools.process.spatialstatistics.RasterClipByFeaturesProcessFactory");

        // Conditional Tool
        TreeParent conditionalTool = new TreeParent(Messages.ToolboxView_Conditional, null, null);
        generalTool.addChild(conditionalTool);
        buildTool(conditionalTool, "org.geotools.process.spatialstatistics.RasterConProcessFactory");
        buildTool(conditionalTool, "org.geotools.process.spatialstatistics.RasterSetNullProcessFactory");

        // Distance
        TreeParent distanceTool = new TreeParent(Messages.ToolboxView_RasterDistance, null, null);
        generalTool.addChild(distanceTool);
        buildTool(distanceTool, "org.geotools.process.spatialstatistics.EuclideanDistanceProcessFactory");
        
        // Math
        TreeParent mathTool = new TreeParent(Messages.ToolboxView_RasterMath, null, null);
        generalTool.addChild(mathTool);
        buildTool(mathTool, "org.geotools.process.spatialstatistics.RasterMathProcessFactory");
        buildTool(mathTool, "org.geotools.process.spatialstatistics.RasterNDVIProcessFactory");

        // Density
        TreeParent densityTool = new TreeParent(Messages.ToolboxView_Density, null, null);
        generalTool.addChild(densityTool);
        buildTool(densityTool, "org.geotools.process.spatialstatistics.KernelDensityProcessFactory");
        buildTool(densityTool, "org.geotools.process.spatialstatistics.PointDensityProcessFactory");
        buildTool(densityTool, "org.geotools.process.spatialstatistics.LineDensityProcessFactory");

        // Interpolation
        TreeParent interpolationTool = new TreeParent(Messages.ToolboxView_Interpolation, null,
                null);
        generalTool.addChild(interpolationTool);
        buildTool(interpolationTool, "org.geotools.process.spatialstatistics.IDWProcessFactory");
        buildTool(interpolationTool, "org.geotools.process.spatialstatistics.TPSProcessFactory");

        // Surface Analysis
        TreeParent surfaceTool = new TreeParent(Messages.ToolboxView_RasterSurface, null, null);
        generalTool.addChild(surfaceTool);
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterHighLowProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterProfileProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterLinearLOSProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterRadialLOSProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterSlopeProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterAspectProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterHillshadeProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterTPIProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterTRIProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterRoughnessProcessFactory");
        buildTool(surfaceTool, "org.geotools.process.spatialstatistics.RasterCurvatureProcessFactory");
        
        // Zonal Tools
        TreeParent zonalTool = new TreeParent(Messages.ToolboxView_RasterZonal, null, null);
        generalTool.addChild(zonalTool);
        buildTool(zonalTool, "org.geotools.process.spatialstatistics.RasterZonalStatisticsProcessFactory");

        // Utilities
        TreeParent utilTool = new TreeParent(Messages.ToolboxView_Utilities, null, null);
        generalTool.addChild(utilTool);
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterForceCRSProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterReprojectProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterResampleProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterFlipProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterMirrorProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterRescaleProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterRotateProcessFactory");
        buildTool(utilTool, "org.geotools.process.spatialstatistics.RasterShiftProcessFactory");
    }

    private void buildTool(TreeParent parent, String title, String dialogName) {
        parent.addChild(new TreeObject(title, null, new NameImpl(null, dialogName)));
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
