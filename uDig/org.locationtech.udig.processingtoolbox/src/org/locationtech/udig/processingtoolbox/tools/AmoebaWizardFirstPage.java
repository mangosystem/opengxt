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

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.StatisticsFeaturesProcess;
import org.geotools.process.spatialstatistics.autocorrelation.LocalGStatisticOperation;
import org.geotools.process.spatialstatistics.autocorrelation.LocalLeesSOperation;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.Style;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.CriteriaType;
import org.locationtech.udig.processingtoolbox.tools.AmoebaParameter.SAAlgorithmType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;

/**
 * AMOEBA Wizard First Page
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AmoebaWizardFirstPage extends AmoebaWizardAbstractPage {
    protected static final Logger LOGGER = Logging.getLogger(AmoebaWizardFirstPage.class);

    private Combo cboAlgorithm, cboLayer, cboField, cboConcept, cboStandard, cboDistance;

    private Combo cboStatField, cboCtExp, cboCtOpr, cboCtVal;

    private ExpandItem eiStat, eiCriteria;

    private Spinner spnDistance;

    private Button btnCalculate, optCtMax, optCtMin, optCtUser;

    private Text txtCtDesc;

    private Group grouitCt;

    protected AmoebaWizardFirstPage(IMap map, AmoebaParameter param) {
        super(Messages.AmoebaWizardFirstPage_title);
        super.setTitle(Messages.AmoebaWizardFirstPage_title);
        super.setDescription(Messages.AmoebaWizardFirstPage_description);

        super.param = param;
        super.map = (org.locationtech.udig.project.internal.Map) map;
    }

    @Override
    public void createControl(Composite parent) {
        Image img = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        setControl(composite); // Important!

        ExpandBar expandBar = new ExpandBar(composite, SWT.V_SCROLL);
        expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // 1. Base Statistics
        Composite container = new Composite(expandBar, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_Algorithm, EMPTY, img, 1);
        cboAlgorithm = wb.createCombo(container, 1, true);
        cboAlgorithm.addModifyListener(modifyListener);

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_Layer, EMPTY, img, 1);
        cboLayer = wb.createCombo(container, 1, true);
        cboLayer.addModifyListener(modifyListener);

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_Field, EMPTY, img, 1);
        cboField = wb.createCombo(container, 1, true);
        cboField.addModifyListener(modifyListener);

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_Conceptualization, EMPTY, img, 1);
        cboConcept = wb.createCombo(container, 1, true);
        cboConcept.addModifyListener(modifyListener);

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_DistanceMethod, EMPTY, img, 1);
        cboDistance = wb.createCombo(container, 1, true);
        cboDistance.addModifyListener(modifyListener);

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_Standardization, EMPTY, img, 1);
        cboStandard = wb.createCombo(container, 1, true);
        cboStandard.addModifyListener(modifyListener);

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_DistanceBand, EMPTY, img, 1);
        spnDistance = wb.createSpinner(container, 0, 0, Integer.MAX_VALUE, 2, 1000, 10000, 1);
        spnDistance.addModifyListener(modifyListener);

        wb.createLabel(container, EMPTY, EMPTY, 1);
        btnCalculate = wb.createButton(container,
                Messages.AmoebaWizardFirstPage_CalcuateStatistics, EMPTY, 1);
        btnCalculate.addSelectionListener(selectionListener);

        eiStat = new ExpandItem(expandBar, SWT.NONE, 0);
        eiStat.setText(Messages.AmoebaWizardFirstPage_LocalStatistics);
        eiStat.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        eiStat.setControl(container);
        eiStat.setExpanded(true);

        // 2. Clustering Criteria
        container = new Composite(expandBar, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        wb.createLabel(container, Messages.AmoebaWizardFirstPage_ClusteringField, EMPTY, img, 1);
        cboStatField = wb.createCombo(container, 1, true);
        cboStatField.addModifyListener(modifyListener);

        grouitCt = wb.createGroup(container, Messages.AmoebaWizardFirstPage_GroupTitle, false, 2,
                0, 3);
        optCtMax = wb.createRadioButton(grouitCt, Messages.AmoebaWizardFirstPage_Maximization,
                EMPTY, 1);
        optCtMax.addSelectionListener(selectionListener);

        optCtMin = wb.createRadioButton(grouitCt, Messages.AmoebaWizardFirstPage_Minimization,
                EMPTY, 1);
        optCtMin.addSelectionListener(selectionListener);

        optCtUser = wb.createRadioButton(grouitCt, Messages.AmoebaWizardFirstPage_CustomCriteria,
                EMPTY, 1);
        optCtUser.addSelectionListener(selectionListener);

        cboCtExp = wb.createCombo(grouitCt, 1, false);
        cboCtExp.addModifyListener(modifyListener);

        cboCtOpr = wb.createCombo(grouitCt, 1, false);
        cboCtOpr.addModifyListener(modifyListener);

        cboCtVal = wb.createCombo(grouitCt, 1, false);
        cboCtVal.addModifyListener(modifyListener);

        txtCtDesc = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData txtCtLayout = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
        txtCtLayout.heightHint = 200;
        txtCtDesc.setLayoutData(txtCtLayout);

        optCtUser.setSelection(true);
        grouitCt.setEnabled(false);

        eiCriteria = new ExpandItem(expandBar, SWT.NONE, 1);
        eiCriteria.setText(Messages.AmoebaWizardFirstPage_ClusteringCriteria);
        eiCriteria.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        eiCriteria.setControl(container);
        eiCriteria.setExpanded(false);

        expandBar.setSpacing(8);

        // fill combo
        cboAlgorithm.setItems(algorithm);
        cboAlgorithm.select(0);
        fillLayers(map, cboLayer, VectorLayerType.ALL);
        fillEnum(cboConcept, SpatialConcept.class, 5);
        fillEnum(cboDistance, DistanceMethod.class, 0);
        fillEnum(cboStandard, StandardizationMethod.class, 1);

        super.setPageComplete(false);
    }

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (widget.equals(cboLayer)) {
                ILayer inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer != null) {
                    param.features = MapUtils.getFeatures(inputLayer);
                    fillFields(cboField, param.features.getSchema(), FieldType.Number);
                }
            } else if (widget.equals(cboAlgorithm)) {
                int selection = cboAlgorithm.getSelectionIndex();
                param.algorithm = selection == 0 ? SAAlgorithmType.GetisOrderGiStar
                        : SAAlgorithmType.LeesSiStar;
            } else if (widget.equals(cboField)) {
                String field = cboField.getText();
                param.field = field.length() > 0 ? field : null;
            } else if (widget.equals(cboConcept)) {
                if (cboConcept.getText().length() > 0) {
                    param.spatialConcept = SpatialConcept.valueOf(cboConcept.getText());
                }
            } else if (widget.equals(cboDistance)) {
                if (cboDistance.getText().length() > 0) {
                    param.distanceMethod = DistanceMethod.valueOf(cboDistance.getText());
                }
            } else if (widget.equals(cboStandard)) {
                if (cboStandard.getText().length() > 0) {
                    param.standardization = StandardizationMethod.valueOf(cboStandard.getText());
                }
            } else if (widget.equals(cboCtExp) || widget.equals(cboCtVal)
                    || widget.equals(cboCtOpr)) {
                if (cboCtExp.getText().length() > 0 && cboCtVal.getText().length() > 0
                        && cboCtOpr.getText().length() > 0) {
                    try {
                        String ecqlPredicate = FIELD.replace("FIELD", AmoebaWizard.ST) + cboCtOpr.getText() + cboCtVal.getText(); //$NON-NLS-1$
                        param.criteriaFilter = ECQL.toFilter(ecqlPredicate);
                    } catch (CQLException ce) {
                        param.criteriaFilter = null;
                    }
                }
            } else if (widget.equals(spnDistance)) {
                param.searchDistance = wb.fromSpinnerValue(spnDistance, spnDistance.getSelection());
            } else if (widget.equals(cboStatField)) {
                String field = cboStatField.getText();
                if (field.isEmpty() || field.equals(param.statField)) {
                    return;
                }

                param.statField = field;
                grouitCt.setEnabled(param.statField != null);

                BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                    @Override
                    public void run() {
                        DataStatisticsResult ret = StatisticsFeaturesProcess.process(param.output,
                                param.statField, new NullProgressListener());
                        txtCtDesc.setText(ret.toString());

                        cboCtExp.setItems(new String[] { "|" + AmoebaWizard.ST + "|" }); //$NON-NLS-1$//$NON-NLS-2$
                        cboCtOpr.setItems(mathOperators);
                        cboCtVal.setItems(cIntervals);

                        cboCtExp.select(0);
                        cboCtOpr.select(1);
                        cboCtVal.select(2);
                    }
                });
            }

            validate();
        }
    };

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(btnCalculate)) {
                BusyIndicator.showWhile(Display.getCurrent(), runnableLocalIndex);
            } else if (widget.equals(optCtMax) && optCtMax.getSelection()) {
                param.criteriaType = CriteriaType.Maximization;
            } else if (widget.equals(optCtMin) && optCtMin.getSelection()) {
                param.criteriaType = CriteriaType.Minimization;
            } else if (widget.equals(optCtUser)) {
                boolean selected = optCtUser.getSelection();
                cboCtExp.setEnabled(selected);
                cboCtOpr.setEnabled(selected);
                cboCtVal.setEnabled(selected);
                if (selected) {
                    param.criteriaType = CriteriaType.Custom;
                }
            }
            validate();
        }
    };

    Runnable runnableLocalIndex = new Runnable() {
        @Override
        public void run() {
            try {
                if (param.algorithm == SAAlgorithmType.LeesSiStar) {
                    LocalLeesSOperation process = new LocalLeesSOperation();
                    process.setSpatialConceptType(param.spatialConcept);
                    process.setDistanceType(param.distanceMethod);
                    process.setStandardizationType(param.standardization);
                    process.setSelfNeighbors(Boolean.TRUE);
                    process.setDistanceBand(param.searchDistance);
                    param.output = process.execute(param.features, param.field);
                    param.matrix = process.getSwMatrix();
                } else {
                    LocalGStatisticOperation process = new LocalGStatisticOperation();
                    process.setSpatialConceptType(param.spatialConcept);
                    process.setDistanceType(param.distanceMethod);
                    process.setStandardizationType(param.standardization);
                    process.setSelfNeighbors(Boolean.TRUE);
                    process.setDistanceBand(param.searchDistance);
                    param.output = process.execute(param.features, param.field);
                    param.matrix = process.getSwMatrix();
                }

                fillFields(cboStatField, param.output.getSchema(), FieldType.Number);

                String field = IDX_FIELDS[param.algorithm.ordinal()];
                cboStatField.setText(field);

                // add to map
                SSStyleBuilder ssBuilder = new SSStyleBuilder(param.output.getSchema());
                ssBuilder.setOpacity(0.8f);
                Style style = ssBuilder.getZScoreStdDevStyle(field);

                param.outputLayer = MapUtils.addFeaturesToMap(map, param.output,
                        algorithm[param.algorithm.ordinal()], style);
            } catch (ProcessException | IOException e) {
                param.output = null;
                cboStatField.removeAll();
                ToolboxPlugin.log(e);
            } finally {
                eiStat.setExpanded(param.output == null);
                eiCriteria.setExpanded(param.output != null);
            }
        }
    };

    private void validate() {
        boolean valid = param.features != null && param.field != null;
        btnCalculate.setEnabled(valid);

        boolean completed = valid && param.output != null && param.statField != null
                && param.criteriaFilter != null;
        super.setPageComplete(completed);
    }
}
