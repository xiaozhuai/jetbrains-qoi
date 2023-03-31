package io.github.xiaozhuai.jetbrains.qoi;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

public class QOIMetadataRegistrar {
    public QOIMetadataRegistrar() {
        ensureQoiRegistered();
    }

    public static void ensureQoiRegistered() {
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        defaultInstance.registerServiceProvider(new QOIImageReaderSpi(), ImageReaderSpi.class);
        defaultInstance.registerServiceProvider(new QOIImageWriterSpi(), ImageWriterSpi.class);
    }
}
