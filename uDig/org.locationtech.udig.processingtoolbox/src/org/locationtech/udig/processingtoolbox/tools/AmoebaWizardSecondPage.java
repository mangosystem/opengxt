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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.QueryBuilderDialog;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.OverlapClusterOption;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.SeedOption;
import org.locationtech.udig.project.IMap;
import org.opengis.filter.Filter;

/**
 * AMOEBA Wizard Second Page
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AmoebaWizardSecondPage extends AmoebaWizardAbstractPage {
    protected static final Logger LOGGER = Logging.getLogger(AmoebaWizardSecondPage.class);

    private Button btnExcFilter;

    private Button chkMaxOne, chkSortDesc, chkExcOnlyCluster, chkEliExclusion, chkEliSingleCell;

    private Button optSeedAll, optSeedSelected, optSeedCustom, optOvrRemove, optOvrAvoid;

    private Combo cboCtExp, cboCtOpr, cboCtVal;

    private Text txtExcFilter;

    protected AmoebaWizardSecondPage(IMap map, AmoebaParameter param) {
        super(Messages.AmoebaWizardSecondPage_title);
        super.setTitle(Messages.AmoebaWizardSecondPage_title);
        super.setDescription(Messages.AmoebaWizardSecondPage_description);

        super.param = param;
        super.map = (org.locationtech.udig.project.internal.Map) map;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        setControl(composite); // Important!

        ExpandBar expandBar = new ExpandBar(composite, SWT.V_SCROLL);
        expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // 0. General
        Composite container = new Composite(expandBar, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        chkMaxOne = wb.createCheckbox(container, Messages.AmoebaWizardSecondPage_UseOnlyMax, EMPTY,
                2);
        chkMaxOne.addSelectionListener(selectionListener);

        chkEliExclusion = wb.createCheckbox(container,
                Messages.AmoebaWizardSecondPage_EliminateExclusion, EMPTY, 2);
        chkEliExclusion.addSelectionListener(selectionListener);

        chkEliSingleCell = wb.createCheckbox(container,
                Messages.AmoebaWizardSecondPage_EliminateSingleCluster, EMPTY, 2);
        chkEliSingleCell.addSelectionListener(selectionListener);

        ExpandItem item0 = new ExpandItem(expandBar, SWT.NONE, 0);
        item0.setText(Messages.AmoebaWizardSecondPage_GeneralOptions);
        item0.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        item0.setControl(container);
        item0.setExpanded(true);

        // 1. Seed
        container = new Composite(expandBar, SWT.NONE);
        container.setLayout(new GridLayout(3, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        optSeedAll = wb.createRadioButton(container, Messages.AmoebaWizardSecondPage_AllCells,
                EMPTY, 1);
        optSeedAll.addSelectionListener(selectionListener);

        optSeedSelected = wb.createRadioButton(container,
                Messages.AmoebaWizardSecondPage_SelectedCells, EMPTY, 1);
        optSeedSelected.addSelectionListener(selectionListener);

        optSeedCustom = wb.createRadioButton(container,
                Messages.AmoebaWizardSecondPage_CustomCells, EMPTY, 1);
        optSeedCustom.addSelectionListener(selectionListener);

        cboCtExp = wb.createCombo(container, 1, false);
        cboCtExp.addModifyListener(modifyListener);

        cboCtOpr = wb.createCombo(container, 1, false);
        cboCtOpr.addModifyListener(modifyListener);

        cboCtVal = wb.createCombo(container, 1, false);
        cboCtVal.addModifyListener(modifyListener);

        chkSortDesc = wb.createCheckbox(container, Messages.AmoebaWizardSecondPage_SortDescending,
                EMPTY, 3);
        chkSortDesc.addSelectionListener(selectionListener);

        ExpandItem item1 = new ExpandItem(expandBar, SWT.NONE, 1);
        item1.setText(Messages.AmoebaWizardSecondPage_SeedOptions);
        item1.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        item1.setControl(container);
        item1.setExpanded(true);

        // 2. Overlap Cluster
        container = new Composite(expandBar, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        optOvrRemove = wb.createRadioButton(container,
                Messages.AmoebaWizardSecondPage_OverlapRemove, EMPTY, 2);
        optOvrRemove.addSelectionListener(selectionListener);

        optOvrAvoid = wb.createRadioButton(container, Messages.AmoebaWizardSecondPage_OverlapAvoid,
                EMPTY, 2);
        optOvrAvoid.addSelectionListener(selectionListener);

        ExpandItem item2 = new ExpandItem(expandBar, SWT.NONE, 2);
        item2.setText(Messages.AmoebaWizardSecondPage_OverlapClusterOptions);
        item2.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        item2.setControl(container);
        item2.setExpanded(true);

        // 3. Exclusion Filter
        container = new Composite(expandBar, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        wb.createLabel(container, Messages.AmoebaWizardSecondPage_ExclusionFilter, EMPTY, 2);
        txtExcFilter = wb.createText(container, EMPTY, 1);
        txtExcFilter.addModifyListener(modifyListener);

        btnExcFilter = wb.createButton(container, EMPTY, EMPTY, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnExcFilter.setImage(helpImage);
        btnExcFilter.addSelectionListener(selectionListener);

        chkExcOnlyCluster = wb.createCheckbox(container,
                Messages.AmoebaWizardSecondPage_ExclusionClusters, EMPTY, 2);
        chkExcOnlyCluster.addSelectionListener(selectionListener);

        ExpandItem item3 = new ExpandItem(expandBar, SWT.NONE, 3);
        item3.setText(Messages.AmoebaWizardSecondPage_ExclusionOptions);
        item3.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        item3.setControl(container);
        item3.setExpanded(true);

        expandBar.setSpacing(8);

        // default settings
        chkMaxOne.setSelection(true);
        chkSortDesc.setSelection(true);
        chkExcOnlyCluster.setSelection(true);
        optOvrAvoid.setSelection(true);
        optSeedCustom.setSelection(true);

        super.setPageComplete(false);
    }

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (widget.equals(txtExcFilter)) {
                try {
                    param.exclusionFilter = ECQL.toFilter(txtExcFilter.getText());
                    txtExcFilter.setBackground(new Color(Display.getCurrent(), 255, 255, 255));
                } catch (CQLException ce) {
                    txtExcFilter.setBackground(new Color(Display.getCurrent(), 255, 255, 200));
                    param.exclusionFilter = null;
                }
            } else if (widget.equals(cboCtExp) || widget.equals(cboCtVal)
                    || widget.equals(cboCtOpr)) {
                if (cboCtExp.getText().length() > 0 && cboCtVal.getText().length() > 0
                        && cboCtOpr.getText().length() > 0) {
                    try {
                        String ecqlPredicate = FIELD.replace("FIELD", param.statField) + cboCtOpr.getText() + cboCtVal.getText(); //$NON-NLS-1$
                        param.seedFilter = ECQL.toFilter(ecqlPredicate);
                    } catch (CQLException ce) {
                        param.seedFilter = Filter.INCLUDE;
                    }
                }
            }
            validate();
        }
    };

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(chkMaxOne)) {
                param.onlyMaxOne = chkMaxOne.getSelection();
            } else if (widget.equals(chkEliExclusion)) {
                param.useExclusionFilter = chkEliExclusion.getSelection();
            } else if (widget.equals(chkEliSingleCell)) {
                param.excludeSingleCulster = chkEliSingleCell.getSelection();
            } else if (widget.equals(chkSortDesc)) {
                param.sortDescending = chkSortDesc.getSelection();
            } else if (widget.equals(chkExcOnlyCluster)) {
                param.excludeFromCluster = chkExcOnlyCluster.getSelection();
            } else if (widget.equals(optSeedAll) && optSeedAll.getSelection()) {
                param.seedOption = SeedOption.All;
            } else if (widget.equals(optSeedSelected) && optSeedSelected.getSelection()) {
                param.seedOption = SeedOption.Selected;
            } else if (widget.equals(optSeedCustom)) {
                boolean selected = optSeedCustom.getSelection();
                cboCtExp.setEnabled(selected);
                cboCtOpr.setEnabled(selected);
                cboCtVal.setEnabled(selected);
                if (selected) {
                    param.seedOption = SeedOption.Custom;
                }
            } else if (widget.equals(optOvrRemove) && optOvrRemove.getSelection()) {
                param.overlapCluster = OverlapClusterOption.Remove;
            } else if (widget.equals(optOvrAvoid) && optOvrAvoid.getSelection()) {
                param.overlapCluster = OverlapClusterOption.Avoid;
            } else if (widget.equals(btnExcFilter)) {
                QueryBuilderDialog dialog = new QueryBuilderDialog(getShell(), param.output);
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    txtExcFilter.setText(dialog.getFilter());
                }
            }
            validate();
        }
    };

    public void updateParameters() {
        cboCtExp.setItems(new String[] { "|" + param.statField + "|" }); //$NON-NLS-1$//$NON-NLS-2$
        cboCtOpr.setItems(mathOperators);
        cboCtVal.setItems(cIntervals);

        cboCtExp.select(0);
        cboCtOpr.select(1);
        cboCtVal.select(2);
    }

    private void validate() {
        boolean valid = param.features != null && param.field != null && param.field.length() > 0;
        super.setPageComplete(valid);
    }
}
