package io.github.xiaozhuai.jetbrains.qoi;

import me.saharnooby.qoi.*;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Locale;

public class QOIImageReaderSpi extends ImageReaderSpi {
    private static final byte[] QOI_HEADER = {'q', 'o', 'i', 'f'};

    private static final int MAX_FILE_SIZE = 0x06400000;  // 100 MiBs

    QOIImageReaderSpi() {
        vendorName = QOIMetadata.QOI_VENDOR;
        version = QOIMetadata.QOI_LIBRARY_VERSION;
        suffixes = QOIMetadata.QOI_SUFFIXES;
        names = QOIMetadata.QOI_FORMAT_NAMES;
        MIMETypes = QOIMetadata.QOI_MIME_TYPES;
        pluginClassName = QOIReader.class.getName();
        inputTypes = new Class<?>[]{ImageInputStream.class};
    }

    @Override
    public boolean canDecodeInput(@NotNull Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) return false;

        ImageInputStream stream = (ImageInputStream) source;
        long length = stream.length();
        // The length may be -1 for files of unknown size.
        // Accept them for now and if needed, throw an IOException later.
        if (length > MAX_FILE_SIZE) {
            return false;
        }

        stream.mark();
        try {
            byte[] header = new byte[4];
            int bytesRead = stream.read(header, 0, 4);
            return bytesRead == 4
                    && arrayEquals(header, 0, QOI_HEADER.length, QOI_HEADER);
        } finally {
            try {
                stream.reset();
            } catch (IOException e) {
                Logger.getInstance(QOIImageReaderSpi.class).error(e);
            }
        }
    }

    private static boolean arrayEquals(byte[] a1, int offset, int len, byte[] a2) {
        for (int i = 0; i < len; i++) {
            if (a1[offset + i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ImageReader createReaderInstance(Object extension) {
        return new QOIReader(this);
    }

    @Override
    public @NotNull String getDescription(Locale locale) {
        return "QOI Image Decoder";
    }

    private static class QOIReader extends ImageReader {
        private static final String UNABLE_TO_READ_QOI_IMAGE = "Unable to read qoi image";

        private QOIImage image = null;

        private QOIReader(ImageReaderSpi originatingProvider) {
            super(originatingProvider);
        }

        @Override
        public void dispose() {
            image = null;
        }

        @Override
        public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
            super.setInput(input, seekForwardOnly, ignoreMetadata);
            try {
                byte[] bytes = readStreamFully((ImageInputStream) input);
                image = QOIUtil.readImage(new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                image = null;
            }
        }

        @Override
        public int getNumImages(boolean allowSearch) {
            return image == null ? 0 : 1;
        }

        private static byte[] readStreamFully(@NotNull ImageInputStream stream) throws IOException {
            if (stream.length() != -1) {
                byte[] bytes = new byte[(int) stream.length()];  // Integer overflow prevented by canDecode check in reader spi above.
                stream.readFully(bytes);
                return bytes;
            }

            // Unknown file size
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(0x100000);  // initialize with 1 MiB to minimize reallocation.
            final int bufferSize = 0x4000;    // 16k
            byte[] bytes = new byte[bufferSize];
            int idx;
            for (idx = 0; idx < MAX_FILE_SIZE / bufferSize; idx++) {  // Just to make sure we don't exceed MAX_FILE_SIZE
                int read = stream.read(bytes, 0, bufferSize);
                buffer.write(bytes, 0, read);
                if (read != bufferSize) {
                    break;
                }
            }
            if (idx == MAX_FILE_SIZE / bufferSize) {
                throw new IOException("qoi image too large");
            }
            return buffer.toByteArray();
        }

        @Override
        public int getWidth(int imageIndex) throws IOException {
            if (image != null) {
                return image.getWidth();
            }
            throw new IOException(UNABLE_TO_READ_QOI_IMAGE);
        }

        @Override
        public int getHeight(int imageIndex) throws IOException {
            if (image != null) {
                return image.getHeight();
            }
            throw new IOException(UNABLE_TO_READ_QOI_IMAGE);
        }

        @Override
        public @Nullable Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
            return null;
        }

        @Override
        public @Nullable IIOMetadata getStreamMetadata() {
            return null;
        }

        @Override
        public @Nullable IIOMetadata getImageMetadata(int imageIndex) {
            return null;
        }

        @Override
        public @NotNull BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
            if (image == null) {
                throw new IOException(UNABLE_TO_READ_QOI_IMAGE);
            }
            byte[] pixels = image.getPixelData();

            if (image.getChannels() == 4) {
                // rgba to argb
                for (int i = 0; i < image.getWidth() * image.getHeight(); ++i) {
                    byte alpha = pixels[i * 4 + 3];
                    pixels[i * 4 + 3] = pixels[i * 4 + 2];
                    pixels[i * 4 + 2] = pixels[i * 4 + 1];
                    pixels[i * 4 + 1] = pixels[i * 4];
                    pixels[i * 4] = alpha;
                }
                @SuppressWarnings("UndesirableClassUsage")
                BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                final int[] a = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                IntBuffer buf = ByteBuffer.wrap(pixels).asIntBuffer();
                assert a.length == buf.remaining();
                buf.get(a);
                return bi;
            } else if (image.getChannels() == 3) {
                // swap r b channel
                for (int i = 0; i < image.getWidth() * image.getHeight(); ++i) {
                    byte tmp = pixels[i * 3 + 2];
                    pixels[i * 3 + 2] = pixels[i * 3];
                    pixels[i * 3] = tmp;
                }
                @SuppressWarnings("UndesirableClassUsage")
                BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                final byte[] a = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
                ByteBuffer buf = ByteBuffer.wrap(pixels);
                assert a.length == buf.remaining();
                buf.get(a);
                return bi;
            } else {
                throw new IOException(UNABLE_TO_READ_QOI_IMAGE);
            }
        }
    }
}
