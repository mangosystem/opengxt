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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.Messages;

public class TableSelectionWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(TableSelectionWidget.class);

    private Table table;

    private Button btnSelectAll, btnUnSelectAll, btnSwitch;

    public TableSelectionWidget(Table table) {
        this.table = table;
    }

    public void create(final Composite parent, final int style, final int colspan, final int rowspan) {
        composite = new Composite(parent, style);
        composite.setLayout(new GridLayout(3, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, colspan, rowspan));

        btnSelectAll = widget.createButton(composite, Messages.TableSelectionWidget_SelectAll,
                null, 1);
        btnSelectAll.addSelectionListener(selectionListener);

        btnUnSelectAll = widget.createButton(composite,
                Messages.TableSelectionWidget_ClearSelection, null, 1);
        btnUnSelectAll.addSelectionListener(selectionListener);

        btnSwitch = widget.createButton(composite, Messages.TableSelectionWidget_SwitchSelection,
                null, 1);
        btnSwitch.addSelectionListener(selectionListener);
    }

    private SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(btnSelectAll) || widget.equals(btnUnSelectAll)) {
                boolean selectAll = event.widget.equals(btnSelectAll);
                for (TableItem item : table.getItems()) {
                    item.setChecked(selectAll);
                }
            } else if (widget.equals(btnSwitch)) {
                for (TableItem item : table.getItems()) {
                    item.setChecked(!item.getChecked());
                }
            }
        }
    };
}
