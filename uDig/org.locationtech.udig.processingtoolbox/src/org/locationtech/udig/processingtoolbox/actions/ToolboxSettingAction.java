/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.locationtech.udig.processingtoolbox.ToolboxView;

/**
 * Toolbox Setting Action
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class ToolboxSettingAction implements IViewActionDelegate {

    private IViewPart view;

    @Override
    public void init(IViewPart view) {
        this.view = view;
    }

    @Override
    public void run(IAction action) {
        if (view instanceof ToolboxView) {
            final ToolboxView tbView = (ToolboxView) view;
            final Shell shell = tbView.getSite().getShell();
            MessageDialog.openInformation(shell, "Cancelled", tbView.getTitle()); //$NON-NLS-1$
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // TODO Auto-generated method stub
    }

}
