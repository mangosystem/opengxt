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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.Parameter;
import org.geotools.process.spatialstatistics.GlobalGStatisticsProcess.GStatisticsProcessResult;
import org.geotools.process.spatialstatistics.GlobalGearysCProcess.GearysCProcessResult;
import org.geotools.process.spatialstatistics.GlobalLeesLProcess.LeesLProcessResult;
import org.geotools.process.spatialstatistics.GlobalLeesSProcess.LeesSProcessResult;
import org.geotools.process.spatialstatistics.GlobalMoransIProcess.MoransIProcessResult;
import org.geotools.process.spatialstatistics.GlobalRogersonRProcess.RogersonRProcessResult;
import org.geotools.process.spatialstatistics.JoinCountStatisticsProcess.JoinCountProcessResult;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.HistogramProcessResult;
import org.geotools.process.spatialstatistics.core.HistogramProcessResult.HistogramItem;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult.DataStatisticsItem;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName.PearsonItem;
import org.geotools.process.spatialstatistics.pattern.NNIOperation.NearestNeighborResult;
import org.geotools.process.spatialstatistics.pattern.QuadratOperation.QuadratResult;
import org.geotools.process.spatialstatistics.relationship.OLSResult;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Diagnostics;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variables.Variable;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance.RegressionItem;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance.ResidualItem;
import org.geotools.process.spatialstatistics.relationship.OLSResult.Variance.SumItem;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;

