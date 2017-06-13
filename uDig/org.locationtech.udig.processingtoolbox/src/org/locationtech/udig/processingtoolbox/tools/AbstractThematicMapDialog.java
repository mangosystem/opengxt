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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.StyleFactory;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.filter.FilterFactory2;

/**
 * Histogram Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractThematicMapDialog extends AbstractGeoProcessingDialog {

    protected static final Logger LOGGER = Logging.getLogger(AbstractThematicMapDialog.class);

    @SuppressWarnings("nls")
    protected static final String[] NUMERIC_METHOD = new String[] { "Equal Interval",
            "Natural Breaks", "Quantile" }; // , "Standard Deviation"

    @SuppressWarnings("nls")
    protected static final String[] CATEGORY_METHOD = new String[] { "Unique Values", "LISA Style" };

    protected ColorBrewer brewer;

    protected StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    protected FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    protected ILayer activeLayer;

    protected Combo cboColorRamp;

    protected Label lblPreview;

    protected Button chkReverse;

    public AbstractThematicMapDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);
        this.brewer = ColorBrewer.instance();
    }

    @SuppressWarnings("nls")
    protected String getFunctionName(String styleName) {
        String functionName = null;
        if (styleName.startsWith("LISA")) {
            functionName = "LISA Style";
        } else if (styleName.startsWith("ZSCORE")) {
            functionName = "ZScore";
        } else if (styleName.startsWith("ZSCORE STAND")) {
            functionName = "ZScore Standard";
        } else if (styleName.startsWith("CL") || styleName.startsWith("JE")
                || styleName.startsWith("NA")) {
            functionName = "JenksNaturalBreaksFunction";
        } else if (styleName.startsWith("E")) {
            functionName = "EqualIntervalFunction";
        } else if (styleName.startsWith("S")) {
            functionName = "StandardDeviationFunction";
        } else if (styleName.startsWith("Q")) {
            functionName = "QuantileFunction";
        } else if (styleName.startsWith("U")) {
            functionName = "UniqueIntervalFunction";
        }

        return functionName;
    }

    protected void updatePreview() {
        int selIndex = cboColorRamp.getSelectionIndex();
        if (selIndex == -1) {
            return;
        }

        // draw image
        String paletteName = cboColorRamp.getItem(selIndex).split("\\(")[0]; //$NON-NLS-1$
        BrewerPalette palette = brewer.getPalette(paletteName);

        org.eclipse.swt.graphics.Point size = cboColorRamp.getSize();
        int width = size.x > 0 ? size.x : windowSize.x - 120;
        int height = size.y > 0 ? size.y : 32;

        java.awt.Color[] colors = palette.getColors();
        if (chkReverse.getSelection()) {
            Collections.reverse(Arrays.asList(colors));
        }

        Image image = paletteToImage(colors, width, height).createImage();
        lblPreview.setImage(image);
        lblPreview.update();
    }

    @SuppressWarnings("nls")
    protected void updateColorRamp(int type, int selection) {
        BrewerPalette[] palettes = null;
        if (type == 0) // All
            palettes = brewer.getPalettes(ColorBrewer.ALL);
        else if (type == 1) // Numerical
            palettes = brewer.getPalettes(ColorBrewer.SUITABLE_RANGED);
        else if (type == 2) // Sequential
            palettes = brewer.getPalettes(ColorBrewer.SEQUENTIAL);
        else if (type == 3) // Diverging
            palettes = brewer.getPalettes(ColorBrewer.DIVERGING);
        else if (type == 4) // Categorical
            palettes = brewer.getPalettes(ColorBrewer.SUITABLE_UNIQUE);
        else
            palettes = brewer.getPalettes(ColorBrewer.ALL);

        cboColorRamp.removeAll();
        for (BrewerPalette palette : palettes) {
            String name = String.format("%s(%s)", palette.getName(), palette.getDescription());
            cboColorRamp.add(name);
        }
        cboColorRamp.select(cboColorRamp.getItemCount() > selection ? selection : 0);
    }

    protected ImageDescriptor paletteToImage(java.awt.Color colors[], final int width,
            final int height) {
        // remove null color
        List<java.awt.Color> list = new ArrayList<java.awt.Color>();
        for (java.awt.Color color : colors) {
            if (color != null) {
                list.add(color);
            }
        }

        final java.awt.Color[] palettes = new java.awt.Color[list.size()];
        list.toArray(palettes);

        return new ImageDescriptor() {

            public ImageData getImageData() {
                Display display = PlatformUI.getWorkbench().getDisplay();

                Image swtImage = new Image(display, width, height);
                org.eclipse.swt.graphics.GC gc = new GC(swtImage);
                gc.setAntialias(SWT.ON);

                org.eclipse.swt.graphics.Color swtColor = null;
                int interval = width / palettes.length;
                for (int i = 0; i < palettes.length; i++) {
                    try {
                        java.awt.Color color = palettes[i];
                        if (color == null) {
                            continue;
                        }

                        swtColor = new org.eclipse.swt.graphics.Color(display, color.getRed(),
                                color.getGreen(), color.getBlue());
                        gc.setBackground(swtColor);
                        gc.fillRectangle(interval * i, 1, interval * (i + 1), height - 1);
                    } finally {
                        swtColor.dispose();
                    }
                }

                ImageData clone = (ImageData) swtImage.getImageData().clone();
                swtImage.dispose();

                return clone;
            }
        };
    }
}
