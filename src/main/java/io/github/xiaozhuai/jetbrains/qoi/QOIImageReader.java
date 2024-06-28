package io.github.xiaozhuai.jetbrains.qoi;

import org.jetbrains.annotations.NotNull;
import me.saharnooby.qoi.QOIColorSpace;
import me.saharnooby.qoi.QOIImage;
import me.saharnooby.qoi.QOIUtil;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

public final class QOIImageReader extends ImageReader {

    static final int[] OFFSETS_3 = {0, 1, 2};
    static final int[] OFFSETS_4 = {0, 1, 2, 3};

    private QOIImage image;

    QOIImageReader(@NotNull ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    private void readImage() throws IOException {
        if (this.image != null) {
            return;
        }

        if (this.input == null) {
            throw new IllegalStateException("Input not set");
        }

        ImageInputStream input = (ImageInputStream) this.input;

        // This assumes that no additional data is stored in the
        // stream, otherwise buffering will corrupt the stream.
        this.image = QOIUtil.readImage(new WrappedImageInputStream(input));
    }

    private void checkIndex(int imageIndex) {
        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int getNumImages(boolean allowSearch) {
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        checkIndex(imageIndex);

        readImage();

        return this.image.getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkIndex(imageIndex);

        readImage();

        return this.image.getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readImage();

        boolean hasAlpha = this.image.getChannels() == 4;

        ColorSpace colorSpace = getAwtColorSpace(this.image.getColorSpace());

        ImageTypeSpecifier type = ImageTypeSpecifier.createInterleaved(
                colorSpace,
                hasAlpha ? OFFSETS_4 : OFFSETS_3,
                DataBuffer.TYPE_BYTE,
                hasAlpha,
                false
        );

        return Collections.singletonList(type).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        // No metadata supported
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        // No metadata supported
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkIndex(imageIndex);

        clearAbortRequest();

        processImageStarted(imageIndex);

        readImage();

        int width = this.image.getWidth();
        int height = this.image.getHeight();

        BufferedImage source = convertToBufferedImage(this.image);

        // Fast path
        if (param == null || ImageParamUtil.isDefault(param)) {
            processImageComplete();

            return source;
        }

        Rectangle sourceRegion = getSourceRegion(param, width, height);

        int sourceXSubsampling = param.getSourceXSubsampling();
        int sourceYSubsampling = param.getSourceYSubsampling();
        int[] sourceBands = param.getSourceBands();
        int[] destinationBands = param.getDestinationBands();
        Point destinationOffset = param.getDestinationOffset();

        BufferedImage dest = getDestination(param, getImageTypes(0), width, height);

        SampleModel destSampleModel = dest.getSampleModel();

        int destBands = destSampleModel.getNumBands();

        for (int band = 0; band < destBands; band++) {
            if (destSampleModel.getSampleSize(band) != 8) {
                throw new IIOException("Reading into images with band size != 8 bits is not supported");
            }
        }

        checkReadParamBandSettings(param, this.image.getChannels(), destBands);

        WritableRaster sourceRaster = source.getWritableTile(0, 0);
        WritableRaster destRaster = dest.getWritableTile(0, 0);

        if (sourceBands != null) {
            sourceRaster = sourceRaster.createWritableChild(
                    0,
                    0,
                    width,
                    height,
                    0,
                    0,
                    sourceBands
            );
        }

        if (destinationBands != null) {
            destRaster = destRaster.createWritableChild(
                    0,
                    0,
                    destRaster.getWidth(),
                    destRaster.getHeight(),
                    0,
                    0,
                    destinationBands
            );
        }

        int destMinX = destRaster.getMinX();
        int destMaxX = destMinX + destRaster.getWidth() - 1;
        int destMinY = destRaster.getMinY();
        int destMaxY = destMinY + destRaster.getHeight() - 1;

        int[] pixel = new int[sourceRaster.getNumBands()];

        for (int y = sourceRegion.y; y < sourceRegion.y + sourceRegion.height; y += sourceYSubsampling) {
            if (y < 0 || y >= height) {
                continue;
            }

            int destY = destinationOffset.y + (y - sourceRegion.y) / sourceYSubsampling;

            if (destY < destMinY || destY > destMaxY) {
                continue;
            }

            for (int x = sourceRegion.x; x < sourceRegion.x + sourceRegion.width; x += sourceXSubsampling) {
                if (x < 0 || x >= width) {
                    continue;
                }

                int destX = destinationOffset.x + (x - sourceRegion.x) / sourceXSubsampling;

                if (destX < destMinX || destX > destMaxX) {
                    continue;
                }

                sourceRaster.getPixel(x, y, pixel);

                destRaster.setPixel(destX, destY, pixel);
            }

            processImageProgress((y - sourceRegion.y) * 100F / sourceRegion.height);

            if (abortRequested()) {
                processReadAborted();

                return dest;
            }
        }

        processImageComplete();

        return dest;
    }

    @Override
    public void reset() {
        super.reset();

        this.image = null;
    }

    public static BufferedImage convertToBufferedImage(@NotNull QOIImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int channels = image.getChannels();

        boolean hasAlpha = channels == 4;

        DataBufferByte buffer = new DataBufferByte(image.getPixelData(), width * height * channels);

        WritableRaster raster = Raster.createInterleavedRaster(
                buffer,
                width,
                height,
                channels * width,
                channels,
                hasAlpha ? OFFSETS_4 : OFFSETS_3,
                new Point(0, 0)
        );

        ColorSpace awtColorSpace = getAwtColorSpace(image.getColorSpace());

        ColorModel colorModel = new ComponentColorModel(
                awtColorSpace,
                hasAlpha,
                false,
                hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE
        );

        return new BufferedImage(
                colorModel,
                raster,
                false,
                new Hashtable<>()
        );
    }

    private static ColorSpace getAwtColorSpace(@NotNull QOIColorSpace colorSpace) {
        switch (colorSpace) {
            case SRGB:
                return ColorSpace.getInstance(ColorSpace.CS_sRGB);
            case LINEAR:
                return ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
            default:
                throw new RuntimeException();
        }
    }

}
