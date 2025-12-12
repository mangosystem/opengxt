package org.geotools.process.spatialstatistics.core;

import java.awt.image.ColorModel;
import java.awt.image.SampleModel;

/**
 * Lightweight compatibility wrapper to replace the old JAITools DiskMemImage
 * with an ImageN-based TiledImage. This class aims to provide the constructors
 * and small API surface used by the project (constructor and setUseCommonCache).
 *
 * NOTE: This is intentionally minimal — if further DiskMemImage methods are
 * required by the codebase they can be added here.
 */
public class DiskMemImage extends org.eclipse.imagen.TiledImage {

    /**
     * Create a DiskMemImage compatible instance backed by ImageN's TiledImage.
     * Matches the common DiskMemImage constructor used in the project.
     *
     * @param minX left coordinate (usually 0)
     * @param minY top coordinate (usually 0)
     * @param width image width in pixels
     * @param height image height in pixels
     * @param tileGridXOffset tile grid X offset (usually 0)
     * @param tileGridYOffset tile grid Y offset (usually 0)
     * @param sampleModel sample model for the image
     * @param colorModel color model for the image
     */
    public DiskMemImage(int minX, int minY, int width, int height, int tileGridXOffset,
            int tileGridYOffset, SampleModel sampleModel, ColorModel colorModel) {
        // org.eclipse.imagen.TiledImage constructors historically accept
        // (minX, minY, width, height, tileGridXOffset, tileGridYOffset, SampleModel, ColorModel)
        // If the ImageN version uses a slightly different signature this may require
        // adjustment; for now call the matching constructor.
        super(minX, minY, width, height, tileGridXOffset, tileGridYOffset, sampleModel,
                colorModel);
    }

    public DiskMemImage(int width, int height, SampleModel sampleModel) {
        this(0, 0, width, height, 0, 0, sampleModel, null);
    }

    public DiskMemImage(int width, int height,
                        SampleModel sampleModel,
                        ColorModel colorModel) {
        this(0, 0, width, height, 0, 0, sampleModel, colorModel);
    }

    public DiskMemImage(int minX, int minY,
                        int width, int height,
                        SampleModel sampleModel) {
        this(minX, minY, width, height, 0, 0, sampleModel, null);
    }

    /**
     * No-op compatibility method. The original DiskMemImage allowed toggling
     * an internal cache mode; ImageN manages caching differently. Keep a
     * method to preserve call sites.
     */
    public void setUseCommonCache(boolean useCommonCache) {
        // intentionally no-op; ImageN handles caching differently
    }
}
