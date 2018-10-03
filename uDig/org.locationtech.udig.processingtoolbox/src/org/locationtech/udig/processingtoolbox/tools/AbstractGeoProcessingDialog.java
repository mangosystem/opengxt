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

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.WidgetBuilder;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Abstract GeoProcessing Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractGeoProcessingDialog extends TitleAreaDialog {
    protected static final Logger LOGGER = Logging.getLogger(AbstractGeoProcessingDialog.class);

    protected int increment = 10; // IRunnableWithProgress

    protected final String EMPTY = ""; //$NON-NLS-1$

    protected final String DOT3 = "..."; //$NON-NLS-1$

    protected String windowTitle = EMPTY;

    protected String windowDesc = EMPTY;

    protected org.eclipse.swt.graphics.Point windowSize = new org.eclipse.swt.graphics.Point(650,
            400);

    protected OutputDataWidget locationView;

    protected String error;

    protected org.locationtech.udig.project.internal.Map map;

    protected WidgetBuilder uiBuilder = WidgetBuilder.newInstance();

    public AbstractGeoProcessingDialog(Shell parentShell, IMap map) {
        super(parentShell);
        this.map = (org.locationtech.udig.project.internal.Map) map;
    }

    @Override
    public void create() {
        super.create();
        setTitle(windowTitle);
        setMessage(windowDesc);
    }

    @Override
    protected org.eclipse.swt.graphics.Point getInitialSize() {
        return windowSize;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(windowTitle);
    }

    @SuppressWarnings("nls")
    protected String saveErrorAsText() {
        OutputStream os = null;
        try {
            File folder = new File(System.getProperty("user.home"));
            File file = File.createTempFile("gxt_", ".txt", folder);
            os = new FileOutputStream(file);
            Writer writer = new OutputStreamWriter(os, Charset.defaultCharset());
            writer.write(error);
            return file.getPath();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(os);
        }
        return null;
    }

    protected boolean existCheckedItem(Table table) {
        for (TableItem item : table.getItems()) {
            if (item.getChecked()) {
                return true;
            }
        }
        return false;
    }

    protected boolean invalidWidgetValue(Widget... widgets) {
        for (Widget widget : widgets) {
            if (widget instanceof Combo && ((Combo) widget).getText().isEmpty()) {
                return true;
            } else if (widget instanceof Text
                    && StringHelper.isNullOrEmpty(((Text) widget).getText())) {
                return true;
            } else if (widget instanceof Table && ((Table) widget).getItemCount() == 0) {
                return true;
            }
        }

        return false;
    }

    protected void openInformation(final Shell activeCell, Object msg) {
        MessageDialog.openInformation(activeCell, activeCell.getText(), msg.toString());
    }

    protected void setEnabled(boolean enabled) {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(enabled);
        }
    }

    protected void setComboItems(Combo combo, Class<?> enumClass) {
        for (Object enumVal : enumClass.getEnumConstants()) {
            combo.add(enumVal.toString());
        }
    }

    protected void addFeaturesToMap(IMap map, String filePath, String layerName) {
        try {
            File shapefile = new File(filePath);
            if (!shapefile.exists()) {
                return;
            }

            CatalogPlugin catalogPlugin = CatalogPlugin.getDefault();
            ICatalog localCatalog = catalogPlugin.getLocalCatalog();

            URL resourceId = DataUtilities.fileToURL(shapefile);
            List<IService> services = catalogPlugin.getServiceFactory().createService(resourceId);
            for (IService service : services) {
                localCatalog.add(service);
                for (IGeoResource resource : service.resources(null)) {
                    List<IGeoResource> resourceList = Collections.singletonList(resource);
                    final int pos = map.getMapLayers().size();
                    Layer layer = (Layer) ApplicationGIS.addLayersToMap(map, resourceList, pos)
                            .get(0);
                    layer.setName(layerName);

                    SSStyleBuilder ssBuilder = new SSStyleBuilder(layer.getSchema());
                    ssBuilder.setOpacity(0.8f);

                    Style style = ssBuilder.getDefaultFeatureStyle();
                    if (style != null) {
                        layer.getStyleBlackboard().put(SLDContent.ID, style);
                        layer.getStyleBlackboard().setSelected(new String[] { SLDContent.ID });
                    }

                    // refresh
                    layer.refresh(layer.getBounds(new NullProgressMonitor(), null));
                }
            }
        } catch (MalformedURLException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (IOException e) {
            ToolboxPlugin.log(e.getMessage());
        }
    }

    protected void fillRasterLayers(IMap map, Combo combo) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(GridCoverageReader.class)
                    || layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                // must be one band!
                GridCoverage2D coverage = MapUtils.getGridCoverage(layer);
                int numBands = coverage.getNumSampleDimensions();
                if (numBands == 1) {
                    combo.add(layer.getName());
                }
            }
        }
    }

    protected void fillLayers(IMap map, Combo combo, VectorLayerType layerType) {
        combo.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.hasResource(FeatureSource.class)) {
                GeometryDescriptor descriptor = layer.getSchema().getGeometryDescriptor();
                Class<?> geometryBinding = descriptor.getType().getBinding();
                switch (layerType) {
                case ALL:
                    combo.add(layer.getName());
                    break;
                case LINESTRING:
                    if (geometryBinding.isAssignableFrom(LineString.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POINT:
                    if (geometryBinding.isAssignableFrom(Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POLYGON:
                    if (geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case MULTIPART:
                    if (geometryBinding.isAssignableFrom(MultiPolygon.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                case POLYLINE:
                    if (geometryBinding.isAssignableFrom(Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)
                            || geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        combo.add(layer.getName());
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    protected void fillEnum(Combo combo, Class<?> enumType) {
        combo.removeAll();
        for (Object enumVal : enumType.getEnumConstants()) {
            combo.add(enumVal.toString());
        }
        combo.select(0);
    }

    protected void fillFields(Combo combo, SimpleFeatureType schema, FieldType fieldType) {
        combo.removeAll();
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                continue;
            }

            Class<?> binding = descriptor.getType().getBinding();
            switch (fieldType) {
            case ALL:
                combo.add(descriptor.getLocalName());
                break;
            case Double:
                if (Double.class.isAssignableFrom(binding) || Float.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case Integer:
                if (Short.class.isAssignableFrom(binding)
                        || Integer.class.isAssignableFrom(binding)
                        || Long.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case Number:
                if (Number.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            case String:
                if (String.class.isAssignableFrom(binding)) {
                    combo.add(descriptor.getLocalName());
                }
                break;
            }
        }
    }

    protected Style buildCoverageStyle(double minValue, double maxValue, Double noData) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

        @SuppressWarnings("nls")
        String[] palettes = { "RdYlGn", "YlOrRd", "YlOrBr", "Oranges", "YlGnBu", "Spectral" };

        int numClasses = 8;

        double[] breaks = new double[numClasses + 1];
        double interval = (maxValue - minValue) / numClasses;

        ColorBrewer brewer = ColorBrewer.instance();
        Random random = new Random();
        BrewerPalette brewerPalette = brewer.getPalette(palettes[random.nextInt(palettes.length)]);

        Color[] colors = brewerPalette.getColors(breaks.length);
        // Collections.reverse(Arrays.asList(colors)); // reverse

        StyleBuilder builder = new StyleBuilder();

        ColorMapEntry nodataEntry = sf.createColorMapEntry();
        nodataEntry.setQuantity(ff.literal(noData));
        nodataEntry.setColor(builder.colorExpression(new java.awt.Color(255, 255, 255, 0)));
        nodataEntry.setOpacity(ff.literal(0.0f));
        nodataEntry.setLabel("No Data"); //$NON-NLS-1$

        ColorMap colorMap = sf.createColorMap();
        colorMap.setType(ColorMap.TYPE_RAMP);

        if (noData < breaks[0]) {
            colorMap.addColorMapEntry(nodataEntry);
        }

        for (int i = 0; i < breaks.length; i++) {
            breaks[i] = minValue + (i * interval);

            ColorMapEntry entry = sf.createColorMapEntry();
            entry.setQuantity(builder.literalExpression(breaks[i]));
            entry.setColor(builder.colorExpression(colors[i]));
            entry.setOpacity(builder.literalExpression(colors[i].getAlpha() / 255.0));

            colorMap.addColorMapEntry(entry);
        }

        if (noData > breaks[breaks.length - 1]) {
            colorMap.addColorMapEntry(nodataEntry);
        }

        return builder.createStyle(builder.createRasterSymbolizer(colorMap, 1.0d));
    }
}
