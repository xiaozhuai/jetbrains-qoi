package io.github.xiaozhuai.jetbrains.qoi;

import org.jetbrains.annotations.NotNull;
import me.saharnooby.qoi.QOIColorSpace;
import me.saharnooby.qoi.QOIImage;
import me.saharnooby.qoi.QOIUtil;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.util.Arrays;

public final class QOIImageWriter extends ImageWriter {

    QOIImageWriter(@NotNull ImageWriterSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        // Metadata is not supported
        return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        // Metadata is not supported
        return null;
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
        // Metadata is not supported
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        // Metadata is not supported
        return null;
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        clearAbortRequest();

        processImageStarted(0);

        RenderedImage rendered = image.getRenderedImage();

        // Fast path
        if (param == null || ImageParamUtil.isDefault(param)) {
            writeImage(createFromRenderedImage(rendered));

            return;
        }

        Rectangle sourceRegion = new Rectangle(0, 0, rendered.getWidth(), rendered.getHeight());

        if (param.getSourceRegion() != null) {
            sourceRegion = sourceRegion.intersection(param.getSourceRegion());
        }

        int sourceXSubsampling = param.getSourceXSubsampling();
        int sourceYSubsampling = param.getSourceYSubsampling();
        int[] sourceBands = param.getSourceBands();

        int subsamplingXOffset = param.getSubsamplingXOffset();
        int subsamplingYOffset = param.getSubsamplingYOffset();
        sourceRegion.x += subsamplingXOffset;
        sourceRegion.y += subsamplingYOffset;
        sourceRegion.width -= subsamplingXOffset;
        sourceRegion.height -= subsamplingYOffset;

        int width = sourceRegion.width;
        int height = sourceRegion.height;

        Raster raster = rendered.getData(sourceRegion);

        int bandCount = sourceBands == null ? raster.getNumBands() : sourceBands.length;

        if (bandCount != 3 && bandCount != 4) {
            throw new IllegalArgumentException("Band count not supported");
        }

        if (sourceBands != null) {
            for (int sourceBand : sourceBands) {
                if (sourceBand >= raster.getNumBands()) {
                    throw new IllegalArgumentException("Invalid band");
                }
            }
        }

        raster = raster.createChild(
                sourceRegion.x,
                sourceRegion.y,
                width,
                height,
                0,
                0,
                sourceBands
        );

        width = (width + sourceXSubsampling - 1) / sourceXSubsampling;
        height = (height + sourceYSubsampling - 1) / sourceYSubsampling;

        byte[] pixels = new byte[width * height * bandCount];

        int[] pixel = new int[bandCount];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // TODO: Should we here convert the pixel using the ColorModel?
                raster.getPixel(x * sourceXSubsampling, y * sourceYSubsampling, pixel);

                int i = (y * width + x) * bandCount;

                pixels[i] = (byte) pixel[0];
                pixels[i + 1] = (byte) pixel[1];
                pixels[i + 2] = (byte) pixel[2];

                if (bandCount == 4) {
                    pixels[i + 3] = (byte) pixel[3];
                }
            }

            processImageProgress(y * 100F / height);

