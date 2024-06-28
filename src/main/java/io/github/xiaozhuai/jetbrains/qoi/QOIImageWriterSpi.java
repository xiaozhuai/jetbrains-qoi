package io.github.xiaozhuai.jetbrains.qoi;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.util.Locale;

import static io.github.xiaozhuai.jetbrains.qoi.QOIPluginConstants.*;

public final class QOIImageWriterSpi extends ImageWriterSpi {

    public QOIImageWriterSpi() {
        super(
                QOI_VENDOR_NAME,
                QOI_VERSION,
                QOI_FORMAT_NAMES,
                QOI_SUFFIXES,
                QOI_MIME_TYPES,
                QOIImageWriter.class.getName(),
                new Class[]{ImageOutputStream.class},
                new String[]{QOIImageReaderSpi.class.getName()},
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
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        int bands = type.getNumBands();

        for (int i = 0; i < bands; i++) {
            if (type.getBitsPerBand(i) != 8) {
                return false;
            }
        }

        return bands == 3 || bands == 4;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new QOIImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "QOI image writer";
    }

}
