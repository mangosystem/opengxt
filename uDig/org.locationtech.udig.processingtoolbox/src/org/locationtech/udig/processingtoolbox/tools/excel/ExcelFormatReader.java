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
import java.util.ArrayList;
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
import org.geotools.process.spatialstatistics.operations.TextColumn;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * ExcelFormatReader.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class ExcelFormatReader {
    protected static final Logger LOGGER = Logging.getLogger(ExcelFormatReader.class);

    private Workbook workbook;

    static final DateFormat dataFormat = new SimpleDateFormat("yyyyMMdd");

    static final DecimalFormat decimalFormat = new DecimalFormat("#.####################");

    static final String prefix = "col_";

    public ExcelFormatReader(File excelFile) {
        this.workbook = getWorkbook(excelFile);
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    private Workbook getWorkbook(File excelFile) {
        Workbook workbook = null;

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(excelFile);
            workbook = WorkbookFactory.create(inputStream);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (InvalidFormatException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(inputStream);
        }

        return workbook;
    }

    public List<String> getWorksheets() {
        List<String> workSheets = new ArrayList<String>();
        if (workbook == null) {
            return workSheets;
        }

        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            workSheets.add(workbook.getSheetAt(index).getSheetName());
        }
        return workSheets;
    }

    public ExcelSheetInfo getSheet(String sheetName, boolean headerFirst, int sampleSize) {
        if (workbook == null) {
            throw new NullPointerException("Invalid workbook file!");
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new NullPointerException(sheetName + " sheet does not exist!");
        }

        // get valid data row
        int firstRow = sheet.getFirstRowNum();
        Row header = sheet.getRow(firstRow);
        while (firstRow <= sheet.getLastRowNum()) {
            if (header.getFirstCellNum() == -1) {
                header = sheet.getRow(++firstRow);
                continue;
            }
            break;
        }

        // build schema
        ExcelSheetInfo sheetInfo = new ExcelSheetInfo(workbook, sheet, sheetName);
        sheetInfo.setHeaderFirst(headerFirst);

        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter(true);
        for (int index = header.getFirstCellNum(); index <= header.getLastCellNum(); index++) {
            TextColumn column = new TextColumn();
            column.setColumnIndex(index);

            String columnName = prefix + index;
            if (headerFirst) {
                Cell cell = sheet.getRow(firstRow).getCell(index);
                columnName = getCellValue(cell, evaluator, formatter);
                if (columnName == null || columnName.isEmpty()) {
                    columnName = prefix + index;
                }
            }

            // set default x, y column
            columnName = columnName.toLowerCase();
            if (columnName.startsWith("x") || columnName.startsWith("lon")) {
                column.setX();
            } else if (columnName.startsWith("y") || columnName.startsWith("lat")) {
                column.setY();
            } else {
                column.setType("String"); // default type
            }
            column.setName(columnName);

            // preview
            int start = headerFirst ? firstRow + 1 : firstRow;
            for (int pos = start, count = 0; count < sampleSize; count++, pos++) {
                Row row = sheet.getRow(pos);
                if (row == null) {
                    continue;
                }

                String val = getCellValue(row.getCell(index), evaluator, formatter);
                column.getSampleValues().add(val);
            }

            sheetInfo.getColumns().add(column);
        }

        return sheetInfo;
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
                value = dataFormat.format(cell.getDateCellValue());
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

    public static class ExcelSheetInfo {

        private Workbook workbook;

        private Sheet sheet;

        private String name;

        private boolean headerFirst = false;

        private List<TextColumn> columns = new ArrayList<TextColumn>();

        private CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;

        private CoordinateReferenceSystem targetCRS = null;

        public ExcelSheetInfo(Workbook workbook, Sheet sheet) {
            this.workbook = workbook;
            this.sheet = sheet;
            this.name = sheet.getSheetName();
        }

        public ExcelSheetInfo(Workbook workbook, Sheet sheet, String name) {
            this(workbook, sheet);
            this.name = name;
        }

        public ExcelSheetInfo(Workbook workbook, Sheet sheet, String name, boolean headerFirst) {
            this(workbook, sheet, name);
            this.headerFirst = headerFirst;
        }

        public Workbook getWorkbook() {
            return workbook;
        }

        public void setWorkbook(Workbook workbook) {
            this.workbook = workbook;
        }

        public Sheet getSheet() {
            return sheet;
        }

        public void setSheet(Sheet sheet) {
            this.sheet = sheet;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isHeaderFirst() {
            return headerFirst;
        }

        public void setHeaderFirst(boolean headerFirst) {
            this.headerFirst = headerFirst;
        }

        public List<TextColumn> getColumns() {
            return columns;
        }

        public void setColumns(List<TextColumn> columns) {
            this.columns = columns;
        }

        public CoordinateReferenceSystem getSourceCRS() {
            return sourceCRS;
        }

        public void setSourceCRS(CoordinateReferenceSystem crs) {
            this.sourceCRS = crs;
        }

        public CoordinateReferenceSystem getTargetCRS() {
            return targetCRS;
        }

        public void setTargetCRS(CoordinateReferenceSystem targetCRS) {
            this.targetCRS = targetCRS;
        }
    }

}