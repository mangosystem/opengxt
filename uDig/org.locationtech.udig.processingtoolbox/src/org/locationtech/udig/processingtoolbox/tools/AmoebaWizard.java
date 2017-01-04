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

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.catalog.util.GeoToolsAdapters;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.SeedOption;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.ui.PlatformGIS;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * AMOEBA Wizard
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AmoebaWizard extends Wizard {
    protected static final Logger LOGGER = Logging.getLogger(AmoebaWizard.class);
    
    public static final String ST = "statistics"; //$NON-NLS-1$

    private AmoebaWizardFirstPage firstPage;

    private AmoebaWizardSecondPage secondPage;

    private org.locationtech.udig.project.internal.Map map;

    private AmoebaParameter param = new AmoebaParameter();

    public AmoebaWizard(IMap map) {
        super.setWindowTitle(Messages.AmoebaWizardDialog_title);
        super.setHelpAvailable(false);
        super.setNeedsProgressMonitor(true);

        this.map = (org.locationtech.udig.project.internal.Map) map;
        this.firstPage = new AmoebaWizardFirstPage(map, param);
        this.secondPage = new AmoebaWizardSecondPage(map, param);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void addPages() {
        addPage(firstPage);
        addPage(secondPage);
    }

    @Override
    public boolean performCancel() {
        return true;
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        final IWizardPage nextPage = super.getNextPage(page);
        if (nextPage instanceof AmoebaWizardSecondPage) {
            ((AmoebaWizardSecondPage) nextPage).updateParameters();
        }
        return nextPage;
    }

    @Override
    public boolean performFinish() {
        PlatformGIS.runInProgressDialog(getWindowTitle(), true, runnalbe, true);

        return false;
    }

    IRunnableWithProgress runnalbe = new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {
            int increment = 10;
            monitor.beginTask(Messages.Task_Running, 100);
            monitor.worked(increment);

            try {
                monitor.setTaskName(String.format(Messages.Task_Executing, getWindowTitle()));
                ToolboxPlugin.log(String.format(Messages.Task_Executing, getWindowTitle()));
                ProgressListener progressListener = GeoToolsAdapters.progress(SubMonitor.convert(
                        monitor, Messages.Task_Internal, increment));

                // check selected features
                if (param.seedOption == SeedOption.Selected
                        && param.outputLayer.getFilter() != Filter.EXCLUDE) {
                    param.seedFilter = param.outputLayer.getFilter();
                }

                // perform
                SpatialAMOEBAOperation operation = new SpatialAMOEBAOperation(progressListener);
                param.cluster = operation.execute(param);

                // finally add layer to current map
                monitor.setTaskName(Messages.Task_AddingLayer);
                MapUtils.addFeaturesToMap(map, param.cluster, Messages.AmoebaWizardDialog_title);
                monitor.worked(increment);
            } catch (Exception e) {
                ToolboxPlugin.log(e.getMessage());
            } finally {
                ToolboxPlugin.log(String.format(Messages.Task_Completed, getWindowTitle()));
                monitor.done();
                MessageDialog.openInformation(getShell(), getWindowTitle(),
                        String.format(Messages.Task_Completed, getWindowTitle()));
            }
        }
    };
}
