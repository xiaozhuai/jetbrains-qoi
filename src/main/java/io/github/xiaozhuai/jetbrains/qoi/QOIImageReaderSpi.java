package io.github.xiaozhuai.jetbrains.qoi;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

import static io.github.xiaozhuai.jetbrains.qoi.QOIPluginConstants.*;

public final class QOIImageReaderSpi extends ImageReaderSpi {

    public QOIImageReaderSpi() {
        super(
                QOI_VENDOR_NAME,
                QOI_VERSION,
                QOI_FORMAT_NAMES,
                QOI_SUFFIXES,
                QOI_MIME_TYPES,
                QOIImageReader.class.getName(),
                new Class[]{ImageInputStream.class},
                new String[]{QOIImageWriterSpi.class.getName()},
                // Standard stream metadata is not supported
                false,
                null,
                null,
                null,
                null,
                // Standard image metadata is not supported
                false,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream in = (ImageInputStream) source;
        byte[] b = new byte[4];
        in.mark();
        in.readFully(b);
        in.reset();

        return b[0] == QOI_HEADER[0]
                && b[1] == QOI_HEADER[1]
                && b[2] == QOI_HEADER[2]
                && b[3] == QOI_HEADER[3];
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new QOIImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "QOI image reader";
    }

}
