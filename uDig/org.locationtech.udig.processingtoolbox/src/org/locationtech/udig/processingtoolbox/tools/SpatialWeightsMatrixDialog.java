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

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Widget;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.DistanceFactory;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;

/**
 * Spatial Weights Matrix Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialWeightsMatrixDialog extends AbstractGeoProcessingDialog {
    protected static final Logger LOGGER = Logging.getLogger(SpatialWeightsMatrixDialog.class);

    private ILayer activeLayer;

    private Combo cboLayer, cboField, cboWeightType, cboDistMethod, cboRowStd;

    private Button optDistanceBased, optContiguitybased, optQueen, optRook, optBishops, optDistance, optKNearest, btnThresh, chkExponent;

    private Spinner spnContiguity, spnDistacne, spnNeighbors, spnExponent;
    
    private Group grpDist, grpCont;
    
    private StackLayout stackLayout;
    
    private Composite cmpStack;
        
    public SpatialWeightsMatrixDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.SpatialWeightsMatrixDialog_title;
        this.windowDesc = Messages.SpatialWeightsMatrixDialog_description;
        this.windowSize = new Point(650, 475);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(3, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$

        // Layer
        uiBuilder.createLabel(container, Messages.SpatialWeightsMatrixDialog_InputLayer, EMPTY, image, 1);
        cboLayer = uiBuilder.createCombo(container, 2, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        // Unique ID Field
        uiBuilder.createLabel(container, Messages.SpatialWeightsMatrixDialog_UniqueField, EMPTY, image, 1);
        cboField = uiBuilder.createCombo(container, 2, true);

        // Spatial Weight Types
        uiBuilder.createLabel(container, Messages.SpatialWeightsMatrixDialog_WeightType, EMPTY, image, 1);
        optDistanceBased = uiBuilder.createRadioButton(container, Messages.SpatialWeightsMatrixDialog_WeightTypeDistance, EMPTY, 1);
        optContiguitybased = uiBuilder.createRadioButton(container, Messages.SpatialWeightsMatrixDialog_WeightTypeContiguity, EMPTY, 1);
        optDistanceBased.addSelectionListener(selectionListener);
        optContiguitybased.addSelectionListener(selectionListener);
        
        // StackLayout
        uiBuilder.createLabel(container, EMPTY, EMPTY, 3);
        
        stackLayout = new StackLayout();
        cmpStack = new Composite(container, SWT.NONE);
        cmpStack.setLayout(stackLayout);
        cmpStack.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        // 1. Distance Based Weights
        grpDist = uiBuilder.createGroup(cmpStack, Messages.SpatialWeightsMatrixDialog_WeightTypeDistance, true, 2);
        Composite cmpDist = new Composite(grpDist, SWT.NONE);
        cmpDist.setLayout(uiBuilder.createGridLayout(3, false, 0, 5));
        cmpDist.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        optDistance = uiBuilder.createRadioButton(cmpDist,  Messages.SpatialWeightsMatrixDialog_ThresholdDistance, EMPTY, 1);
        optDistance.addSelectionListener(selectionListener);
        btnThresh = uiBuilder.createButton(cmpDist,  Messages.SpatialWeightsMatrixDialog_Calculate, EMPTY, 1);
        btnThresh.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        spnDistacne = uiBuilder.createSpinner(cmpDist, 0, 0, Integer.MAX_VALUE, 2, 10000, 100000, 1);

        uiBuilder.createLabel(cmpDist, Messages.SpatialWeightsMatrixDialog_DistanceMethod, EMPTY, 1);
        cboDistMethod = uiBuilder.createCombo(cmpDist, 2, true);
        cboDistMethod.setItems(new String[] { Messages.SpatialWeightsMatrixDialog_Euclidean, Messages.SpatialWeightsMatrixDialog_Manhattan });
        cboDistMethod.select(0);
        
        uiBuilder.createLabel(cmpDist, Messages.SpatialWeightsMatrixDialog_RowStandardization, EMPTY, 1);
        cboRowStd = uiBuilder.createCombo(cmpDist, 2, true);
        cboRowStd.setItems(new String[] { "True", "False" });  //$NON-NLS-1$//$NON-NLS-2$
        cboRowStd.select(0);
        
        // advanced options
        chkExponent = uiBuilder.createCheckbox(cmpDist, Messages.SpatialWeightsMatrixDialog_Exponent, EMPTY, 1);
        spnExponent = uiBuilder.createSpinner(cmpDist, 1, 1, 2, 0, 1, 1, 2);

        optKNearest = uiBuilder.createRadioButton(cmpDist,  Messages.SpatialWeightsMatrixDialog_kNearestNeighbors, EMPTY, 1);
        optKNearest.addSelectionListener(selectionListener);
        uiBuilder.createLabel(cmpDist,  Messages.SpatialWeightsMatrixDialog_Numberofneighbors, EMPTY, 1);
        spnNeighbors = uiBuilder.createSpinner(cmpDist, 4, 1, 24, 0, 1, 2, 1);

        // 2. Contiguity Based Weights
        grpCont = uiBuilder.createGroup(cmpStack, Messages.SpatialWeightsMatrixDialog_WeightTypeContiguity, true, 2);
        Composite cmpCont = new Composite(grpCont, SWT.NONE);
        cmpCont.setLayout(uiBuilder.createGridLayout(3, false, 0, 5));
        cmpCont.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        optQueen = uiBuilder.createRadioButton(cmpCont, Messages.SpatialWeightsMatrixDialog_Queen, EMPTY, 1);
        optQueen.addSelectionListener(selectionListener);
        uiBuilder.createLabel(cmpCont, Messages.SpatialWeightsMatrixDialog_OrderofContiguity, EMPTY, 1);
        spnContiguity = uiBuilder.createSpinner(cmpCont, 1, 1, 12, 0, 1, 10, 1);

        optRook = uiBuilder.createRadioButton(cmpCont, Messages.SpatialWeightsMatrixDialog_Rook, EMPTY, 3);
        optBishops = uiBuilder.createRadioButton(cmpCont, Messages.SpatialWeightsMatrixDialog_Bishops, EMPTY, 3);
        
        stackLayout.topControl = grpDist;
        cmpStack.layout();

        // 3. Output location
        locationView = new OutputDataWidget(FileDataType.WEIGHT_MATRIX, SWT.SAVE);
        locationView.create(container, SWT.BORDER, 3, 1);

        File file = new File(ToolboxView.getWorkspace(), "weight_matrix.gal"); //$NON-NLS-1$
        locationView.setFile(file.getPath());

        // Listener
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboLayer.getSelectionIndex() == -1) {
                    return;
                }
                activeLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (activeLayer != null) {
                    fillFields(cboField, activeLayer.getSchema(), FieldType.ALL);
                }
            }
        });
        
        btnThresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (activeLayer == null) {
                            openInformation(getShell(), Messages.Task_ParameterRequired);
                            return;
                        }
                        
                        SimpleFeatureCollection features = MapUtils.getFeatures(activeLayer);
                        List<SpatialEvent> events = DistanceFactory.loadEvents(features, null);

                        DistanceFactory factory = DistanceFactory.newInstance();
                        factory.DistanceType = cboDistMethod.getSelectionIndex() == 0 ? DistanceMethod.Euclidean
                                : DistanceMethod.Manhattan;
                        double sumDistance = 0.0;
                        for (SpatialEvent curEvent : events) {
                            double nnDist = factory.getMinimumDistance(events, curEvent);
                            sumDistance += nnDist;
                        }

                        double averageDistance = sumDistance / (events.size() * 1.0);
                        spnDistacne.setSelection(uiBuilder.toSpinnerValue(spnDistacne, averageDistance));
                    }
                };

                try {
                    BusyIndicator.showWhile(Display.getCurrent(), runnable);
                } catch (Exception e) {
                    ToolboxPlugin.log(e);
                }
            }
        });
        
        optDistanceBased.setSelection(true);
        optQueen.setSelection(true);
        optDistance.setSelection(true);
        spnNeighbors.setEnabled(false);

        area.pack(true);
        return area;
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(optQueen)) {
                spnContiguity.setEnabled(optQueen.getSelection());
            } else if (widget.equals(optDistance)) {
                btnThresh.setEnabled(optDistance.getSelection());
                spnDistacne.setEnabled(optDistance.getSelection());
                spnExponent.setEnabled(optDistance.getSelection());
                chkExponent.setEnabled(optDistance.getSelection());
                cboDistMethod.setEnabled(optDistance.getSelection());
                cboRowStd.setEnabled(optDistance.getSelection());
            } else if (widget.equals(optKNearest)) {
                spnNeighbors.setEnabled(optKNearest.getSelection());
            } else if (widget.equals(optDistanceBased)) {
                stackLayout.topControl = grpDist;
                cmpStack.layout();
            } else if (widget.equals(optContiguitybased)) {
                stackLayout.topControl = grpCont;
                cmpStack.layout();
            }
        }
    };

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, cboField)) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            @SuppressWarnings({})
            public void run() {
                SimpleFeatureCollection features = MapUtils.getFeatures(activeLayer);
                String fieldName = cboField.getText();
                
                openInformation(getShell(), "implementing...");
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
