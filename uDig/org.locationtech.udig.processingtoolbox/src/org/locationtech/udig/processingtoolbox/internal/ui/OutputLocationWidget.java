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

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;

/**
 * Output location control
 * 
 * @author MapPlus
 * 
 */
@SuppressWarnings("nls")
public class OutputLocationWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(OutputLocationWidget.class);

    private FileDataType fileDataType = FileDataType.RASTER;

    private int fileDialogStyle = SWT.SAVE;

    private String[] fileNames;

    private String[] fileExtensions;
    
    private String fileExtension;

    public OutputLocationWidget(FileDataType fileDataType, int fileDialogStyle) {
        this.fileDialogStyle = fileDialogStyle;
        this.fileDataType = fileDataType;

        switch (fileDataType) {
        case EXCEL:
            fileNames = new String[] { "Microsoft Excel Document (*.xls, *.xlsx)" };
            fileExtensions = new String[] { "*.xls;*.xlsx" };
            fileExtension = ".xls";
            break;
        case PGDB:
            fileNames = new String[] { "Microsoft Access Database (*.mdb)" };
            fileExtensions = new String[] { "*.mdb" };
            fileExtension = ".mdb";
            break;
        case RASTER:
            fileNames = new String[] { "GeoTiff Files (*.tif, *.tiff)" };
            fileExtensions = new String[] { "*.tif;*.tiff" };
            fileExtension = ".tif";
            break;
        case SHAPEFILE:
            fileNames = new String[] { "ESRI Shapefile (*.shp)" };
            fileExtensions = new String[] { "*.shp" };
            fileExtension = ".shp";
            break;
        case FOLDER:
            break;
        }
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        GridLayout layout = new GridLayout(3, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        // 1. result title
        String title = param.title.toString();
        title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage();
        CLabel lblName = new CLabel(composite, SWT.NONE);
        lblName.setImage(image);
        lblName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        lblName.setText(title);
        lblName.setToolTipText(param.description.toString());

        // 2. file path
        final Text txtPath = new Text(composite, SWT.BORDER);
        txtPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtPath.setData(param.key);

        final Color oldBackColor = txtPath.getBackground();
        txtPath.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (txtPath.getText().length() == 0) {
                    txtPath.setBackground(oldBackColor);
                } else {
                    processParams.put(param.key, txtPath.getText());
                }
            }
        });
        
        if (param.sample != null) {
            txtPath.setText(param.sample.toString());
        } else {
            // default location
            if (fileDataType == FileDataType.FOLDER) {
                txtPath.setText(ToolboxView.getWorkspace());
            } else {
                File file = new File(ToolboxView.getWorkspace(), "result" + fileExtension);
                txtPath.setText(file.getPath());
            }
        }

        // 3. open & save button
        Button btnOpen = new Button(composite, SWT.PUSH);
        btnOpen.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        Image saveImage = ToolboxPlugin.getImageDescriptor("icons/save.gif").createImage();
        btnOpen.setImage(saveImage);

        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (fileDataType == FileDataType.FOLDER) {
                    DirectoryDialog dirDialog = new DirectoryDialog(parent.getShell());
                    dirDialog.setMessage(Messages.OutputDataViewer_folderdialog);
                    dirDialog.setFilterPath(txtPath.getText());
                    String selectedDir = dirDialog.open();
                    if (selectedDir != null) {
                        txtPath.setText(selectedDir);
                    }
                } else {
                    FileDialog fileDialog = new FileDialog(parent.getShell(), fileDialogStyle);
                    fileDialog.setFilterNames(fileNames);
                    fileDialog.setFilterExtensions(fileExtensions);
                    fileDialog.setFileName(txtPath.getText());
                    String selectedFile = fileDialog.open();
                    if (selectedFile != null) {
                        txtPath.setText(selectedFile);
                    }
                }
            }
        });

        composite.pack();
    }
}
