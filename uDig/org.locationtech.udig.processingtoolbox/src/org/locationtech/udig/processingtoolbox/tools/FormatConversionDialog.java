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
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.internal.ui.TableSelectionWidget;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.tools.FormatTransformer.EncodeType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Exports layers to other format
 * <p>
 * - GML, GeoJSON, KML ...
 * 
 * @author MapPlus
 */
public class FormatConversionDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(FormatConversionDialog.class);

    private Table inputTable;

    private Combo cboOption;

    private String delimiter = "|"; //$NON-NLS-1$

    private Text txtDelimiter;

    private Group grpOption;

    private Button optTab, optColon, optComma, optSpace, optEtc;

    public FormatConversionDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.FormatConversionDialog_title;
        this.windowDesc = Messages.FormatConversionDialog_description;
        this.windowSize = new Point(650, 500);
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group group = uiBuilder.createGroup(container,
                Messages.FormatConversionDialog_SelectLayers, false, 2);
        inputTable = uiBuilder.createTable(group, new String[] {
                Messages.FormatConversionDialog_Name, Messages.FormatConversionDialog_Type,
                Messages.FormatConversionDialog_CRS }, 2);

        TableSelectionWidget tblSelection = new TableSelectionWidget(inputTable);
        tblSelection.create(group, SWT.NONE, 2, 1);

        uiBuilder.createLabel(container, Messages.FormatConversionDialog_Format, null, 1);
        cboOption = uiBuilder.createCombo(container, 1);
        cboOption.setItems(new String[] { "Geography Markup Language (GML2.1.2)",
                "Geography Markup Language (GML3.1.1)", "Geography Markup Language (GML3.2)",
                "GeoJSON", "Keyhole Markup Language (KML 2.1)",
                "Keyhole Markup Language (KML 2.2)", "Delimiter separated Text files",
                "Delimiter separated WKT files", "ESRI Shapefiles" });
        cboOption.addSelectionListener(selectionListener);
        cboOption.select(1);

        grpOption = new Group(container, SWT.SHADOW_ETCHED_IN);
        grpOption.setText(Messages.TextfileToPointDialog_Delimiters);
        grpOption.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        grpOption.setLayout(new GridLayout(6, false));
        grpOption.setEnabled(false);
        grpOption.setVisible(false);

        optTab = uiBuilder
                .createRadioButton(grpOption, Messages.TextfileToPointDialog_Tab, null, 1);
        optTab.setData("\t"); //$NON-NLS-1$
        optTab.addSelectionListener(selectionListener);

        optColon = uiBuilder.createRadioButton(grpOption, Messages.TextfileToPointDialog_Semicolon,
                null, 1);
        optColon.setData(";"); //$NON-NLS-1$
        optColon.addSelectionListener(selectionListener);

        optComma = uiBuilder.createRadioButton(grpOption, Messages.TextfileToPointDialog_Colon,
                null, 1);
        optComma.setData(","); //$NON-NLS-1$
        optComma.addSelectionListener(selectionListener);

        optSpace = uiBuilder.createRadioButton(grpOption, Messages.TextfileToPointDialog_Space,
                null, 1);
        optSpace.setData(" "); //$NON-NLS-1$
        optSpace.addSelectionListener(selectionListener);

        optEtc = uiBuilder.createRadioButton(grpOption, Messages.TextfileToPointDialog_Custom,
                null, 1);
        optEtc.addSelectionListener(selectionListener);

        txtDelimiter = uiBuilder.createText(grpOption, EMPTY, 1);
        txtDelimiter.addModifyListener(modifyListener);

        // default value
        optEtc.setSelection(true);
        txtDelimiter.setText("|");

        locationView = new OutputDataWidget(FileDataType.FOLDER, SWT.OPEN);
        locationView.create(container, SWT.BORDER, 2, 1);
        locationView.setFolder(ToolboxView.getWorkspace());

        // load layers
        loadlayers(inputTable);

        area.pack(true);
        return area;
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(optTab) || widget.equals(optColon) || widget.equals(optComma)
                    || widget.equals(optSpace)) {
                delimiter = widget.getData().toString();
            } else if (widget.equals(optEtc)) {
                txtDelimiter.setEnabled(optEtc.getSelection());
                txtDelimiter.setFocus();
                if (!StringHelper.isNullOrEmpty(txtDelimiter.getText())) {
                    delimiter = txtDelimiter.getText();
                }
            } else if (widget.equals(cboOption)) {
                int selection = cboOption.getSelectionIndex();
                grpOption.setEnabled(selection == 6 || selection == 7);
                grpOption.setVisible(selection == 6 || selection == 7);
                if (selection == 7) {
                    optEtc.setSelection(true);
                    txtDelimiter.setEnabled(true);
                    txtDelimiter.setText("|"); //$NON-NLS-1$
                    txtDelimiter.setFocus();
                }
            }
        }
    };

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            if (optEtc.getSelection()) {
                delimiter = txtDelimiter.getText();
            }
        }
    };

    private void loadlayers(Table table) {
        table.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(FeatureSource.class)) {
                TableItem item = new TableItem(table, SWT.NONE);
                String type = layer.getSchema().getGeometryDescriptor().getType().getBinding()
                        .getSimpleName();
                CoordinateReferenceSystem crs = layer.getCRS();
                item.setText(new String[] { layer.getName(), type, crs.toString() });
                item.setData(layer);
            }
        }
    }

    @Override
    protected void okPressed() {
        if (!existCheckedItem(inputTable)) {
            openInformation(getShell(), Messages.FormatConversionDialog_Warning);
            return;
        }

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
            openInformation(getShell(), Messages.General_Completed);
            super.okPressed();
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(String.format(Messages.Task_Executing, windowTitle),
                inputTable.getItems().length * increment);
        try {
            monitor.worked(increment);

            final String outputFolder = locationView.getFolder();
            int selectionIdx = cboOption.getSelectionIndex();

            FormatTransformer ftrans = null;
            ShapeExportOperation export = null;
            if (selectionIdx == 8) {
                export = new ShapeExportOperation();
                export.setOutputDataStore(locationView.getDataStore());
            } else {
                ftrans = new FormatTransformer(EncodeType.valueOf(selectionIdx));
            }

            for (TableItem item : inputTable.getItems()) {
                monitor.subTask(item.getText());
                if (item.getChecked()) {
                    ILayer layer = (ILayer) item.getData();
                    SimpleFeatureCollection features = MapUtils.getFeatures(layer);
                    if (selectionIdx == 6) { // CSV
                        Charset charset = Charset.forName(ToolboxPlugin.defaultCharset());
                        File outputFile = new File(outputFolder, layer.getName()
                                + ftrans.getExtension());
                        ftrans.encodeCSV(features, outputFile, charset, delimiter);
                    } else if (selectionIdx == 7) { // WKT
                        Charset charset = Charset.forName(ToolboxPlugin.defaultCharset());
                        File outputFile = new File(outputFolder, layer.getName()
                                + ftrans.getExtension());
                        ftrans.encodeWKT(features, outputFile, charset, delimiter);
                    } else if (selectionIdx == 8) { // Shape file
                        export.setOutputTypeName(layer.getName());
                        export.execute(features);
                    } else {
                        File outputFile = new File(outputFolder, layer.getName()
                                + ftrans.getExtension());
                        ftrans.encode(features, outputFile);
                    }
                }
                monitor.worked(increment);
            }
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            monitor.done();
        }
    }

}
