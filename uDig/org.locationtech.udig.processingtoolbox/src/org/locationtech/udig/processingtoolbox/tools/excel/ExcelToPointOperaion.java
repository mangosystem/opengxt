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
package org.locationtech.udig.processingtoolbox.tools.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.operations.TextColumn;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.tools.excel.ExcelFormatReader.ExcelSheetInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Excel file to point features operation.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class ExcelToPointOperaion extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(ExcelToPointOperaion.class);

    static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    static final DecimalFormat decimalFormat = new DecimalFormat("#.####################");

    static final String SEP = System.getProperty("line.separator");

    private StringBuffer errorBuffer = new StringBuffer();

    public String getError() {
        return errorBuffer.toString();
    }

    public SimpleFeatureCollection execute(File excelFile, String sheetName,
            List<TextColumn> columns, boolean headerFirst, CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) throws IOException {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(excelFile);
            Workbook workbook = WorkbookFactory.create(inputStream);
            return execute(workbook, sheetName, columns, headerFirst, sourceCRS, targetCRS);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (InvalidFormatException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(inputStream);
        }

        return null;
    }

    public SimpleFeatureCollection execute(Workbook workbook, String sheetName,
            List<TextColumn> columns, boolean headerFirst, CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) throws IOException {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new NullPointerException(sheetName + " sheet does not exist!");
        }

        ExcelSheetInfo sheetInfo = new ExcelSheetInfo(workbook, sheet, sheetName);
        sheetInfo.setHeaderFirst(headerFirst);
        sheetInfo.setColumns(columns);
        sheetInfo.setSourceCRS(sourceCRS);
        sheetInfo.setTargetCRS(targetCRS);

        return execute(sheetInfo);
    }

    public SimpleFeatureCollection execute(Workbook workbook, Sheet sheet,
            List<TextColumn> columns, boolean headerFirst, CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) throws IOException {
        ExcelSheetInfo sheetInfo = new ExcelSheetInfo(workbook, sheet);
        sheetInfo.setHeaderFirst(headerFirst);
        sheetInfo.setColumns(columns);
        sheetInfo.setSourceCRS(sourceCRS);
        sheetInfo.setTargetCRS(targetCRS);

        return execute(sheetInfo);
    }

    public SimpleFeatureCollection execute(ExcelSheetInfo sheetInfo) throws IOException {
        SimpleFeatureType schema = buildSchema(sheetInfo);

        MathTransform transform = null;
        if (sheetInfo.getSourceCRS() != null && sheetInfo.getTargetCRS() != null) {
            transform = getMathTransform(sheetInfo.getSourceCRS(), sheetInfo.getTargetCRS());
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(schema);

        FormulaEvaluator evaluator = sheetInfo.getWorkbook().getCreationHelper()
                .createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter(true);

        Sheet sheet = sheetInfo.getSheet();
        int firstRow = sheet.getFirstRowNum();
        Row header = sheet.getRow(firstRow);
        while (firstRow <= sheet.getLastRowNum()) {
            if (header.getFirstCellNum() == -1) {
                header = sheet.getRow(++firstRow);
                continue;
            }
            break;
        }

        try {
            final int startID = sheetInfo.isHeaderFirst() ? firstRow + 1 : firstRow;

            // convert
            for (int rowID = startID; rowID <= sheet.getLastRowNum(); rowID++) {
                Row row = sheet.getRow(rowID);
                if (row == null) {
                    continue;
                }

                Double x = null;
                Double y = null;

                SimpleFeature newFeature = featureWriter.buildFeature();

                for (TextColumn col : sheetInfo.getColumns()) {
                    Cell cell = row.getCell(col.getColumnIndex());
                    Object value = getCellValue(cell, evaluator, formatter);
                    value = Converters.convert(value, col.getBinding());
                    newFeature.setAttribute(col.getName(), value);

                    if (value != null && col.isX()) {
                        x = Converters.convert(value, Double.class);
                    } else if (value != null && col.isY()) {
                        y = Converters.convert(value, Double.class);
                    }
                }

                if (x != null && y != null) {
                    Geometry point = gf.createPoint(new Coordinate(x, y));
                    if (transform != null) {
                        try {
                            point = JTS.transform(point, transform);
                        } catch (MismatchedDimensionException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        } catch (TransformException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        }
                    }

                    newFeature.setDefaultGeometry(point);
                    try {
                        featureWriter.write(newFeature);
                    } catch (IOException e) {
                        errorBuffer.append(rowID);
                        errorBuffer.append(SEP);
                    }
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private SimpleFeatureType buildSchema(ExcelSheetInfo sheetInfo) throws NullPointerException {
        TextColumn xColumn = null;
        TextColumn yColomn = null;
        for (TextColumn col : sheetInfo.getColumns()) {
            if (col.isX()) {
                xColumn = col;
            } else if (col.isY()) {
                yColomn = col;
            }
        }

        if (xColumn == null || yColomn == null) {
            throw new NullPointerException("X or Y Column does not exist!");
        }

        // build schema
        CoordinateReferenceSystem crs = sheetInfo.getTargetCRS() == null ? sheetInfo.getSourceCRS()
                : sheetInfo.getTargetCRS();

        SimpleFeatureType schema = null;
        schema = FeatureTypes.getDefaultType("ExcelToPoint", Point.class, crs);
        for (TextColumn col : sheetInfo.getColumns()) {
            if (col.getBinding().isAssignableFrom(String.class)) {
                schema = FeatureTypes.add(schema, col.getName(), col.getBinding(), col.getLength());
            } else {
                schema = FeatureTypes.add(schema, col.getName(), col.getBinding());
            }
        }

        return schema;
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }

        String value = "";
        switch (cell.getCellTypeEnum()) {
        case _NONE:
        case BLANK:
        case ERROR:
            value = "";
            break;
        case BOOLEAN:
            value = String.valueOf(cell.getBooleanCellValue());
            break;
        case STRING:
            value = formatter.formatCellValue(cell).trim();
            break;
        case NUMERIC:
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                value = dateFormat.format(cell.getDateCellValue());
            } else {
                value = decimalFormat.format(cell.getNumericCellValue());
            }
            break;
        case FORMULA:
            try {
                value = formatter.formatCellValue(cell, evaluator);
            } catch (Exception ee) {
                value = "";
            }
            break;
        }
        return value;
    }

    private MathTransform getMathTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) {
        if (sourceCRS == null || targetCRS == null) {
            LOGGER.log(Level.WARNING,
                    "Input CoordinateReferenceSystem is Unknown Coordinate System!");
            return null;
        }

        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            LOGGER.log(Level.WARNING, "Input and Output Coordinate Reference Systems are equal!");
            return null;
        }

        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
        } catch (FactoryException e1) {
            LOGGER.log(Level.WARNING, e1.getMessage(), 1);
        }

        return transform;
    }
}