/**
 * HTML Writer
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class HtmlWriter {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private StringBuffer sb = new StringBuffer();

    private boolean closed = false;

    public HtmlWriter(String title) {
        sb.append("<!DOCTYPE html>").append(NEWLINE);
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">").append(NEWLINE);
        sb.append("<head>").append(NEWLINE);
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
        sb.append(NEWLINE);
        sb.append("  <title>" + title + "</title>").append(NEWLINE);

        sb.append("  <style type=\"text/css\">").append(NEWLINE);
        sb.append("     body {").append(NEWLINE);
        sb.append("        font-size:10pt;").append(NEWLINE);
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;");
        sb.append(NEWLINE);
        sb.append("        background-color: #ffffff }").append(NEWLINE);

        sb.append("     h1 {").append(NEWLINE);
        sb.append("        font-size:12pt;").append(NEWLINE);
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}");
        sb.append(NEWLINE);

        sb.append("     h2 {").append(NEWLINE);
        sb.append("        font-size:11pt;").append(NEWLINE);
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}");
        sb.append(NEWLINE);

        sb.append("     table, th, td {").append(NEWLINE);
        sb.append("        font-size:10pt;").append(NEWLINE);
        sb.append("        vertical-align:center;").append(NEWLINE);
        sb.append("        border-collapse:collapse;").append(NEWLINE);
        sb.append("        padding:0px; border-spacing:0px;").append(NEWLINE);
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}");
        sb.append(NEWLINE);

        sb.append("  </style>").append(NEWLINE);
        sb.append("</head>").append(NEWLINE);
        sb.append("<body>").append(NEWLINE);
    }

    public String getHTML() {
        if (!closed) {
            sb.append("</body></html>");
            closed = true;
        }
        return sb.toString();
    }

    public void writeH1(String value) {
        sb.append("<h1>" + value + "</h1>").append(NEWLINE);
    }

    public void writeH2(String value) {
        sb.append("<h2>" + value + "</h2>").append(NEWLINE);
    }

    public void write(String value) {
        sb.append(value).append(NEWLINE);
    }

    public void writeProcessMetadata(org.geotools.process.ProcessFactory factory,
            org.opengis.feature.type.Name processName) {
        // input parameters
        sb.append("<h1>" + Messages.ProcessDescriptor_Input_Parameters + "</h1>").append(NEWLINE);
        describeparameters(sb, factory.getParameterInfo(processName));

        // output parameters
        sb.append("<h1>" + Messages.ProcessDescriptor_Output_Parameters + "</h1>").append(NEWLINE);
        describeparameters(sb, factory.getResultInfo(processName, null));

        // general information
        sb.append("<h1>" + Messages.ProcessDescriptor_General_Information + "</h1>");
        sb.append(NEWLINE);
        sb.append("<ul>").append(NEWLINE);
        if (factory.getClass().getName().startsWith("org.geotools.process.spatialstatistics")) {
            sb.append("<li>" + Messages.ProcessDescriptor_Product + ": Processing ToolBox ");
            sb.append(ToolboxPlugin.getDefault().getBundle().getVersion()).append("</li>");
            sb.append(NEWLINE);
            sb.append("<li>" + Messages.ProcessDescriptor_Author + ": <a href=\"")
                    .append("www.mangosystem.com").append("\">");
            sb.append("Mango System").append("</a></li>").append(NEWLINE);
            sb.append("<li>" + Messages.ProcessDescriptor_Document + ": <a href=\"").append(
                    "www.mangosystem.com");
            sb.append("\">" + Messages.ProcessDescriptor_Online_Help + "</a>");
            sb.append(", <a href=\"http://onspatial.com\">" + Messages.ProcessDescriptor_Team_Blog
                    + "</a></li>");
            sb.append(NEWLINE);

            sb.append("<li>" + Messages.ProcessDescriptor_Contact
                    + ": <a href=\"mailto:mapplus@gmail.com\">mapplus@gmail.com</a>, ");
            sb.append("<a href=\"mailto:mango@mangosystem.com\">mango@mangosystem.com</a></li>");
            sb.append(NEWLINE);
        } else {
            sb.append("<li>" + Messages.ProcessDescriptor_Product + ": GeoTools</li>");
            sb.append(NEWLINE);
            sb.append("<li>" + Messages.ProcessDescriptor_Home
                    + ": <a href=\"http://www.geotools.org\">http://www.geotools.org</a></li>");
            sb.append(NEWLINE);
        }

        sb.append("</ul>").append(NEWLINE);
    }

    private void describeparameters(StringBuffer sb, Map<String, Parameter<?>> params) {
        sb.append("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");
        sb.append(NEWLINE);

        sb.append("<colgroup>").append(NEWLINE);
        sb.append("<col width=\"30%\" />").append(NEWLINE);
        sb.append("<col width=\"60%\" />").append(NEWLINE);
        sb.append("<col width=\"10%\" />").append(NEWLINE);
        sb.append("</colgroup>").append(NEWLINE);

        sb.append("<tr bgcolor=\"#cccccc\"><td><strong>" + Messages.ProcessDescriptor_Parameter
                + "</strong></td>");
        sb.append(NEWLINE);
        sb.append("<td><strong>" + Messages.ProcessDescriptor_Explanation + "</strong></td>");
        sb.append(NEWLINE);
        sb.append("<td><strong>" + Messages.ProcessDescriptor_Required + "</strong></td></tr>");
        sb.append(NEWLINE);
        for (Entry<String, Parameter<?>> entrySet : params.entrySet()) {
            Parameter<?> param = entrySet.getValue();
            sb.append("<tr><td>").append(param.title).append("</td>").append(NEWLINE);
            sb.append("<td>").append(param.description).append("</td>").append(NEWLINE);
            sb.append("<td>").append(param.required).append("</td></tr>").append(NEWLINE);
        }
        sb.append("</table>").append(NEWLINE);
    }

    // DataStatisticsResult
    public void writeDataStatistics(DataStatisticsResult value) {
        writeH1("Summary Statistics");
        for (DataStatisticsItem item : value.getList()) {
            writeH2(item.getTypeName() + ": " + item.getPropertyName());
            write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

            // header
            write("<colgroup>");
            write("<col width=\"60%\" />");
            write("<col width=\"40%\" />");
            write("</colgroup>");

            write("<tr bgcolor=\"#cccccc\">");
            write("<td><strong>Category</strong></td>");
            write("<td><strong>Value</strong></td>");
            write("</tr>");

            // body
            if (item.getCaseValue() != null) {
                write("<tr><td>Case Value</td><td>" + item.getCaseValue() + "</td></tr>");
            }

            write("<tr><td>Count</td><td>" + item.getCount() + "</td></tr>");
            write("<tr><td>Invalid Count</td><td>" + item.getInvalidCount() + "</td></tr>");
            write("<tr><td>Minimum</td><td>" + format(item.getMinimum()) + "</td></tr>");
            write("<tr><td>Maximum</td><td>" + format(item.getMaximum()) + "</td></tr>");
            write("<tr><td>Range</td><td>" + format(item.getRange()) + "</td></tr>");
            write("<tr><td>Ranges</td><td>" + item.getRanges() + "</td></tr>");
            write("<tr><td>Sum</td><td>" + format(item.getSum()) + "</td></tr>");
            write("<tr><td>Mean</td><td>" + format(item.getMean()) + "</td></tr>");
            write("<tr><td>Variance</td><td>" + format(item.getVariance()) + "</td></tr>");
            write("<tr><td>Standard Deviation</td><td>" + format(item.getStandardDeviation())
                    + "</td></tr>");
            write("<tr><td>Coefficient Of Variance</td><td>"
                    + format(item.getCoefficientOfVariance()) + "</td></tr>");
            if (item.getNoData() != null) {
                write("<tr><td>NoData</td><td>" + item.getNoData() + "</td></tr>");
            }
            write("</table>");
        }
    }

    // PearsonResult
    public void writePearson(PearsonResult value) {
        writeH1("Pearson Correlation Coefficient");
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"40%\" />");
        write("<col width=\"30%\" />");
        write("<col width=\"30%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong> </strong></td>");
        for (PropertyName item : value.getProeprtyNames()) {
            write("<td><strong>" + item.getName() + "</strong></td>");
        }
        write("</tr>");

        // body
        for (PropertyName item : value.getProeprtyNames()) {
            write("<tr>");
            write("<td bgcolor=\"#cccccc\">" + item.getName() + "</td>");
            for (PearsonItem subItem : item.getItems()) {
                write("<td>" + FormatUtils.format(subItem.getValue(), 10) + "</td>");
            }
            write("</tr>");
        }
        write("</table>");
    }

    // MoransIProcessResult
    public void writeMoransI(MoransIProcessResult value) {
        writeH1("Global Moran's I");
        writeH2(value.getTypeName() + ": " + value.getPropertyName());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Moran Index</td><td>" + value.getObserved_Index() + "</td></tr>");
        write("<tr><td>Expected Index</td><td>" + value.getExpected_Index() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");

        write("</table>");
    }

    // GearysCProcessResult
    public void writeGearysC(GearysCProcessResult value) {
        writeH1("Global Geary's C");
        writeH2(value.getTypeName() + ": " + value.getPropertyName());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Observed Geary's C</td><td>" + value.getObserved_Index() + "</td></tr>");
        write("<tr><td>Expected Geary's C</td><td>" + value.getExpected_Index() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");

        write("</table>");
    }

    // LeesSProcessResult
    public void writeLeesS(LeesSProcessResult value) {
        writeH1("Global Lee's S");
        writeH2(value.getTypeName() + ": " + value.getPropertyName());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Observed Lee's S</td><td>" + value.getObserved_Index() + "</td></tr>");
        write("<tr><td>Expected Lee's S</td><td>" + value.getExpected_Index() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");

        write("</table>");
    }

    // LeesLProcessResult
    public void writeLeesL(LeesLProcessResult value) {
        writeH1("Global Lee's L");
        writeH2(value.getTypeName() + ": " + value.getxField() + " vs " + value.getyField());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Observed Lee's L</td><td>" + value.getObserved_Index() + "</td></tr>");
        write("<tr><td>Expected Lee's L</td><td>" + value.getExpected_Index() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");

        write("</table>");
    }

    public void writeRogersonR(RogersonRProcessResult value) {
        writeH1("Global Rogerson's R");
        writeH2(value.getTypeName() + ": " + value.getxField() + " vs " + value.getyField());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Observed Rogerson's R</td><td>" + value.getObserved_Index() + "</td></tr>");
        write("<tr><td>Expected Rogerson's R</td><td>" + value.getExpected_Index() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");
        write("<tr><td>Kappa</td><td>" + value.getKappa() + "</td></tr>");

        write("</table>");
    }

    // GStatisticsProcessResult
    public void writeGStatistics(GStatisticsProcessResult value) {
        writeH1("Global G Statistics");
        writeH2(value.getTypeName() + ": " + value.getPropertyName());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Observed General G</td><td>" + value.getObserved_Index() + "</td></tr>");
        write("<tr><td>Expected General G</td><td>" + value.getExpected_Index() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");

        write("</table>");
    }

    // JoinCountProcessResult
    public void writeJoinCount(JoinCountProcessResult value) {
        writeH1("Join Count Statistics: " + value.getTypeName());

        // 1. general
        writeH2("General");
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Contiguity Type</td><td>" + value.getContiguityType() + "</td></tr>");
        write("<tr><td>Feature Count</td><td>" + value.getFeatureCount() + "</td></tr>");
        write("<tr><td>Black : White Count</td><td>" + value.getBlackCount() + " : "
                + value.getWhiteCount() + "</td></tr>");
        write("<tr><td>Number Of Joins</td><td>" + value.getNumberOfJoins() + "</td></tr>");

        write("</table>");

        // 2. summary
        writeH2("Summary");
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Type</strong></td>");
        write("<td><strong>Observed</strong></td>");
        write("<td><strong>Expected</strong></td>");
        write("<td><strong>Std Dev</strong></td>");
        write("<td><strong>z-Score</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>BB</td>");
        write("<td>" + value.getObservedBB() + "</td>");
        write("<td>" + value.getExpectedBB() + "</td>");
        write("<td>" + format(value.getStdDevBB()) + "</td>");
        write("<td>" + format(value.getzScoreBB()) + "</td></tr>");

        write("<tr><td>WW</td>");
        write("<td>" + value.getObservedWW() + "</td>");
        write("<td>" + value.getExpectedWW() + "</td>");
        write("<td>" + format(value.getStdDevWW()) + "</td>");
        write("<td>" + format(value.getzScoreWW()) + "</td></tr>");

        write("<tr><td>BW</td>");
        write("<td>" + value.getObservedBW() + "</td>");
        write("<td>" + value.getExpectedBW() + "</td>");
        write("<td>" + format(value.getStdDevBW()) + "</td>");
        write("<td>" + format(value.getzScoreBW()) + "</td></tr>");

        write("</table>");
    }

    // NearestNeighborResult
    public void writeNearestNeighbor(NearestNeighborResult value) {
        writeH1("Average Nearest Neighbor");
        writeH2(value.getTypeName());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Observed Point Count</td><td>" + value.getObserved_Point_Count()
                + "</td></tr>");
        write("<tr><td>Study Area</td><td>" + format(value.getStudy_Area()) + "</td></tr>");
        write("<tr><td>Observed Mean Distance</td><td>" + format(value.getObserved_Mean_Distance())
                + "</td></tr>");
        write("<tr><td>Expected Mean Distance</td><td>" + format(value.getExpected_Mean_Distance())
                + "</td></tr>");
        write("<tr><td>Nearest Neighbor Ratio</td><td>" + format(value.getNearest_Neighbor_Ratio())
                + "</td></tr>");
        write("<tr><td>Z-Score</td><td>" + format(value.getZ_Score()) + "</td></tr>");
        write("<tr><td>p-Value</td><td>" + format(value.getP_Value()) + "</td></tr>");
        write("<tr><td>Standard Error</td><td>" + format(value.getStandard_Error()) + "</td></tr>");

        write("</table>");
    }

    // HistogramProcessResult
    public void writeHistogramProcess(HistogramProcessResult value) {
        writeH1("Histogram");
        writeH2(value.getTypeName() + ": " + value.getPropertyName());
        writeH2("Area: " + value.getArea());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Value</strong></td>");
        write("<td><strong>Frequency</strong></td>");
        write("</tr>");

        // body
        for (HistogramItem item : value.getHistogramItems()) {
            write("<tr><td>" + item.getValue() + "</td><td>" + item.getFrequency() + "</td></tr>");
        }

        write("</table>");
    }

    public void writeQuadratProcess(QuadratResult value) {
        writeH1("Quadrat Analysis");
        writeH2(value.getTypeName());
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        write("<tr><td>Feature Count</td><td>" + value.getFeatureCount() + "</td></tr>");
        write("<tr><td>Area</td><td>" + format(value.getArea()) + "</td></tr>");
        write("<tr><td>Cell Size</td><td>" + value.getCellSize() + "</td></tr>");

        write("<tr><td>Columns</td><td>" + value.getColumns() + "</td></tr>");
        write("<tr><td>Rows</td><td>" + value.getRows() + "</td></tr>");
        write("<tr><td>Number of Quadrats</td><td>" + value.getNumber_of_Quadrats() + "</td></tr>");

        write("<tr><td>Mean</td><td>" + value.getMean() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>Variance/Mean Ratio</td><td>" + value.getVariance_Mean_Ratio()
                + "</td></tr>");

        write("<tr><td>D value of Kolmogorov-Smirnov Test</td><td>"
                + value.getKolmogorov_Smirnov_Test() + "</td></tr>");
        write("<tr><td>Critical value at the 5% level</td><td>"
                + value.getCritical_Value_at_5percent() + "</td></tr>");

        write("</table>");
    }

    public void writeOLSProcess(OLSResult value) {
        writeH1("Ordinary Least Squares (OLS)");

        // 1. Diagnostics
        writeH2("Diagnostics");
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"60%\" />");
        write("<col width=\"40%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Category</strong></td>");
        write("<td><strong>Value</strong></td>");
        write("</tr>");

        // body
        Diagnostics diag = value.getDiagnostics();
        write("<tr><td>Number of Observations</td><td>" + format(diag.getNumberOfObservations())
                + "</td></tr>");
        write("<tr><td>R</td><td>" + format(diag.getR()) + "</td></tr>");
        write("<tr><td>R-Squared</td><td>" + format(diag.getRSquared()) + "</td></tr>");
        write("<tr><td>Adjusted R-Squared</td><td>" + format(diag.getAdjustedRSquared())
                + "</td></tr>");
        write("<tr><td>Standard Error</td><td>" + format(diag.getStandardError()) + "</td></tr>");
        write("<tr><td>Akaike's Information Criterion (AIC)</td><td>" + format(diag.getAIC())
                + "</td></tr>");
        write("<tr><td>Corrected Akaike's Information Criterion (AICc)</td><td>"
                + format(diag.getAICc()) + "</td></tr>");
        write("</table>");

        // 2. Variance
        writeH2("Variance Analysis");
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"15%\" />");
        write("<col width=\"5%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>-</strong></td>");
        write("<td><strong>DoF</strong></td>");
        write("<td><strong>Sum of squares</strong></td>");
        write("<td><strong>Square mean</strong></td>");
        write("<td><strong>F-Statistic</strong></td>");
        write("<td><strong>Probability</strong></td>");
        write("</tr>");

        // body
        RegressionItem reg = value.getVariance().getRegression();
        write("<tr><td>Regression</td><td>" + reg.getDegreesOfFreedom() + "</td><td>"
                + format(reg.getSumOfSquare()) + "</td><td>" + format(reg.getSquareMean())
                + "</td><td>" + format(reg.getfStatistic()) + "</td><td>"
                + format(reg.getfProbability()) + "</td></tr>");

        ResidualItem res = value.getVariance().getResidual();
        write("<tr><td>Residual</td><td>" + res.getDegreesOfFreedom() + "</td><td>"
                + format(res.getSumOfSquare()) + "</td><td>" + format(res.getSquareMean())
                + "</td><td>-</td><td>-</td></tr>");

        SumItem sum = value.getVariance().getSum();
        write("<tr><td>Sum</td><td>" + sum.getDegreesOfFreedom() + "</td><td>"
                + format(sum.getSumOfSquare()) + "</td><td>-</td><td>-</td><td>-</td></tr>");
        write("</table>");

        // 3. Model Variables
        writeH2("Model Variables");
        write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        write("<colgroup>");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("<col width=\"20%\" />");
        write("</colgroup>");

        write("<tr bgcolor=\"#cccccc\">");
        write("<td><strong>Variable</strong></td>");
        write("<td><strong>Coefficient</strong></td>");
        write("<td><strong>StdError</strong></td>");
        write("<td><strong>t-Statistic</strong></td>");
        write("<td><strong>Probability</strong></td>");
        write("</tr>");

        // body
        List<Variable> items = value.getVariables().getItems();
        for (Variable var : items) {
            write("<tr><td>" + var.getVariable() + "</td><td>" + format(var.getCoefficient())
                    + "</td><td>" + format(var.getStdError()) + "</td><td>"
                    + format(var.gettStatistic()) + "</td><td>" + format(var.getProbability())
                    + "</td></tr>");
        }
        write("</table>");
    }

    private String format(double value) {
        return FormatUtils.format(value);
    }
}
