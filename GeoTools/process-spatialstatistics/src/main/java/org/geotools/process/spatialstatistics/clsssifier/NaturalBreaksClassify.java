/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.clsssifier;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.DataHistogram;
import org.geotools.process.spatialstatistics.core.HistogramFeatures;
import org.geotools.process.spatialstatistics.core.HistogramGridCoverage;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.util.logging.Logging;

/**
 * The NaturalBreaks uses a statistical formula to determine natural clusters of attribute values. The formula is known as Jenk's method. This
 * attempts to minimize the variance within a class and to maximize the variance between classes.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NaturalBreaksClassify extends DataClassify {
    protected static final Logger LOGGER = Logging.getLogger(NaturalBreaksClassify.class);

    static final String sMethodName = "Natural Breaks(Jenks)";

    @Override
    public String getMethodName() {
        return sMethodName;
    }

    @Override
    public Double[] classify(double[] arrayValues, int[] arrayFrequencies, int binCount) {
        initializeClassBreaks(binCount);

        if (arrayValues == null || arrayFrequencies == null) {
            return classBreaks;
        }

        double sumOfVal = 0.0;
        int count = 0;
        List<ValuePair> valuePairs = new ArrayList<ValuePair>();
        for (int index = 0; index < arrayFrequencies.length; index++) {
            ValuePair vp = new ValuePair(arrayValues[index], arrayFrequencies[index]);
            valuePairs.add(vp);

            count += vp.frequency;
            sumOfVal += vp.value * vp.frequency;
        }

        calculateJenksBreaks(valuePairs, sumOfVal / count, binCount);

        return classBreaks;
    }

    @Override
    public Double[] classify(SimpleFeatureCollection inputFeatures, String propertyName,
            int binCount) {
        initializeClassBreaks(binCount);

        DataHistogram histo = new HistogramFeatures();
        if (histo.calculateHistogram(inputFeatures, propertyName)) {
            final double mean = histo.getMean();
            final double[] arrayValues = histo.getArrayValues();
            final int[] arrayFrequencies = histo.getArrayFrequencies();

            List<ValuePair> valuePairs = new ArrayList<ValuePair>();
            for (int index = 0; index < arrayFrequencies.length; index++) {
                valuePairs.add(new ValuePair(arrayValues[index], arrayFrequencies[index]));
            }

            calculateJenksBreaks(valuePairs, mean, binCount);
        }

        return classBreaks;
    }

    @Override
    public Double[] classify(GridCoverage2D inputGc, int bandIndex, int binCount) {
        initializeClassBreaks(binCount);

        final double noDataValue = RasterHelper.getNoDataValue(inputGc);
        DataHistogram histo = new HistogramGridCoverage();
        if (histo.calculateHistogram(inputGc, bandIndex, noDataValue)) {
            final double mean = histo.getMean();
            final double[] arrayValues = histo.getArrayValues();
            final int[] arrayFrequencies = histo.getArrayFrequencies();

            List<ValuePair> valuePairs = new ArrayList<ValuePair>();
            for (int index = 0; index < arrayFrequencies.length; index++) {
                valuePairs.add(new ValuePair(arrayValues[index], arrayFrequencies[index]));
            }

            calculateJenksBreaks(valuePairs, mean, binCount);
        }

        return classBreaks;
    }

    public boolean calculateJenksBreaks(List<ValuePair> valuePairs, double mean, int numOfClass) {
        if (numOfClass >= valuePairs.size()) {
            classBreaks = new Double[valuePairs.size()];
            for (int index = 0; index < valuePairs.size(); index++) {
                classBreaks[index] = valuePairs.get(index).value;
            }
            return true;
        }

        // 1. calculate mean & the sum of squared deviations from the array mean (SDAM).
        double[] ldSDAM = new double[1];

        double[] finalGVF = new double[1];
        double[] newGVF = new double[1];

        // Calculate the sum of squared deviations from the array mean (SDAM).
        ldSDAM[0] = calculateSDAM(valuePairs, mean);

        int[] initClass = new int[numOfClass - 1];
        int numOfElement = valuePairs.size() / numOfClass;
        for (int index = 0; index < initClass.length; index++) {
            if (index == 0) {
                initClass[index] = numOfElement - 1;
            } else {
                initClass[index] = initClass[index - 1] + numOfElement;
            }
        }

        DataClass[] ldaSDCM_Parciales = new DataClass[initClass.length + 1];
        DataClass[] ldaSDCM_Validos = new DataClass[initClass.length + 1];

        if (!calculateGVF(valuePairs, ldaSDCM_Parciales, initClass, ldSDAM[0], finalGVF, -1,
                false)) {
            calculateJenksBreaks(valuePairs, mean, valuePairs.size());
            return false;
        }

        ldaSDCM_Validos = getArray(ldaSDCM_Parciales);

        boolean lbMoverADerecha;
        boolean lbMoverAIzquierda;
        boolean lbIntentarDesplazamiento;
        int llIndiceRupturaOriginal;

        final int maxIteration = 100;
        double currentGVF = finalGVF[0];

        int i;
        for (int iter = 1; iter <= maxIteration; iter++) {
            for (i = 0; i < initClass.length; i++) {
                lbMoverADerecha = false;
                lbMoverAIzquierda = false;
                llIndiceRupturaOriginal = initClass[i];
                ldaSDCM_Validos = getArray(ldaSDCM_Parciales);

                lbIntentarDesplazamiento = false;

                if (i == (initClass.length - 1)) {
                    if ((initClass[i] + 1) < valuePairs.size()) {
                        lbIntentarDesplazamiento = true;
                    }
                } else {
                    if ((initClass[i] + 1) < initClass[i + 1]) {
                        lbIntentarDesplazamiento = true;
                    }
                }

                if (lbIntentarDesplazamiento) {
                    initClass[i] = initClass[i] + 1;

                    if (!calculateGVF(valuePairs, ldaSDCM_Parciales, initClass, ldSDAM[0], newGVF,
                            i, false)) {
                        return false;
                    }

                    if (newGVF[0] > finalGVF[0]) {
                        lbMoverADerecha = true;
                        ldaSDCM_Validos = getArray(ldaSDCM_Parciales);
                    } else {
                        initClass[i] = llIndiceRupturaOriginal;
                        ldaSDCM_Parciales = getArray(ldaSDCM_Validos);
                    }
                }

                lbIntentarDesplazamiento = false;

                if (!lbMoverADerecha) {
                    if (i == 0) {
                        if ((initClass[i] - 1) >= 0) {
                            lbIntentarDesplazamiento = true;
                        }
                    } else {
                        if ((initClass[i] - 1) > initClass[i - 1]) {
                            lbIntentarDesplazamiento = true;
                        }
                    }
                }

                if (lbIntentarDesplazamiento) {
                    initClass[i] = initClass[i] - 1;

                    if (!calculateGVF(valuePairs, ldaSDCM_Parciales, initClass, ldSDAM[0], newGVF,
                            i, true)) {
                        return false;
                    }

                    if (newGVF[0] > finalGVF[0]) {
                        lbMoverAIzquierda = true;
                        ldaSDCM_Validos = getArray(ldaSDCM_Parciales);
                    } else {
                        initClass[i] = llIndiceRupturaOriginal;
                        ldaSDCM_Parciales = getArray(ldaSDCM_Validos);
                    }
                }

                lbIntentarDesplazamiento = false;

                if (lbMoverAIzquierda || lbMoverADerecha) {
                    finalGVF[0] = newGVF[0];

                    boolean exit = false;

                    while (!exit) {
                        llIndiceRupturaOriginal = initClass[i];

                        if (lbMoverADerecha) {
                            if (i == (initClass.length - 1)) {
                                if ((initClass[i] + 1) >= valuePairs.size()) {
                                    exit = true;
                                }
                            } else {
                                if ((initClass[i] + 1) >= initClass[i + 1]) {
                                    exit = true;
                                }
                            }

                            initClass[i] = initClass[i] + 1;
                        } else {
                            if (i == 0) {
                                if ((initClass[i] - 1) < 0) {
                                    exit = true;
                                }
                            } else {
                                if ((initClass[i] - 1) <= initClass[i - 1]) {
                                    exit = true;
                                }
                            }
                            initClass[i] = initClass[i] - 1;
                        }

                        if (!calculateGVF(valuePairs, ldaSDCM_Parciales, initClass, ldSDAM[0],
                                newGVF, i, lbMoverAIzquierda)) {
                            return false;
                        }

                        if (newGVF[0] < finalGVF[0]) {
                            initClass[i] = llIndiceRupturaOriginal;
                            ldaSDCM_Parciales = getArray(ldaSDCM_Validos);
                            exit = true;
                        } else {
                            finalGVF[0] = newGVF[0];
                            ldaSDCM_Validos = getArray(ldaSDCM_Parciales);
                        }
                    }
                }
            }

            if (finalGVF[0] <= currentGVF) {
                i = maxIteration + 1;
            }

            currentGVF = finalGVF[0];
        }

        initializeClassBreaks(numOfClass);

        classBreaks[0] = Double.valueOf(valuePairs.get(0).value);
        classBreaks[classBreaks.length - 1] = Double
                .valueOf(valuePairs.get(valuePairs.size() - 1).value);

        double[] mdaValInit = new double[initClass.length];
        for (int index = 0; index < mdaValInit.length; index++) {
            if (initClass[index] == -1) {
                initClass[index] = 1;
            }

            if (initClass[index] > valuePairs.size() - 1) {
                initClass[index] = valuePairs.size() - 1;
            }

            classBreaks[index + 1] = Double.valueOf(valuePairs.get(initClass[index]).value);

            if ((initClass[index] + 1) < valuePairs.size()) {
                mdaValInit[index] = valuePairs.get(initClass[index] + 1).value;
            } else {
                mdaValInit[index] = valuePairs.get(initClass[index]).value;
            }
        }

        ldaSDCM_Validos = null;
        ldaSDCM_Parciales = null;

        return true;
    }

    private DataClass[] getArray(DataClass[] dataClass) {
        DataClass[] aux = new DataClass[dataClass.length];

        for (int i = 0; i < dataClass.length; i++) {
            aux[i] = new DataClass();
            aux[i].Mean = dataClass[i].Mean;
            aux[i].Count = dataClass[i].Count;
            aux[i].SDCM = dataClass[i].SDCM;
            aux[i].SumOfSquaredTotal = dataClass[i].SumOfSquaredTotal;
            aux[i].SumOfTotal = dataClass[i].SumOfTotal;
        }

        return aux;
    }

    private double calculateSDAM(List<ValuePair> valuePairs, double mean) {
        double rdSDAM = 0;
        for (ValuePair vp : valuePairs) {
            rdSDAM += Math.pow((vp.value - mean), 2) * vp.frequency;
        }
        return rdSDAM;
    }

    private boolean calculateGVF(List<ValuePair> valuePairs, DataClass[] dataClass,
            int[] rlaIndicesRuptura, double vdSDAM, double[] rdGVF, int vlIndiceRupturaActual,
            boolean vbDesplazAIzquierda) {
        int i;

        if (vlIndiceRupturaActual == -1) {
            for (i = 0; i < rlaIndicesRuptura.length; i++) {
                if (i == 0) {
                    if (!getDataClass(valuePairs, 0, rlaIndicesRuptura[i], dataClass, i)) {
                        return false;
                    }
                } else {
                    if (!getDataClass(valuePairs, rlaIndicesRuptura[i - 1] + 1,
                            rlaIndicesRuptura[i], dataClass, i)) {
                        return false;
                    }
                }
            }

            if (!getDataClass(valuePairs, rlaIndicesRuptura[rlaIndicesRuptura.length - 1] + 1,
                    valuePairs.size() - 1, dataClass, dataClass.length - 1)) {
                return false;
            }
        } else {
            i = vlIndiceRupturaActual;

            if (vbDesplazAIzquierda) {
                if (!recalculateDataClass(dataClass, i, valuePairs, rlaIndicesRuptura[i] + 1,
                        vdSDAM, false)) {
                    return false;
                }

                if (!recalculateDataClass(dataClass, i + 1, valuePairs, rlaIndicesRuptura[i] + 1,
                        vdSDAM, true)) {
                    return false;
                }
            } else {
                if (!recalculateDataClass(dataClass, i, valuePairs, rlaIndicesRuptura[i], vdSDAM,
                        true)) {
                    return false;
                }

                if (!recalculateDataClass(dataClass, i + 1, valuePairs, rlaIndicesRuptura[i],
                        vdSDAM, false)) {
                    return false;
                }
            }
        }

        double ldSDCM_aux = 0;
        for (i = 0; i < dataClass.length; i++) {
            ldSDCM_aux += dataClass[i].SDCM;
        }

        rdGVF[0] = (vdSDAM - ldSDCM_aux) / vdSDAM;

        return true;
    }

    private boolean getDataClass(List<ValuePair> valuePairs, int limitInf, int limitSup,
            DataClass[] dataClass, int clsIndex) {
        if (limitInf < 0) {
            return false;
        }

        if (limitSup > valuePairs.size()) {
            return false;
        }

        if (limitSup < limitInf) {
            return false;
        }

        dataClass[clsIndex] = new DataClass();

        for (int i = limitInf; i < (limitSup + 1); i++) {
            dataClass[clsIndex].Count += valuePairs.get(i).frequency;
            dataClass[clsIndex].SumOfTotal += valuePairs.get(i).getMultiValue();
            dataClass[clsIndex].SumOfSquaredTotal += (Math.pow(valuePairs.get(i).getMultiValue(),
                    2));
        }

        dataClass[clsIndex].Mean = dataClass[clsIndex].SumOfTotal / dataClass[clsIndex].Count;
        dataClass[clsIndex].SDCM = (dataClass[clsIndex].SumOfSquaredTotal)
                - (2 * dataClass[clsIndex].Mean * dataClass[clsIndex].SumOfTotal)
                + (dataClass[clsIndex].Count * Math.pow(dataClass[clsIndex].Mean, 2));

        return true;
    }

    private boolean recalculateDataClass(DataClass[] dataClass, int idxCls,
            List<ValuePair> valuePairs, int idxVp, double vdSDAM, boolean vbAnyadir) {
        try {
            if (idxVp > valuePairs.size() - 1) {
                return true;
            }

            final double multi = valuePairs.get(idxVp).getMultiValue();
            final int frequency = valuePairs.get(idxVp).frequency;

            if (vbAnyadir) {
                dataClass[idxCls].SumOfTotal += multi;
                dataClass[idxCls].SumOfSquaredTotal += Math.pow(multi, 2);
                dataClass[idxCls].Count += frequency;
            } else {
                dataClass[idxCls].SumOfTotal -= multi;
                dataClass[idxCls].SumOfSquaredTotal -= Math.pow(multi, 2);
                dataClass[idxCls].Count -= frequency;
            }

            if (dataClass[idxCls].Count <= 0) {
                dataClass[idxCls].Count = 1;
            }

            dataClass[idxCls].Mean = dataClass[idxCls].SumOfTotal / dataClass[idxCls].Count;

            dataClass[idxCls].SDCM = (dataClass[idxCls].SumOfSquaredTotal)
                    - (2 * dataClass[idxCls].Mean * dataClass[idxCls].SumOfTotal)
                    + (dataClass[idxCls].Count * Math.pow(dataClass[idxCls].Mean, 2));
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    final class DataClass {
        public long Count;

        public double Mean;

        public double SumOfTotal;

        public double SumOfSquaredTotal;

        public double SDCM;
    }

    final class ValuePair {
        public double value;

        public int frequency;

        public double getMultiValue() {
            return value * frequency;
        }

        public ValuePair() {
        }

        public ValuePair(double value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }
    }
}
