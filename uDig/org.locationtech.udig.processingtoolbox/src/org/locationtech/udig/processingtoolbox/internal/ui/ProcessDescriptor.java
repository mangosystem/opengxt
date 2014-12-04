/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;

/**
 * Process Descriptor
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class ProcessDescriptor {
    protected static final Logger LOGGER = Logging.getLogger(ProcessDescriptor.class);

    private static String GXT_DOCS = "toolbox_help"; //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    @SuppressWarnings({ "nls" })
    public static File generate(org.geotools.process.ProcessFactory factory,
            org.opengis.feature.type.Name processName) throws IOException {
        File configurationsFolder = ToolboxPlugin.getDefault().getStateLocation().toFile();
        File htmlDocsFolder = new File(configurationsFolder, GXT_DOCS);
        if (!htmlDocsFolder.exists()) {
            if (!htmlDocsFolder.mkdir()) {
                throw new RuntimeException();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>").append(NEWLINE);
        sb.append("<head>").append(NEWLINE);
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />").append(NEWLINE);
        sb.append("  <title>" + processName.getLocalPart() + "</title>").append(NEWLINE);
        
        sb.append("  <style type=\"text/css\">").append(NEWLINE);
        sb.append("     body {").append(NEWLINE);
        sb.append("        font-size:9pt;").append(NEWLINE);
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;").append(NEWLINE);
        sb.append("        background-color: #ffffff }").append(NEWLINE);
        
        sb.append("     h1 {").append(NEWLINE);
        sb.append("        font-size:11pt;").append(NEWLINE);
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}").append(NEWLINE);

        sb.append("     table, th, td {").append(NEWLINE);
        sb.append("        font-size:9pt;").append(NEWLINE);
        sb.append("        vertical-align:center;").append(NEWLINE);
        sb.append("        border-collapse:collapse;").append(NEWLINE);
        sb.append("        padding:0px; border-spacing:0px;").append(NEWLINE);        
        sb.append("        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}").append(NEWLINE);
        
        sb.append("  </style>").append(NEWLINE);
        sb.append("</head>").append(NEWLINE);
        sb.append("<body>").append(NEWLINE);

        // input parameters
        sb.append("<h1>" + Messages.ProcessDescriptor_Input_Parameters + "</h1>").append(NEWLINE);
        describeparameters(sb, factory.getParameterInfo(processName));

        // output parameters
        sb.append("<h1>" + Messages.ProcessDescriptor_Output_Parameters + "</h1>").append(NEWLINE);
        describeparameters(sb, factory.getResultInfo(processName, null));

        // general information
        sb.append("<h1>" + Messages.ProcessDescriptor_General_Information + "</h1>").append(NEWLINE);
        sb.append("<ul>").append(NEWLINE);
        if (factory.getClass().getName().startsWith("org.geotools.process.spatialstatistics"))  {
            sb.append("<li>" + Messages.ProcessDescriptor_Product + ": Processing ToolBox ");
            sb.append(ToolboxPlugin.getDefault().getBundle().getVersion()).append("</li>").append(NEWLINE);
            sb.append("<li>" + Messages.ProcessDescriptor_Author + ": <a href=\"").append("www.mangosystem.com").append("\">");
            sb.append("Mango System").append("</a></li>").append(NEWLINE);
            sb.append("<li>" + Messages.ProcessDescriptor_Document + ": <a href=\"").append("www.mangosystem.com");
            sb.append("\">" + Messages.ProcessDescriptor_Online_Help + "</a>");
            sb.append(", <a href=\"http://onspatial.com\">" + Messages.ProcessDescriptor_Team_Blog + "</a></li>").append(NEWLINE);

            sb.append("<li>" + Messages.ProcessDescriptor_Contact + ": <a href=\"mailto:mapplus@gmail.com\">mapplus@gmail.com</a>, ");
            sb.append("<a href=\"mailto:jya1210@gmail.com\">jya1210@gmail.com</a></li>").append(NEWLINE);
        } else {
            sb.append("<li>" + Messages.ProcessDescriptor_Product + ": GeoTools</li>").append(NEWLINE);
            sb.append("<li>" + Messages.ProcessDescriptor_Home + ": <a href=\"http://www.geotools.org\">http://www.geotools.org</a></li>").append(NEWLINE);
        }
        
        sb.append("</ul>").append(NEWLINE);

        // write to file
        sb.append("</body></html>");
        
        File htmlDocs = new File(htmlDocsFolder, processName.getLocalPart() + ".html");
        FileUtils.writeStringToFile(htmlDocs, sb.toString());
        return htmlDocs;
    }

    @SuppressWarnings("nls")
    private static void describeparameters(StringBuilder sb, Map<String, Parameter<?>> params) {
        sb.append("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">")
                .append(NEWLINE);

        sb.append("<colgroup>").append(NEWLINE);
        sb.append("<col width=\"30%\" />").append(NEWLINE);
        sb.append("<col width=\"60%\" />").append(NEWLINE);
        sb.append("<col width=\"10%\" />").append(NEWLINE);
        sb.append("</colgroup>").append(NEWLINE);

        sb.append("<tr bgcolor=\"#cccccc\"><td><strong>" + Messages.ProcessDescriptor_Parameter + "</strong></td>").append(NEWLINE);
        sb.append("<td><strong>" + Messages.ProcessDescriptor_Explanation + "</strong></td>").append(NEWLINE);
        sb.append("<td><strong>" + Messages.ProcessDescriptor_Required + "</strong></td></tr>").append(NEWLINE);
        for (Entry<String, Parameter<?>> entrySet : params.entrySet()) {
            Parameter<?> param = entrySet.getValue();
            sb.append("<tr><td>").append(param.title).append("</td>").append(NEWLINE);
            sb.append("<td>").append(param.description).append("</td>").append(NEWLINE);
            sb.append("<td>").append(param.required).append("</td></tr>").append(NEWLINE);
        }
        sb.append("</table>").append(NEWLINE);
    }
}
