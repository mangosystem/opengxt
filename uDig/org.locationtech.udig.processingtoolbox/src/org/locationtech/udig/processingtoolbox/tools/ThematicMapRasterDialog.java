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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.process.spatialstatistics.styler.GraduatedColorStyleBuilder;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.style.sld.SLDContent;

/**
 * Histogram Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ThematicMapRasterDialog extends AbstractThematicMapDialog {
    protected static final Logger LOGGER = Logging.getLogger(ThematicMapRasterDialog.class);

    private Text txtNoData;

    private Combo cboLayer, cboMethod;

    private Spinner spnClass, spnTransparency;

    private Slider sldTransparency;

    public ThematicMapRasterDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        this.windowTitle = Messages.ThematicMapRasterDialog_title;
        this.windowDesc = Messages.ThematicMapRasterDialog_description;
        this.windowSize = ToolboxPlugin.rescaleSize(parentShell, 550, 320);
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
        fillRasterLayers(map, cboLayer);

        // Nodata
        uiBuilder.createLabel(container, Messages.ThematicMapRasterDialog_NoData, EMPTY, image, 1);
        txtNoData = uiBuilder.createText(container, EMPTY, 5, true);

        // Class
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Mode, EMPTY, image, 1);
        cboMethod = uiBuilder.createCombo(container, 2, true);
        cboMethod.setItems(NUMERIC_METHOD);
        cboMethod.select(0);

        uiBuilder.createLabel(container, Messages.ThematicMapDialog_Classes, EMPTY, image, 1);
        spnClass = uiBuilder.createSpinner(container, 7, 2, 10, 0, 1, 1, 2);

        // Ramp
        uiBuilder.createLabel(container, Messages.ThematicMapDialog_ColorRamp, EMPTY, image, 1);
        cboColorRamp = uiBuilder.createCombo(container, 5, true);

        // Preview ramp
        uiBuilder.createLabel(container, EMPTY, EMPTY, null, 1);
        lblPreview = new Label(container, SWT.NONE);
        lblPreview.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 5, 1));

        // Reverse color ramp
        uiBuilder.createLabel(container, EMPTY, EMPTY, null, 1);
        chkReverse = uiBuilder.createCheckbox(container, Messages.ThematicMapDialog_Reverse, EMPTY,
                5);

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

        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboLayer.getSelectionIndex() == -1) {
                    return;
                }

                activeLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (activeLayer != null) {
                    GridCoverage2D coverage = MapUtils.getGridCoverage(activeLayer);
                    txtNoData.setText(String.valueOf(RasterHelper.getNoDataValue(coverage)));
                }
            }
        });

        cboColorRamp.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updatePreview();
            }
        });

        chkReverse.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                updatePreview();
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

        updateColorRamp(0, 0);
        area.pack(true);
        return area;
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer)) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            @SuppressWarnings({})
            public void run() {
                float opacity = (100 - spnTransparency.getSelection()) / 100f;
                String palette = cboColorRamp.getItem(cboColorRamp.getSelectionIndex());
                String paletteName = palette.split("\\(")[0]; //$NON-NLS-1$
                boolean reverse = chkReverse.getSelection();

                int numClasses = spnClass.getSelection();
                String styleName = cboMethod.getText().toUpperCase();
                String functionName = getFunctionName(styleName);

                // crate thematic style
                GridCoverage2D coverage = MapUtils.getGridCoverage(activeLayer);
                GraduatedColorStyleBuilder builder = new GraduatedColorStyleBuilder();
                Style style = builder.createStyle(coverage, functionName, numClasses, paletteName,
                        reverse, opacity);

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
}
