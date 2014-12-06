/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.operations.TextColumn;
import org.geotools.process.spatialstatistics.operations.TextfileToPointOperation;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.ui.CRSChooserDialog;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Convert Text file to Point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TextfileToPointDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(TextfileToPointDialog.class);

    private Table inputTable;

    private Combo cboSource, cboEncoding;

    private Text txtCrs, txtSplit;

    private Button btnSource, chkHeader, optTab, optColon, optComma, optSpace, optEtc;

    private String splitter = ","; //$NON-NLS-1$

    private File textFile;

    private Charset charset = Charset.defaultCharset(); // default

    public TextfileToPointDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.TextfileToPointDialog_title;
        this.windowDesc = Messages.TextfileToPointDialog_description;
        this.windowSize = new Point(700, 500);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(3, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // 1. select text file
        uiBuilder.createLabel(container, Messages.TextfileToPointDialog_Textfile, null, 1);
        cboSource = uiBuilder.createCombo(container, 1);
        cboSource.addModifyListener(modifyListener);

        btnSource = uiBuilder.createButton(container, DOT3, null, 1);
        btnSource.addSelectionListener(selectionListener);

        // 3. encoding & spatial reference
        uiBuilder.createLabel(container, Messages.TextfileToPointDialog_Encoding, null, 1);
        GridLayout layout = new GridLayout(5, false);
        layout.marginWidth = 0;

        Composite subCon = new Composite(container, SWT.NONE);
        subCon.setLayout(layout);
        subCon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        cboEncoding = uiBuilder.createCombo(subCon, 1, false);
        Map<String, Charset> charsetMap = Charset.availableCharsets();
        for (Entry<String, Charset> entrySet : charsetMap.entrySet()) {
            cboEncoding.add(entrySet.getKey());
        }
        cboEncoding.addModifyListener(modifyListener);
        cboEncoding.setText(ToolboxPlugin.defaultCharset());

        uiBuilder.createLabel(subCon, "   ", null, 1); //$NON-NLS-1$
        uiBuilder.createLabel(subCon, Messages.TextfileToPointDialog_CRS, null, 1);
        txtCrs = uiBuilder.createText(subCon, EMPTY, 1, true);
        txtCrs.setEditable(false);
        final Button btnOpen = uiBuilder.createButton(subCon, DOT3, null, 1);
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                CRSChooserDialog dialog = new CRSChooserDialog(parent.getShell(), null);
                if (dialog.open() == Window.OK) {
                    CoordinateReferenceSystem crs = dialog.getResult();
                    try {
                        if (crs != null) {
                            txtCrs.setText(CRS.lookupIdentifier(crs, true));
                        }
                    } catch (FactoryException e) {
                        txtCrs.setText(EMPTY);
                    }
                }
            }
        });

        // 3. define schema
        Group group = uiBuilder.createGroup(container, Messages.TextfileToPointDialog_Schema,
                false, 3);
        uiBuilder.createLabel(group, Messages.TextfileToPointDialog_FieldNameSetting, null, 1);
        chkHeader = uiBuilder.createCheckbox(group, Messages.TextfileToPointDialog_ColumnFirst,
                null, 1);
        chkHeader.setSelection(true);
        chkHeader.addSelectionListener(selectionListener);

        Group grpField = new Group(group, SWT.SHADOW_ETCHED_IN);
        grpField.setText(Messages.TextfileToPointDialog_Delimiters);
        grpField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        grpField.setLayout(new GridLayout(6, false));

        optTab = uiBuilder.createRadioButton(grpField, Messages.TextfileToPointDialog_Tab, null, 1);
        optTab.setData("\t"); //$NON-NLS-1$
        optTab.addSelectionListener(selectionListener);

        optColon = uiBuilder.createRadioButton(grpField, Messages.TextfileToPointDialog_Semicolon,
                null, 1);
        optColon.setData(";"); //$NON-NLS-1$
        optColon.addSelectionListener(selectionListener);

        optComma = uiBuilder.createRadioButton(grpField, Messages.TextfileToPointDialog_Colon,
                null, 1);
        optComma.setData(","); //$NON-NLS-1$
        optComma.setSelection(true);
        optComma.addSelectionListener(selectionListener);

        optSpace = uiBuilder.createRadioButton(grpField, Messages.TextfileToPointDialog_Space,
                null, 1);
        optSpace.setData(" "); //$NON-NLS-1$
        optSpace.addSelectionListener(selectionListener);

        optEtc = uiBuilder.createRadioButton(grpField, Messages.TextfileToPointDialog_Custom, null,
                1);
        optEtc.addSelectionListener(selectionListener);

        txtSplit = uiBuilder.createText(grpField, EMPTY, 1);
        txtSplit.addModifyListener(modifyListener);
        txtSplit.setEnabled(false);

        String[] header = new String[] { Messages.TextfileToPointDialog_FieldName,
                Messages.TextfileToPointDialog_FieldType,
                Messages.TextfileToPointDialog_FieldLength,
                Messages.TextfileToPointDialog_FieldRow1, Messages.TextfileToPointDialog_FieldRow2,
                Messages.TextfileToPointDialog_FieldRow3, Messages.TextfileToPointDialog_FieldRow4 };
        inputTable = uiBuilder.createSchemaEditableTable(group, header,
                TextColumn.getFieldTypes(true), 2, 2, true);

        // 5. select output folder
        locationView = new OutputDataWidget(FileDataType.SHAPEFILE, SWT.SAVE);
        locationView.create(container, SWT.BORDER, 3, 1);

        group.pack(true);
        area.pack(true);
        this.windowSize = new Point(650, area.getSize().y + 50);
        return area;
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @SuppressWarnings("nls")
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(btnSource)) {
                final Shell activeShell = event.display.getActiveShell();
                FileDialog fileDialog = new FileDialog(activeShell, SWT.SINGLE);
                fileDialog
                        .setFilterNames(new String[] { "Text file (*.txt, *.csv, *.tab, *.asc, *.dat)" });
                fileDialog.setFilterExtensions(new String[] { "*.txt;*.csv;*.tab;*.asc;*.dat" });
                String selectedFile = fileDialog.open();
                if (selectedFile != null) {
                    cboSource.add(selectedFile);
                    cboSource.setText(selectedFile);
                }
            } else if (widget.equals(chkHeader)) {
                loadTables();
            } else if (widget.equals(optTab) || widget.equals(optColon) || widget.equals(optComma)
                    || widget.equals(optSpace)) {
                splitter = widget.getData().toString();
                loadTables();
            } else if (widget.equals(optEtc)) {
                txtSplit.setEnabled(optEtc.getSelection());
                txtSplit.setFocus();
                if (!StringHelper.isNullOrEmpty(txtSplit.getText())) {
                    splitter = txtSplit.getText();
                    loadTables();
                }
            }
        }
    };

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (invalidWidgetValue(widget)) {
                return;
            }

            if (widget.equals(cboSource)) {
                textFile = new File(cboSource.getText());
                loadTables();
            } else if (widget.equals(cboEncoding)) {
                charset = Charset.forName(cboEncoding.getText());
                loadTables();
            } else if (widget.equals(txtSplit)) {
                if (optEtc.getSelection()) {
                    splitter = txtSplit.getText();
                    loadTables();
                }
            }
        }
    };

    private void loadTables() {
        if (cboSource.getSelectionIndex() == -1) {
            return;
        }

        inputTable.removeAll();

        TextColumn[] columns = TextColumn.getColumns(textFile, charset, splitter,
                chkHeader.getSelection());
        if (columns != null) {
            for (TextColumn column : columns) {
                TableItem item = new TableItem(inputTable, SWT.NONE);
                item.setText(column.getItems());
                item.setData(column);
                item.setChecked(true);
            }
        }
    }

    @SuppressWarnings("nls")
    private List<TextColumn> getTextColumns() {
        List<TextColumn> columns = new ArrayList<TextColumn>();

        for (int index = 0; index < inputTable.getItemCount(); index++) {
            TableItem item = inputTable.getItem(index);
            if (item.getChecked()) {
                TextColumn col = (TextColumn) item.getData();
                col.setName(item.getText(0));
                col.setType(item.getText(1));

                if (col.getType().equalsIgnoreCase("String")) {
                    if (item.getText(2).isEmpty()) {
                        col.setLength(255);
                    } else {
                        col.setLength(Integer.parseInt(item.getText(2)));
                    }
                }
                columns.add(col);
            }
        }

        return columns;
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboSource) || getTextColumns().isEmpty()
                || locationView.getFile().isEmpty()) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
        }

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
            openInformation(getShell(),
                    String.format(Messages.Task_Completed, locationView.getFile()));

            if (!StringHelper.isNullOrEmpty(error)) {
                if (MessageDialog.openConfirm(getShell(), windowTitle,
                        Messages.Task_ConfirmErrorFile)) {
                    String errorPath = saveErrorAsText();
                    if (errorPath != null) {
                        openInformation(getShell(),
                                String.format(Messages.Task_CheckFile, errorPath));
                    }
                }
            }
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(Messages.Task_Running, 100);
        try {
            monitor.setTaskName(String.format(Messages.Task_Executing, windowTitle));
            monitor.worked(increment);

            boolean headerFirst = chkHeader.getSelection();
            List<TextColumn> schema = getTextColumns();

            CoordinateReferenceSystem crs = null;
            if (!StringHelper.isNullOrEmpty(txtCrs.getText())) {
                try {
                    crs = CRS.decode(txtCrs.getText());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }

            String outputName = FilenameUtils.removeExtension(FilenameUtils.getName(locationView
                    .getFile()));

            monitor.subTask(String.format(Messages.Task_Executing, windowTitle));
            TextfileToPointOperation process = new TextfileToPointOperation();
            process.setOutputDataStore(locationView.getDataStore());
            process.setOutputTypeName(outputName);

            SimpleFeatureCollection features = process.execute(textFile, charset, splitter,
                    headerFirst, schema, crs);
            error = process.getError();
            monitor.worked(increment);

            if (features != null) {
                monitor.setTaskName(Messages.Task_AddingLayer);
                addFeaturesToMap(map, locationView.getFile(), outputName);
                monitor.worked(increment);
            }
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            ToolboxPlugin.log(String.format(Messages.Task_Completed, windowTitle));
            monitor.done();
        }
    }
}
