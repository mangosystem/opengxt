/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.process.spatialstatistics.styler.GraduatedColorStyleBuilder;
import org.geotools.process.spatialstatistics.styler.GraduatedSymbolStyleBuilder;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.style.sld.SLDContent;
import org.locationtech.udig.style.sld.editor.BorderColorComboListener.Outline;
import org.locationtech.udig.ui.graphics.Glyph;

/**
 * Histogram Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ThematicMapDialog extends AbstractGeoProcessingDialog {
    protected static final Logger LOGGER = Logging.getLogger(ThematicMapDialog.class);

    @SuppressWarnings("nls")
    static final String[] NUMERIC_METHOD = new String[] { "Equal Interval", "Natural Breaks",
            "Quantile", "Standard Deviation" };

    @SuppressWarnings("nls")
    static final String[] CATEGORY_METHOD = new String[] { "Unique Values", "LISA Style" };

    private ColorBrewer brewer;

    private ILayer activeLayer;

    private Combo cboLayer, cboField, cboMethod, cboNormal, cboOutline;

    private ImageCombo cboColorRamp;

    private Spinner spnClass, spnTransparency, spnLineWidth;

    private Slider sldTransparency;

    public ThematicMapDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.ThematicMapDialog_title;
        this.windowDesc = Messages.ThematicMapDialog_description;
        this.windowSize = new Point(500, 330);
        this.brewer = ColorBrewer.instance();
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(6, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$

        // Layer
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_InputLayer, EMPTY, image, 1);
        cboLayer = uiBuilder.createCombo(container, 5, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        // Field
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_InputField, EMPTY, image, 1);
        cboField = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Normalization, EMPTY, image, 1);
        cboNormal = uiBuilder.createCombo(container, 2, true);

        // Class
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Mode, EMPTY, image, 1);
        cboMethod = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Classes, EMPTY, image, 1);
        spnClass = uiBuilder.createSpinner(container, 5, 2, 20, 0, 1, 1, 2);

        // Ramp
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_ColorRamp, EMPTY, image, 1);
        cboColorRamp = new ImageCombo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        cboColorRamp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

        // transparency
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Transparency, EMPTY, image, 1);

        Composite subCon = new Composite(container, SWT.NONE);
        subCon.setLayout(uiBuilder.createGridLayout(5, false, 0, 5));
        subCon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

        sldTransparency = uiBuilder.createSlider(subCon, 10, 0, 100, 0, 1, 10, 4);

        spnTransparency = new Spinner(subCon, SWT.RIGHT_TO_LEFT | SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gridData.widthHint = 20;
        spnTransparency.setLayoutData(gridData);
        spnTransparency.setValues(10, 0, 100, 0, 1, 10);

        // outline
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_OutlineColor, EMPTY, image, 1);
        cboOutline = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_OutlineWidth, EMPTY, image, 1);
        spnLineWidth = uiBuilder.createSpinner(container, 5, 0, 100, 1, 1, 10, 2);

        cboOutline.setItems(Outline.labels());
        cboOutline.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                int selection = cboOutline.getSelectionIndex();
                if (selection == 0) { // None
                    cboOutline.setData(java.awt.Color.GRAY);
                } else if (selection == 1) { // Black
                    cboOutline.setData(java.awt.Color.BLACK);
                } else if (selection == 2) { // White
                    cboOutline.setData(java.awt.Color.WHITE);
                } else if (selection == 3) { // Custom
                    ColorDialog cd = new ColorDialog(parent.getShell());
                    cd.setText("Select Custom Color"); //$NON-NLS-1$
                    if (cboOutline.getData() != null) {
                        java.awt.Color c = (java.awt.Color) cboOutline.getData();
                        cd.setRGB(new RGB(c.getRed(), c.getGreen(), c.getBlue()));
                    }
                    RGB rgb = cd.open();
                    if (rgb != null) {
                        cboOutline.setData(new java.awt.Color(rgb.red, rgb.green, rgb.blue));
                    }
                }
            }
        });
        cboOutline.select(2);

        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboLayer.getSelectionIndex() == -1) {
                    return;
                }
                activeLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (activeLayer != null) {
                    fillFields(cboField, activeLayer.getSchema(), FieldType.Number);
                    fillFields(cboNormal, activeLayer.getSchema(), FieldType.Number);
                    cboNormal.add(EMPTY);
                    cboNormal.select(cboNormal.getItemCount() - 1);
                    cboMethod.removeAll();
                }
            }
        });

        cboField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboField.getSelectionIndex() == -1) {
                    return;
                }
                boolean numeric = FeatureTypes.isNumeric(activeLayer.getSchema(),
                        cboField.getText());
                cboNormal.setEnabled(numeric);
                if (numeric) {
                    cboMethod.setItems(NUMERIC_METHOD);
                } else {
                    cboMethod.setItems(CATEGORY_METHOD);
                }
                cboMethod.select(0);
            }
        });

        sldTransparency.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                spnTransparency.setSelection(sldTransparency.getSelection());
            }
        });

        spnTransparency.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                sldTransparency.setSelection(spnTransparency.getSelection());
            }
        });

        updateColorRamp(0);
        area.pack(true);
        return area;
    }

    @SuppressWarnings("nls")
    private void updateColorRamp(int selection) {
        BrewerPalette[] palettes = null;
        if (selection == 0) // All
            palettes = brewer.getPalettes(ColorBrewer.ALL);
        else if (selection == 1) // Numerical
            palettes = brewer.getPalettes(ColorBrewer.SUITABLE_RANGED);
        else if (selection == 2) // Sequential
            palettes = brewer.getPalettes(ColorBrewer.SEQUENTIAL);
        else if (selection == 3) // Diverging
            palettes = brewer.getPalettes(ColorBrewer.DIVERGING);
        else if (selection == 4) // Categorical
            palettes = brewer.getPalettes(ColorBrewer.SUITABLE_UNIQUE);
        else
            palettes = brewer.getPalettes(ColorBrewer.ALL);

        cboColorRamp.removeAll();
        for (BrewerPalette palette : palettes) {
            String name = String.format("%s(%s)", palette.getName(), palette.getDescription());
            Image image = Glyph.palette(palette.getColors()).createImage();
            cboColorRamp.add(image, name);
        }
        cboColorRamp.select(cboColorRamp.getItemCount() > 5 ? 5 : 0);
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, cboField)) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
        }

        // Style origStyle = (Style) inputLayer.getStyleBlackboard().get(SLDContent.ID);

        Runnable runnable = new Runnable() {
            @Override
            @SuppressWarnings({})
            public void run() {

                float opacity = (100 - spnTransparency.getSelection()) / 100f;
                String fieldName = cboField.getText();
                String normalProperty = cboNormal.getText();
                String palette = cboColorRamp.getItem(cboColorRamp.getSelectionIndex());
                String paletteName = palette.split("\\(")[0]; //$NON-NLS-1$

                java.awt.Color outlineColor = (java.awt.Color) cboOutline.getData();
                float outlineWidth = spnLineWidth.getSelection() / (10f * spnLineWidth.getDigits());
                if (cboOutline.getSelectionIndex() == 0) {
                    outlineWidth = 0f;
                }

                int numClasses = spnClass.getSelection();
                String functionName = getFunctionName();

                SimpleFeatureCollection features = MapUtils.getFeatures(activeLayer);

                // create default style
                SSStyleBuilder ssBuilder = new SSStyleBuilder(activeLayer.getSchema());
                ssBuilder.setOpacity(opacity);
                Style style = ssBuilder.getDefaultFeatureStyle();

                // crate thematic style
                SimpleShapeType shapeType = FeatureTypes
                        .getSimpleShapeType(activeLayer.getSchema());
                if (shapeType == SimpleShapeType.POINT) {
                    GraduatedSymbolStyleBuilder builder = new GraduatedSymbolStyleBuilder();
                    builder.setFillOpacity(opacity);
                    builder.setOutlineColor(outlineColor);
                    builder.setOutlineWidth(outlineWidth);
                    if (outlineWidth == 0) {
                        builder.setLineOpacity(0.0f);
                    }
                    builder.setNormalProperty(normalProperty);
                    builder.setMethodName(functionName);

                    style = builder.createStyle(features, fieldName);
                } else {
                    GraduatedColorStyleBuilder builder = new GraduatedColorStyleBuilder();
                    builder.setFillOpacity(opacity);
                    builder.setOutlineColor(outlineColor);
                    builder.setOutlineWidth(outlineWidth);
                    if (outlineWidth == 0) {
                        builder.setLineOpacity(0.0f);
                    }
                    builder.setNormalProperty(normalProperty);

                    style = builder.createStyle(features, fieldName, functionName, numClasses,
                            paletteName);
                }

                if (style != null) {
                    // put the style on the blackboard
                    activeLayer.getStyleBlackboard().clear();
                    activeLayer.getStyleBlackboard().put(SLDContent.ID, style);
                    activeLayer.getStyleBlackboard().flush();
                    activeLayer.refresh(activeLayer.getBounds(new NullProgressMonitor(), null));
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
        } catch (Exception e) {
            ToolboxPlugin.log(e);
            MessageDialog.openError(getParentShell(), Messages.General_Error, e.getMessage());
        }
    }

    private String getFunctionName() {
        String styleName = cboMethod.getText().toUpperCase();

        String functionName = null;
        if (styleName.startsWith("LISA")) { //$NON-NLS-1$
            functionName = "LISA Style"; //$NON-NLS-1$
        } else if (styleName.startsWith("ZSCORE")) { //$NON-NLS-1$
            functionName = "ZScore"; //$NON-NLS-1$
        } else if (styleName.startsWith("ZSCORE STAND")) { //$NON-NLS-1$
            functionName = "ZScore Standard"; //$NON-NLS-1$
        } else if (styleName.startsWith("CL") || styleName.startsWith("JE") //$NON-NLS-1$ //$NON-NLS-2$
                || styleName.startsWith("NA")) { //$NON-NLS-1$
            functionName = "JenksNaturalBreaksFunction"; //$NON-NLS-1$
        } else if (styleName.startsWith("E")) { //$NON-NLS-1$
            functionName = "EqualIntervalFunction"; //$NON-NLS-1$
        } else if (styleName.startsWith("S")) { //$NON-NLS-1$
            functionName = "StandardDeviationFunction"; //$NON-NLS-1$
        } else if (styleName.startsWith("Q")) { //$NON-NLS-1$
            functionName = "QuantileFunction"; //$NON-NLS-1$
        } else if (styleName.startsWith("U")) { //$NON-NLS-1$
            functionName = "UniqueIntervalFunction"; //$NON-NLS-1$
        }

        return functionName;
    }
}
