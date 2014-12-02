/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Localization
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.locationtech.udig.processingtoolbox.internal.messages"; //$NON-NLS-1$

    public static String ToolboxView_workspace;
    public static String ToolboxView_GeoTools;
    
    public static String SettingsDialog_title;
    public static String SettingsDialog_general;
    public static String SettingsDialog_UseLog;
    public static String SettingsDialog_advanced; 

    public static String Task_AddingLayer;
    public static String Task_Completed;
    public static String Task_ParameterRequired;
    public static String Task_ConfirmErrorFile;
    public static String Task_CheckFile;
    public static String Task_Executing;
    public static String Task_Internal;
    public static String Task_Running;
    public static String Task_WritingResult;

    public static String General_Cancelled;
    public static String General_Error;
    public static String General_ErrorOccurred;
    
    public static String ExpressionBuilderDialog_title;
    public static String ExpressionBuilderDialog_Clear;
    public static String ExpressionBuilderDialog_Test;
    public static String ExpressionBuilderDialog_Layer_Functions;
    public static String ExpressionBuilderDialog_Layer;
    public static String ExpressionBuilderDialog_Fields;
    public static String ExpressionBuilderDialog_Functions;
    public static String ExpressionBuilderDialog_Operators;
    public static String ExpressionBuilderDialog_Expression;

    public static String ExtentSelection_currentextent;
    public static String ExtentSelection_fullextent;
    public static String ExtentSelection_layer;
    public static String ExtentSelection_layerextent;
    public static String ExtentSelection_message;
    public static String ExtentSelection_title;
    
    public static String BoundingBoxViewer_CurrentMapExtent;
    public static String BoundingBoxViewer_CurrentMapFullExtent;
    public static String BoundingBoxViewer_LayerExtent;

    public static String CrsViewer_CRSDialog;
    public static String CrsViewer_LayerCRS;
    public static String CrsViewer_MapCRS;

    public static String OutputDataViewer_folderdialog;
    public static String OutputDataViewer_outputdata;
    public static String OutputDataViewer_outputlocation;
    public static String OutputDataViewer_selectdata;
    
    public static String ProcessInformation_Others;

    public static String ProcessExecutionDialog_deletefailed;
    public static String ProcessExecutionDialog_optional;
    public static String ProcessExecutionDialog_overwriteconfirm;
    public static String ProcessExecutionDialog_requiredparam;
    public static String ProcessExecutionDialog_tabhelp;
    public static String ProcessExecutionDialog_taboutput;
    public static String ProcessExecutionDialog_tabparameters;
    public static String ProcessExecutionDialog_Yes;
    public static String ProcessExecutionDialog_No;
    
    public static String ProcessDescriptor_Input_Parameters;
    public static String ProcessDescriptor_Output_Parameters;
    public static String ProcessDescriptor_General_Information;
    public static String ProcessDescriptor_Product;
    public static String ProcessDescriptor_Home;
    public static String ProcessDescriptor_Parameter;
    public static String ProcessDescriptor_Explanation;
    public static String ProcessDescriptor_Required;
    public static String ProcessDescriptor_Author;
    public static String ProcessDescriptor_Document;
    public static String ProcessDescriptor_Contact;
    public static String ProcessDescriptor_Online_Help;
    public static String ProcessDescriptor_Team_Blog;
    
    public static String QueryDialog_title;
    public static String QueryDialog_Clear;
    public static String QueryDialog_Test;
    public static String QueryDialog_Layer;
    public static String QueryDialog_Fields;
    public static String QueryDialog_Values;
    public static String QueryDialog_Sample;
    public static String QueryDialog_All;
    public static String QueryDialog_Operators;
    public static String QueryDialog_SQL_where_clause;
    
    public static String GeometryPickerDialog_title;
    public static String GeometryPickerDialog_Layer;
    public static String GeometryPickerDialog_Feature;
    public static String GeometryPickerDialog_WKT;
    public static String GeometryPickerDialog_Clipboard;
    public static String GeometryPickerDialog_AddLayer;
    
    public static String GeometryViewer_MapCenter;
    public static String GeometryViewer_MapExtent;
    public static String GeometryViewer_GeometryFromFeatures;
    
    public static String MultipleFieldsSelectionDialog_title;
    public static String MultipleFieldsSelectionDialog_Layer;
    public static String MultipleFieldsSelectionDialog_Fields;
    public static String MultipleFieldsSelectionDialog_SelectedFields;
    public static String MultipleFieldsSelectionDialog_SelectAll;
    public static String MultipleFieldsSelectionDialog_SwitchSelect;
    public static String MultipleFieldsSelectionDialog_Clear;
    
    public static String LiteralDataViewer_ExpressionBuilder;
    public static String LiteralDataViewer_MultipleFieldsSelection;
    public static String LiteralDataViewer_StatisticsFieldsSelection;
    
    public static String NumberDataViewer_GetArea;
    
    public static String StatisticsFieldsSelectionDialog_title;
    public static String StatisticsFieldsSelectionDialog_Layer;
    public static String StatisticsFieldsSelectionDialog_Fields;
    public static String StatisticsFieldsSelectionDialog_Add;
    public static String StatisticsFieldsSelectionDialog_Delete;
    public static String StatisticsFieldsSelectionDialog_TargetField;
    public static String StatisticsFieldsSelectionDialog_StatisticsType;
    public static String StatisticsFieldsSelectionDialog_WarningDuplicate;
    
    public static String TextfileToPointDialog_title;
    public static String TextfileToPointDialog_description;
    public static String TextfileToPointDialog_Textfile;
    public static String TextfileToPointDialog_CRS;
    public static String TextfileToPointDialog_Encoding;
    public static String TextfileToPointDialog_Schema;
    public static String TextfileToPointDialog_FieldNameSetting;
    public static String TextfileToPointDialog_ColumnFirst;
    public static String TextfileToPointDialog_Delimiters;
    public static String TextfileToPointDialog_Tab;
    public static String TextfileToPointDialog_Semicolon;
    public static String TextfileToPointDialog_Colon;
    public static String TextfileToPointDialog_Space;
    public static String TextfileToPointDialog_Custom;
    public static String TextfileToPointDialog_FieldName;
    public static String TextfileToPointDialog_FieldType;
    public static String TextfileToPointDialog_FieldLength;
    public static String TextfileToPointDialog_FieldRow1;
    public static String TextfileToPointDialog_FieldRow2;
    public static String TextfileToPointDialog_FieldRow3;
    public static String TextfileToPointDialog_FieldRow4;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
