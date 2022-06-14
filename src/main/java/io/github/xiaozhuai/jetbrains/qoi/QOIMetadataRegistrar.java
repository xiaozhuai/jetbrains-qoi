package io.github.xiaozhuai.jetbrains.qoi;

import com.intellij.openapi.diagnostic.Logger;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

public class QOIMetadataRegistrar {
    private QOIMetadataRegistrar() {
        ensureQoiRegistered();
    }

    public static void ensureQoiRegistered() {
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        defaultInstance.registerServiceProvider(new QOIImageReaderSpi(), ImageReaderSpi.class);
        defaultInstance.registerServiceProvider(new QOIImageWriterSpi(), ImageWriterSpi.class);
    }
}