            if (abortRequested()) {
                processWriteAborted();

                return;
            }
        }

        writeImage(QOIUtil.createFromPixelData(pixels, width, height, bandCount));
    }

    private void writeImage(@NotNull QOIImage converted) throws IOException {
        ImageOutputStream output = (ImageOutputStream) this.output;

        QOIUtil.writeImage(converted, new WrappedImageOutputStream(output));

        output.flush();

        processImageComplete();
    }

    public static QOIImage createFromRenderedImage(@NotNull RenderedImage image) {
        if (image instanceof BufferedImage) {
            return createFromBufferedImage((BufferedImage) image);
        }

        return createFromRaster(image.getData(), image.getColorModel());
    }

    private static QOIImage createFromBufferedImage(@NotNull BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Try use data buffer directly, if possible
        {
            WritableRaster raster = image.getRaster();

            if (raster.getClass().getName().equals("sun.awt.image.ByteInterleavedRaster") &&
                    raster.getMinX() == 0 &&
                    raster.getMinY() == 0 &&
                    raster.getWidth() == width &&
                    raster.getHeight() == height) {
                ColorModel colorModel = image.getColorModel();

                if (!colorModel.isAlphaPremultiplied()) {
                    byte[] buffer = ((DataBufferByte) raster.getDataBuffer()).getData();

                    SampleModel model = raster.getSampleModel();

                    if (model instanceof PixelInterleavedSampleModel &&
                            model.getTransferType() == DataBuffer.TYPE_BYTE &&
                            model.getWidth() == width &&
                            model.getHeight() == height &&
                            model.getNumBands() == 3 &&
                            ((PixelInterleavedSampleModel) model).getPixelStride() == 3 &&
                            ((PixelInterleavedSampleModel) model).getScanlineStride() == 3 * width &&
                            Arrays.equals(((PixelInterleavedSampleModel) model).getBandOffsets(), QOIImageReader.OFFSETS_3) &&
                            buffer.length == width * height * 3) {
                        return QOIUtil.createFromPixelData(buffer, width, height, 3);
                    }

                    if (model instanceof PixelInterleavedSampleModel &&
                            model.getTransferType() == DataBuffer.TYPE_BYTE &&
                            model.getWidth() == width &&
                            model.getHeight() == height &&
                            model.getNumBands() == 4 &&
                            ((PixelInterleavedSampleModel) model).getPixelStride() == 4 &&
                            ((PixelInterleavedSampleModel) model).getScanlineStride() == 4 * width &&
                            Arrays.equals(((PixelInterleavedSampleModel) model).getBandOffsets(), QOIImageReader.OFFSETS_4) &&
                            buffer.length == width * height * 4) {
                        return QOIUtil.createFromPixelData(buffer, width, height, 4);
                    }
                }
            }
        }

        int channels = image.getTransparency() != Transparency.OPAQUE ? 4 : 3;

        byte[] pixelData = new byte[width * height * channels];

        switch (image.getType()) {
            case BufferedImage.TYPE_INT_ARGB: {
                assert channels == 4;

                Raster raster = image.getRaster();

                DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();

                int[] data = buffer.getData();

                int total = width * height * 4;

                for (int i = 0, j = buffer.getOffset(); i < total; i += 4, j++) {
                    int pixel = data[j];

                    pixelData[i] = (byte) (pixel >> 16);
                    pixelData[i + 1] = (byte) (pixel >> 8);
                    pixelData[i + 2] = (byte) pixel;
                    pixelData[i + 3] = (byte) (pixel >> 24);
                }

                break;
            }
            case BufferedImage.TYPE_INT_RGB: {
                assert channels == 3;

                Raster raster = image.getRaster();

                DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();

                int[] data = buffer.getData();

                int total = width * height * 3;

                for (int i = 0, j = buffer.getOffset(); i < total; i += 3, j++) {
                    int pixel = data[j];

                    pixelData[i] = (byte) (pixel >> 16);
                    pixelData[i + 1] = (byte) (pixel >> 8);
                    pixelData[i + 2] = (byte) pixel;
                }

                break;
            }
            case BufferedImage.TYPE_INT_BGR: {
                assert channels == 3;

                Raster raster = image.getRaster();

                DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();

                int[] data = buffer.getData();

                int total = width * height * 3;

                for (int i = 0, j = buffer.getOffset(); i < total; i += 3, j++) {
                    int pixel = data[j];

                    pixelData[i] = (byte) pixel;
                    pixelData[i + 1] = (byte) (pixel >> 8);
                    pixelData[i + 2] = (byte) (pixel >> 16);
                }

                break;
            }
            case BufferedImage.TYPE_3BYTE_BGR: {
                assert channels == 3;

                Raster raster = image.getRaster();

                DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();

                byte[] data = buffer.getData();

                int total = width * height * 3;

                for (int i = 0, j = buffer.getOffset(); i < total; i += 3, j += 3) {
                    pixelData[i] = data[j + 2];
                    pixelData[i + 1] = data[j + 1];
                    pixelData[i + 2] = data[j];
                }

                break;
            }
            case BufferedImage.TYPE_4BYTE_ABGR: {
                assert channels == 4;

                Raster raster = image.getRaster();

                DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();

                byte[] data = buffer.getData();

                int total = width * height * 4;

                for (int i = 0, j = buffer.getOffset(); i < total; i += 4, j += 4) {
                    pixelData[i] = data[j + 3];
                    pixelData[i + 1] = data[j + 2];
                    pixelData[i + 2] = data[j + 1];
                    pixelData[i + 3] = data[j];
                }

                break;
            }
            case BufferedImage.TYPE_BYTE_GRAY: {
                assert channels == 3;

                Raster raster = image.getRaster();

                DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();

                byte[] data = buffer.getData();

                int total = width * height * 3;

                for (int i = 0, j = buffer.getOffset(); i < total; i += 3, j++) {
                    byte value = data[j];
                    pixelData[i] = value;
                    pixelData[i + 1] = value;
                    pixelData[i + 2] = value;
                }

                break;
            }
            default:
                return createFromRaster(image.getRaster(), image.getColorModel());
        }

        return QOIUtil.createFromPixelData(pixelData, width, height, channels, QOIColorSpace.SRGB);
    }

    // Slowest method
    private static QOIImage createFromRaster(@NotNull Raster raster, @NotNull ColorModel colorModel) {
        int width = raster.getWidth();
        int height = raster.getHeight();
        int channels = colorModel.getTransparency() != Transparency.OPAQUE ? 4 : 3;

        byte[] pixelData = new byte[width * height * channels];

        Object pixel = null;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel = raster.getDataElements(x, y, pixel);

                int rgb = colorModel.getRGB(pixel);

                int i = (y * width + x) * channels;

                pixelData[i] = (byte) (rgb >> 16);
                pixelData[i + 1] = (byte) (rgb >> 8);
                pixelData[i + 2] = (byte) rgb;

                if (channels == 4) {
                    pixelData[i + 3] = (byte) (rgb >> 24);
                }
            }
        }

        return QOIUtil.createFromPixelData(pixelData, width, height, channels, QOIColorSpace.SRGB);
    }

}
