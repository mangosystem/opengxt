/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.util.logging.Logger;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;

/**
 * Extent Selection Dialog
 * 
 * @author MapPlus
 * 
 */
public class ExtentSelectionDialog extends TitleAreaDialog {
    protected static final Logger LOGGER = Logging.getLogger(ExtentSelectionDialog.class);

    private IMap map;

    private CoordinateReferenceSystem mapCrs;

    private ReferencedEnvelope extent;

    private Composite layerContainer;

    private int height = 100;

    public ReferencedEnvelope getExtent() {
        return extent;
    }

    public ExtentSelectionDialog(Shell parentShell, IMap map) {
        super(parentShell);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);

        this.map = map;
        try {
            this.mapCrs = map.getViewportModel().getCRS();
            this.extent = map.getViewportModel().getBounds();
        } catch (Exception e) {
            this.extent = new ReferencedEnvelope();
        }
    }

    @Override
    public void create() {
        super.create();

        setTitle(Messages.ExtentSelection_title);
        setMessage(Messages.ExtentSelection_message);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(400, height);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.ExtentSelection_title);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        WidgetBuilder widget = WidgetBuilder.newInstance();

        // Radio button
        final Button optFull = widget.createRadioButton(container,
                Messages.ExtentSelection_fullextent, null, 1);
        optFull.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    extent = map.getBounds(new NullProgressMonitor());
                } catch (Exception e) {
                    extent = null;
                }
                layerContainer.setEnabled(false);
            }
        });

        // Radio button
        final Button optCurrent = widget.createRadioButton(container,
                Messages.ExtentSelection_currentextent, null, 1);
        optCurrent.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    extent = map.getViewportModel().getBounds();
                } catch (Exception e) {
                    extent = null;
                }
                layerContainer.setEnabled(false);
            }
        });

        // Radio button
        final Button optLayer = widget.createRadioButton(container,
                Messages.ExtentSelection_layerextent, null, 1);
        optLayer.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                layerContainer.setEnabled(true);
            }
        });

        // Layer selection
        layerContainer = new Composite(container, SWT.BORDER);
        layerContainer.setLayout(new GridLayout(2, false));
        layerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        widget.createLabel(layerContainer, Messages.ExtentSelection_layer, null, 1);

        final Combo cboSfLayer = widget.createCombo(layerContainer, 1);
        MapUtils.fillLayers(map, cboSfLayer);
        cboSfLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                ILayer layer = MapUtils.getLayer(map, cboSfLayer.getText());
                if (layer != null) {
                    extent = layer.getBounds(new NullProgressMonitor(), null);
                    if (!CRS.equalsIgnoreMetadata(extent.getCoordinateReferenceSystem(), mapCrs)) {
                        // 지도 범위로 변경 
                    }
                }
            }
        });

        container.pack();
        area.pack();
        height = container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 162;

        return area;
    }

}
