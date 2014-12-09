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

import org.geotools.process.spatialstatistics.GlobalGStatisticsProcess.GStatisticsProcessResult;
import org.geotools.process.spatialstatistics.GlobalMoransIProcess.MoransIProcessResult;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult.DataStatisticsItem;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName.PearsonItem;
import org.geotools.process.spatialstatistics.pattern.NNIOperation.NearestNeighborResult;

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
        sb.append("<head>").append(NEWLINE);
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />")
                .append(NEWLINE);
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
            write("<tr><td>Count</td><td>" + item.getCount() + "</td></tr>");
            write("<tr><td>Invalid Count</td><td>" + item.getInvalidCount() + "</td></tr>");
            write("<tr><td>Minimum</td><td>" + item.getMinimum() + "</td></tr>");
            write("<tr><td>Maximum</td><td>" + item.getMaximum() + "</td></tr>");
            write("<tr><td>Range</td><td>" + item.getRange() + "</td></tr>");
            write("<tr><td>Ranges</td><td>" + item.getRanges() + "</td></tr>");
            write("<tr><td>Sum</td><td>" + item.getSum() + "</td></tr>");
            write("<tr><td>Mean</td><td>" + item.getMean() + "</td></tr>");
            write("<tr><td>Variance</td><td>" + item.getVariance() + "</td></tr>");
            write("<tr><td>Standard Deviation</td><td>" + item.getStandardDeviation()
                    + "</td></tr>");
            write("<tr><td>Coefficient Of Variance</td><td>" + item.getCoefficientOfVariance()
                    + "</td></tr>");
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
        write("<tr><td>Moran Index</td><td>" + value.getMoran_Index() + "</td></tr>");
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
        write("<tr><td>Observed General G</td><td>" + value.getObserved_General_G() + "</td></tr>");
        write("<tr><td>Expected General G</td><td>" + value.getExpected_General_G() + "</td></tr>");
        write("<tr><td>Variance</td><td>" + value.getVariance() + "</td></tr>");
        write("<tr><td>z Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Conceptualization</td><td>" + value.getConceptualization() + "</td></tr>");
        write("<tr><td>Distance Method</td><td>" + value.getDistanceMethod() + "</td></tr>");
        write("<tr><td>Row Standardization</td><td>" + value.getRowStandardization() + "</td></tr>");
        write("<tr><td>Distance Threshold</td><td>" + value.getDistanceThreshold() + "</td></tr>");

        write("</table>");
    }

    // NearestNeighborResult
    public void writeNearestNeighbor(NearestNeighborResult value) {
        writeH1("Average Nearest Neighbor Summary");
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
        write("<tr><td>Study Area</td><td>" + value.getStudy_Area() + "</td></tr>");
        write("<tr><td>Observed Mean Distance</td><td>" + value.getObserved_Mean_Distance()
                + "</td></tr>");
        write("<tr><td>Expected Mean Distance</td><td>" + value.getExpected_Mean_Distance()
                + "</td></tr>");
        write("<tr><td>Nearest Neighbor Ratio</td><td>" + value.getNearest_Neighbor_Ratio()
                + "</td></tr>");
        write("<tr><td>Z-Score</td><td>" + value.getZ_Score() + "</td></tr>");
        write("<tr><td>p-Value</td><td>" + value.getP_Value() + "</td></tr>");
        write("<tr><td>Standard Error</td><td>" + value.getStandard_Error() + "</td></tr>");

        write("</table>");
    }
}
