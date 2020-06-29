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
package org.geotools.process.spatialstatistics.storage;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.process.spatialstatistics.gridcoverage.RasterProcessingOperation;
import org.geotools.util.logging.Logging;

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;

/**
 * Calculates statistics on values of a raster within the zones of another features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CreatePyramidOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(CreatePyramidOperation.class);

    public CreatePyramidOperation() {

    }

    public void execute(URI src, URI dest, int level) {
        try {
            long l = System.currentTimeMillis();
            File tiff = new File(src);
            GeoTiffReader gtr = new GeoTiffReader(tiff);
            GeneralEnvelope env = gtr.getOriginalEnvelope();
            double realWidth = env.getMaximum(0) - env.getMinimum(0);
            double realHeight = env.getMaximum(1) - env.getMinimum(1);
            // System.out.println(gtr.getEnvelope());
            // Rectangle r = new Rectangle((int) x, (int) y, (int) roiW,
            // (int) roiH);

            ImageInputStream iis = ImageIO.createImageInputStream(tiff);
            ImageReader imageReader = (new TIFFImageReaderSpi()).createReaderInstance();
            imageReader.setInput(iis);
            System.out.println(imageReader.getNumThumbnails(0));
            System.out.println(imageReader.getNumImages(true));

            int imgWidth = imageReader.getWidth(0);
            int imgHeight = imageReader.getHeight(0);

            double xCellSize = realWidth / (double) imgWidth;
            double yCellSize = realHeight / (double) imgHeight;

            Rectangle r = new Rectangle((int) 0, (int) 0, (int) imageReader.getWidth(0),
                    (int) imageReader.getHeight(0));

            
            System.out.println( gtr.getNumOverviews());
            
            ImageReadParam readP = new ImageReadParam();
            readP.setSourceSubsampling(1, 1, 0, 0);
            readP.setSourceRegion(r);

            ParameterBlock pbjRead = new ParameterBlock();
            pbjRead.add(ImageIO.createImageInputStream(tiff));
            pbjRead.add(new Integer(0));
            pbjRead.add(Boolean.FALSE);
            pbjRead.add(Boolean.FALSE);
            pbjRead.add(Boolean.FALSE);
            pbjRead.add(null);
            pbjRead.add(null);
            pbjRead.add(readP);
            pbjRead.add((new TIFFImageReaderSpi()).createReaderInstance());
            RenderedOp image = JAI.create("ImageRead", pbjRead, null);

            PlanarImage overview = image;
            int i = 0;
            double scale = 0.5;
            List<RenderedImage> overviews = new ArrayList<RenderedImage>();

            TIFFEncodeParam parms = new TIFFEncodeParam();
            parms.setWriteTiled(true);
            parms.setTileSize(image.getTileWidth(), image.getTileHeight());
            parms.setCompression(TIFFEncodeParam.COMPRESSION_DEFLATE);

            System.out.println(i + "  getW    = " + overview.getNumXTiles());
            System.out.println(i + "  getH    = " + overview.getNumYTiles());
            System.out.println(i + "  W    = " + overview.getTileWidth());
            System.out.println(i + "  H    = " + overview.getTileHeight());
            
            PlanarImage pi = overview;
//            overviews.add(overview);
            ParameterBlock srcPb = new ParameterBlock();
            srcPb.addSource(pi);
            srcPb.add((float) 1);
            srcPb.add((float) 1);
            srcPb.add(0f);
            srcPb.add(0f);
            srcPb.add(new InterpolationNearest());
            overview = JAI.create("scale", srcPb, null);
            overviews.add(overview);

            System.out.println(i + "  getW    = " + overview.getNumXTiles());
            System.out.println(i + "  getH    = " + overview.getNumYTiles());

            while (i < level) {

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(overview);
                pb.add((float) scale);
                pb.add((float) scale);
                pb.add(0f);
                pb.add(0f);
                pb.add(new InterpolationNearest());
                overview = JAI.create("scale", pb, null);

                ++i;
                try {
                    if (overview.getNumXTiles() <= 2 && overview.getNumYTiles() <= 2) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
                System.out.println(i + "  getNumXTiles    = " + overview.getNumXTiles());
                System.out.println(i + "  getNumYTiles    = " + overview.getNumYTiles());
                overviews.add(overview);
            }
            if (!overviews.isEmpty()) {
                parms.setExtraImages(overviews.iterator());
            }

            File outputFile = new File(dest);
            FileOutputStream ostream = new FileOutputStream(outputFile);

            File outputWorld = new File(outputFile.getParent() + File.separator
                    + outputFile.getName().substring(0, outputFile.getName().lastIndexOf("."))
                    + ".tfw");
            PrintWriter pw = new PrintWriter(outputWorld);
            pw.println(xCellSize);
            pw.println(0);
            pw.println(0);
            pw.println(yCellSize * -1.);
            pw.println(env.getLowerCorner().getOrdinate(0) + xCellSize / 2.);
            pw.println(env.getUpperCorner().getOrdinate(1) - yCellSize / 2.);
            // pw.println(fEnvelope.getLowerCorner().getOrdinate(0));
            // pw.println(fEnvelope.getUpperCorner().getOrdinate(1));
            pw.flush();
            pw.close();

            File outputPrj = new File(outputFile.getParent() + File.separator
                    + outputFile.getName().substring(0, outputFile.getName().lastIndexOf("."))
                    + ".prj");
            if (env.getCoordinateReferenceSystem() != null) {
                pw = new PrintWriter(outputPrj);
                pw.println(env.getCoordinateReferenceSystem().toWKT());
                pw.flush();
                pw.close();
            }

            TIFFImageEncoder encoder = new TIFFImageEncoder(ostream, parms);
            encoder.encode(image);
            System.out.println("encode end " + (System.currentTimeMillis() - l));
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File f = new File("/Users/jyajya/work/자료/gis_data/SnowGIS/os_2.tif");
        File f2 = new File("/Users/jyajya/work/자료/gis_data/SnowGIS/os_1.tif");
        CreatePyramidOperation cpo = new CreatePyramidOperation();
        try {
            cpo.execute(f.toURI(), f2.toURI(), 8);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}