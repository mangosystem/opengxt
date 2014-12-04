/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

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
        sb.append(
                "        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;")
                .append(NEWLINE);
        sb.append("        background-color: #ffffff }").append(NEWLINE);

        sb.append("     h1 {").append(NEWLINE);
        sb.append("        font-size:12pt;").append(NEWLINE);
        sb.append(
                "        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}")
                .append(NEWLINE);

        sb.append("     h2 {").append(NEWLINE);
        sb.append("        font-size:11pt;").append(NEWLINE);
        sb.append(
                "        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}")
                .append(NEWLINE);

        sb.append("     table, th, td {").append(NEWLINE);
        sb.append("        font-size:10pt;").append(NEWLINE);
        sb.append("        vertical-align:center;").append(NEWLINE);
        sb.append("        border-collapse:collapse;").append(NEWLINE);
        sb.append("        padding:0px; border-spacing:0px;").append(NEWLINE);
        sb.append(
                "        font-family: \"Lucida Sans\", \"Lucida Sans Unicode\", \"Lucida Grande\", Verdana, Arial, Helvetica, sans-serif;}")
                .append(NEWLINE);

        sb.append("  </style>").append(NEWLINE);
        sb.append("</head>").append(NEWLINE);
        sb.append("<body>").append(NEWLINE);
    }

    public String getHTML() {
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

    public void close() {
        if (!closed) {
            sb.append("</body></html>");
            closed = true;
        }
    }

}
