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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.tools.excel.ExcelFormatReader;
import org.locationtech.udig.processingtoolbox.tools.excel.ExcelFormatReader.ExcelSheetInfo;
import org.locationtech.udig.processingtoolbox.tools.excel.ExcelToPointOperaion;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.ui.CRSChooserDialog;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Convert Excel file to Point features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ExcelToPointDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(ExcelToPointDialog.class);

    private ExcelFormatReader formatReader;

    private Table inputTable;

    private Combo cboSource, cboSheet;

    private Button btnSource, chkHeader;

    private Text txtSourceCrs, txtTargetCrs;

    private Button btnSourceCrs, btnTargetCrs;

    public ExcelToPointDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.ExcelToPointDialog_title;
        this.windowDesc = Messages.ExcelToPointDialog_description;
        this.windowSize = ToolboxPlugin.rescaleSize(parentShell, 650, 500);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(3, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // 1. select excel file
        uiBuilder.createLabel(container, Messages.ExcelToPointDialog_Excelfile, null, 1);
        cboSource = uiBuilder.createCombo(container, 1);
        cboSource.addModifyListener(modifyListener);

        btnSource = uiBuilder.createButton(container, DOT3, null, 1);
        btnSource.addSelectionListener(selectionListener);

        // 2. select sheet
        uiBuilder.createLabel(container, Messages.ExcelToPointDialog_Sheet, null, 1);
        cboSheet = uiBuilder.createCombo(container, 2);
        cboSheet.addModifyListener(modifyListener);

        // 3. source/target spatial reference
        GridLayout subLayout = new GridLayout(6, false);
        subLayout.marginWidth = 0;
        
        uiBuilder.createLabel(container, EMPTY, null, 1);
        Composite subCon = new Composite(container, SWT.NONE);
        subCon.setLayout(subLayout);
        subCon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        uiBuilder.createLabel(subCon, Messages.ExcelToPointDialog_SourceCRS, null, 1);
        txtSourceCrs = uiBuilder.createText(subCon, EMPTY, 1, true);
        txtSourceCrs.setEditable(false);
        txtSourceCrs.setData(null);

        btnSourceCrs = uiBuilder.createButton(subCon, DOT3, null, 1);
        btnSourceCrs.addSelectionListener(selectionListener);

        uiBuilder.createLabel(subCon, Messages.ExcelToPointDialog_TargetCRS, null, 1);
        txtTargetCrs = uiBuilder.createText(subCon, EMPTY, 1, true);
        txtTargetCrs.setEditable(false);
        txtTargetCrs.setData(null);

        btnTargetCrs = uiBuilder.createButton(subCon, DOT3, null, 1);
        btnTargetCrs.addSelectionListener(selectionListener);

        // 4. define schema
        Group group = uiBuilder.createGroup(container, Messages.TextfileToPointDialog_Schema,
                false, 3);

        uiBuilder.createLabel(group, EMPTY, null, 1);
        chkHeader = uiBuilder.createCheckbox(group, Messages.TextfileToPointDialog_ColumnFirst,
                null, 1);
        chkHeader.setSelection(true);
        chkHeader.addSelectionListener(selectionListener);

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
        return area;
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @SuppressWarnings("nls")
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            final Shell activeShell = event.display.getActiveShell();

            if (widget.equals(btnSource)) {
                FileDialog fileDialog = new FileDialog(activeShell, SWT.SINGLE);
                fileDialog
                        .setFilterNames(new String[] { "Microsoft Excel Document (*.xls, *.xlsx)" });
                fileDialog.setFilterExtensions(new String[] { "*.xls;*.xlsx" });
                String selectedFile = fileDialog.open();
                if (selectedFile != null) {
                    cboSource.add(selectedFile);
                    cboSource.setText(selectedFile);
                }
            } else if (widget.equals(chkHeader)) {
                loadTables();
            } else if (widget.equals(btnTargetCrs)) {
                CRSChooserDialog dialog = new CRSChooserDialog(activeShell, null);
                if (dialog.open() == Window.OK) {
                    CoordinateReferenceSystem crs = dialog.getResult();
                    try {
                        if (crs != null) {
                            txtTargetCrs.setText(CRS.lookupIdentifier(crs, true));
                        }
                    } catch (FactoryException e) {
                        txtTargetCrs.setText(EMPTY);
                    }
                    txtTargetCrs.setData(crs);
                }
            } else if (widget.equals(btnSourceCrs)) {
                CRSChooserDialog dialog = new CRSChooserDialog(activeShell, null);
                if (dialog.open() == Window.OK) {
                    CoordinateReferenceSystem crs = dialog.getResult();
                    try {
                        if (crs != null) {
                            txtSourceCrs.setText(CRS.lookupIdentifier(crs, true));
                        }
                    } catch (FactoryException e) {
                        txtSourceCrs.setText(EMPTY);
                    }
                    txtSourceCrs.setData(crs);
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
                // load sheet
                cboSheet.removeAll();
                if (cboSource.getSelectionIndex() == -1) {
                    return;
                }

                BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
                    @Override
                    public void run() {
                        File excelFile = new File(cboSource.getText());
                        formatReader = new ExcelFormatReader(excelFile);
                        List<String> workSheets = formatReader.getWorksheets();
                        for (String sheetName : workSheets) {
                            cboSheet.add(sheetName);
                        }

                        if (cboSheet.getItemCount() > 0) {
                            cboSheet.select(0);
                        }
                    }
                });
            } else if (widget.equals(cboSheet)) {
                loadTables();
            }
        }
    };

    private void loadTables() {
        inputTable.removeAll();

        if (cboSheet.getSelectionIndex() == -1) {
            return;
        }

        // load excel sheet
        boolean headerFirst = chkHeader.getSelection();
        ExcelSheetInfo sheetInfo = formatReader.getSheet(cboSheet.getText(), headerFirst, 4);
        for (TextColumn column : sheetInfo.getColumns()) {
            TableItem item = new TableItem(inputTable, SWT.NONE);
            item.setText(column.getItems());
            item.setData(column);
            item.setChecked(true);
        }
    }

    @SuppressWarnings("nls")
    private List<TextColumn> getExcelColumns() {
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
        if (invalidWidgetValue(cboSource, cboSheet) || getExcelColumns().isEmpty()
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

            String sheetName = cboSheet.getText();
            boolean headerFirst = chkHeader.getSelection();
            List<TextColumn> schema = getExcelColumns();

            final String outputName = locationView.getOutputName();

            CoordinateReferenceSystem sourceCRS = null;
            CoordinateReferenceSystem targetCRS = null;

            if (txtSourceCrs.getData() != null) {
                sourceCRS = (CoordinateReferenceSystem) txtSourceCrs.getData();
            }

            if (txtTargetCrs.getData() != null) {
                targetCRS = (CoordinateReferenceSystem) txtTargetCrs.getData();
            }

            monitor.subTask(String.format(Messages.Task_Executing, windowTitle));
            ExcelToPointOperaion process = new ExcelToPointOperaion();
            ShapeExportOperation exportOp = new ShapeExportOperation();

            exportOp.setOutputDataStore(locationView.getDataStore());
            exportOp.setOutputTypeName(outputName);

            SimpleFeatureCollection features = exportOp.execute(
                    process.execute(formatReader.getWorkbook(), sheetName, schema, headerFirst,
                            sourceCRS, targetCRS)).getFeatures();

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