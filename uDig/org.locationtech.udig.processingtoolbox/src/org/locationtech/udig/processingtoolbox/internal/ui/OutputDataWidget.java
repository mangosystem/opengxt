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
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.DataStore;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.storage.DataStoreFactory;

/**
 * Output Data Viewer
 * 
 * @author MapPlus
 * 
 */
public class OutputDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(OutputDataWidget.class);

    public enum FileDataType {
        PGDB, SHAPEFILE, RASTER, EXCEL, FOLDER
    }

    private Text txtPath;

    private FileDataType fileType = FileDataType.RASTER;

    private int openType = SWT.SAVE;

    private String[] fileNames;

    private String[] fileExtensions;

    @SuppressWarnings("nls")
    public OutputDataWidget(FileDataType fileType, int openType) {
        this.openType = openType;
        this.fileType = fileType;

        switch (fileType) {
        case EXCEL:
            fileNames = new String[] { "Microsoft Excel Document (*.xls, *.xlsx)" };
            fileExtensions = new String[] { "*.xls;*.xlsx" };
            break;
        case PGDB:
            fileNames = new String[] { "Microsoft Access Database (*.mdb)" };
            fileExtensions = new String[] { "*.mdb" };
            break;
        case RASTER:
            fileNames = new String[] { "GeoTiff Files (*.tif, *.tiff)" };
            fileExtensions = new String[] { "*.tif;*.tiff" };
            break;
        case SHAPEFILE:
            fileNames = new String[] { "ESRI Shapefile (*.shp)" };
            fileExtensions = new String[] { "*.shp" };
            break;
        case FOLDER:
            break;
        }
    }

    public void create(final Composite parent, final int style, final int colspan) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, colspan, 1));
        
        GridLayout layout = new GridLayout(3, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        String labelTitle = Messages.OutputDataViewer_outputlocation;
        if (fileType == FileDataType.FOLDER) {
            labelTitle = Messages.OutputDataViewer_outputlocation;
        } else {
            if (openType == SWT.OPEN) {
                labelTitle = Messages.OutputDataViewer_selectdata;
            } else {
                labelTitle = Messages.OutputDataViewer_outputdata;
            }
        }

        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$
        CLabel lblName = new CLabel(composite, SWT.NONE);
        lblName.setImage(image);
        lblName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        lblName.setText(labelTitle);

        txtPath = new Text(composite, SWT.BORDER);
        txtPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Button btnOpen = new Button(composite, SWT.PUSH);
        btnOpen.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        Image saveImage = ToolboxPlugin.getImageDescriptor("icons/save.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(saveImage);

        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (fileType == FileDataType.FOLDER) {
                    DirectoryDialog dirDialog = new DirectoryDialog(parent.getShell());
                    dirDialog.setMessage(Messages.OutputDataViewer_folderdialog);
                    dirDialog.setFilterPath(txtPath.getText());
                    String selectedDir = dirDialog.open();
                    if (selectedDir != null) {
                        txtPath.setText(selectedDir);
                    }
                } else {
                    FileDialog fileDialog = new FileDialog(parent.getShell(), openType);
                    fileDialog.setFilterNames(fileNames);
                    fileDialog.setFilterExtensions(fileExtensions);
                    fileDialog.setFileName(getFile());

                    String selectedFile = fileDialog.open();
                    if (selectedFile != null) {
                        txtPath.setText(selectedFile);
                    }
                }
            }
        });

        composite.pack();
    }

    public DataStore getDataStore() {
        return DataStoreFactory.getShapefileDataStore(getFolder());
    }

    public String getFolder() {
        if (fileType == FileDataType.FOLDER) {
            return getFile();
        } else {
            return new File(getFile()).getParent();
        }
    }

    public void setFolder(String folder) {
        this.txtPath.setText(folder);
    }

    public String getFile() {
        return this.txtPath.getText();
    }

    public void setFile(String filePath) {
        this.txtPath.setText(filePath);
    }

    public void setFile(File filePath) {
        this.txtPath.setText(filePath.getPath());
    }

    public void setReferenceFile(final String sourceFile, String postfix) {
        File file = new File(sourceFile);

        String outputFile = ""; //$NON-NLS-1$
        if (file.isFile()) {
            outputFile = file.getParent() + File.separator;
            String fileName = file.getName();
            int pos = file.getName().lastIndexOf("."); //$NON-NLS-1$
            if (pos > 0) {
                fileName = file.getName().substring(0, pos);
                outputFile += fileName + "_" + postfix + file.getName().substring(pos); //$NON-NLS-1$
            } else {
                outputFile += fileName + "_" + postfix; //$NON-NLS-1$
            }
        }

        setFile(outputFile);
    }
}