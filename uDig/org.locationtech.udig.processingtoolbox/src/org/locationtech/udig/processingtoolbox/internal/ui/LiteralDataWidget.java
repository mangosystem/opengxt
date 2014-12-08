/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.project.IMap;

/**
 * LiteralData control
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class LiteralDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(LiteralDataWidget.class);

    private IMap map;

    private Text txtLiteral;

    private MenuItem mnuExp, mnuFeature, mnuStatistics;

    public LiteralDataWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        GridLayout layout = new GridLayout(2, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        txtLiteral = widget.createText(composite, null, 1);
        txtLiteral.setData(param.key);
        if (param.sample != null) {
            txtLiteral.setText(param.sample.toString());
        }

        txtLiteral.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                processParams.put(param.key, txtLiteral.getText());
            }
        });

        final Button btnOpen = widget.createButton(composite, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(helpImage);
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);

                // create popup menu
                Shell shell = parent.getShell();
                Menu popupMenu = new Menu(shell, SWT.POP_UP);

                // 1. LiteralDataViewer_ExpressionBuilder = Build Expression...
                mnuExp = new MenuItem(popupMenu, SWT.PUSH);
                mnuExp.setText(Messages.LiteralDataViewer_ExpressionBuilder);
                mnuExp.addSelectionListener(selectionListener);

                // 2. LiteralDataViewer_MultipleFieldsSelection = Select Multiple Fields...
                mnuFeature = new MenuItem(popupMenu, SWT.PUSH);
                mnuFeature.setText(Messages.LiteralDataViewer_MultipleFieldsSelection);
                mnuFeature.addSelectionListener(selectionListener);

                // 3. StatisticsFieldsSelectionDialog
                mnuStatistics = new MenuItem(popupMenu, SWT.PUSH);
                mnuStatistics.setText(Messages.LiteralDataViewer_StatisticsFieldsSelection);
                mnuStatistics.addSelectionListener(selectionListener);

                // location of popup menu
                Control ctrl = (Control) event.widget;
                Point loc = ctrl.getLocation();
                Rectangle rect = ctrl.getBounds();

                Point pos = new Point(loc.x - 1, loc.y + rect.height);
                popupMenu.setLocation(shell.getDisplay().map(ctrl.getParent(), null, pos));
                popupMenu.setVisible(true);
            }
        });

        composite.pack();
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            Shell shell = widget.getDisplay().getActiveShell();
            if (widget.equals(mnuExp)) {
                ExpressionBuilderDialog dialog = new ExpressionBuilderDialog(shell, map, txtLiteral.getText());
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    txtLiteral.setText(dialog.getSelectedValues());
                }
            } else if (widget.equals(mnuFeature)) {
                MultipleFieldsSelectionDialog dialog = new MultipleFieldsSelectionDialog(shell, map);
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    txtLiteral.setText(dialog.getSelectedValues());
                }
            } else if (widget.equals(mnuStatistics)) {
                StatisticsFieldsSelectionDialog dialog = new StatisticsFieldsSelectionDialog(shell, map);
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    txtLiteral.setText(dialog.getSelectedValues());
                }
            }
        }
    };
}
