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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;

/**
 * Setting Dialog
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class SettingsDialog extends Dialog {
    protected static final Logger LOGGER = Logging.getLogger(SettingsDialog.class);

    public SettingsDialog(Shell parentShell) {
        super(parentShell);

        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.SettingsDialog_title);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(550, 300);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight =  layout.marginRight = layout.marginBottom = 2;
        area.setLayout(layout);
        
        WidgetBuilder widget = WidgetBuilder.newInstance();
        
        // 0. create tab folder
        CTabFolder parentTabFolder = widget.createTabFolder(area, 1);
        
        // 1. general tab
        CTabItem tabItemGeneral = widget.createTabItem(parentTabFolder, Messages.SettingsDialog_general);
        Composite generalComposite = new Composite(parentTabFolder, SWT.NONE);
        generalComposite.setLayout(new GridLayout(4, false));
        generalComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // 1.1 workspace
        widget.createLabel(generalComposite, Messages.ToolboxView_workspace, null, 1);
        final Text txtCrs = widget.createText(generalComposite, ToolboxView.getWorkspace(), 2, false);
        final Button btnOpen = widget.createButton(generalComposite, "...", null, 1); //$NON-NLS-1$
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                final Shell shell = Display.getCurrent().getActiveShell();
                DirectoryDialog dirDialog = new DirectoryDialog(shell);
                dirDialog.setFilterPath(ToolboxView.getWorkspace());
                if (dirDialog.open() != null) {
                    ToolboxView.setWorkspace(dirDialog.getFilterPath());
                    txtCrs.setText(ToolboxView.getWorkspace());
                }
            }
        });
        
        widget.createLabel(generalComposite, null, null, 4);
        final Button chkLog = widget.createCheckbox(generalComposite, Messages.SettingsDialog_UseLog, null, 4);
        chkLog.setSelection(ToolboxView.getShowLog());
        chkLog.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                ToolboxView.setShowLog(chkLog.getSelection());
            }
        });
        
        tabItemGeneral.setControl(generalComposite);
        
        // 2. advanced tab
        CTabItem tabItemAdvanced = widget.createTabItem(parentTabFolder, Messages.SettingsDialog_advanced);
        Composite advancedComposite = new Composite(parentTabFolder, SWT.NONE);
        advancedComposite.setLayout(new GridLayout(4, false));
        advancedComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // 2.1 Process management - register & load...
        // TODO: 
        //widget.createLabel(advancedComposite, "", null, 4);
        
        tabItemAdvanced.setControl(advancedComposite);
        
        area.pack();
        return area;
    }
}
