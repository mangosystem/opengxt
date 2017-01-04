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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.Messages;

/**
 * AMOEBA Wizard Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AmoebaWizardDialog extends WizardDialog {
    protected static final Logger LOGGER = Logging.getLogger(AmoebaWizardDialog.class);

    private static final int width = 500;

    private static final int height = 600;

    int pageCount = 1;

    public AmoebaWizardDialog(Shell parentShell, IWizard newWizard) {
        super(parentShell, newWizard);

        super.setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS
                | SWT.RESIZE);
        super.setWizard(newWizard);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setSize(width, height);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control control = super.createDialogArea(parent);
        getProgressMonitor();
        return control;
    }

    @Override
    protected IProgressMonitor getProgressMonitor() {
        ProgressMonitorPart monitor = (ProgressMonitorPart) super.getProgressMonitor();
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = 0;
        monitor.setLayoutData(gridData);
        monitor.setVisible(false);
        return monitor;
    }

    @Override
    protected ProgressMonitorPart createProgressMonitorPart(Composite composite, GridLayout pmlayout) {
        return super.createProgressMonitorPart(composite, pmlayout);
    }

    @Override
    protected void nextPressed() {
        super.nextPressed();
        pageCount++;
        updateWizardProgress();
    }

    @Override
    protected void backPressed() {
        super.backPressed();
        pageCount--;
        updateWizardProgress();
    }

    @Override
    protected void finishPressed() {
        super.finishPressed();
    }

    private void updateWizardProgress() {
        getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                getProgressMonitor().beginTask(Messages.General_Completed,
                        getWizard().getPageCount());
                getProgressMonitor().worked(pageCount);
            }
        });
    }
}
