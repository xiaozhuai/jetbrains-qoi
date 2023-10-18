package io.github.xiaozhuai.jetbrains.qoi;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import java.util.List;

public class QOIMetadataRegistrar implements AppLifecycleListener, DynamicPluginListener {
    private static final ImageReaderSpi IMAGE_READER_PROVIDER = new QOIImageReaderSpi();
    private static final ImageWriterSpi IMAGE_WRITER_PROVIDER = new QOIImageWriterSpi();

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        ensureQoiRegistered();
    }

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        ensureQoiRegistered();
    }

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        deRegisterQoi();
    }

    public static void ensureQoiRegistered() {
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        defaultInstance.registerServiceProvider(IMAGE_READER_PROVIDER, ImageReaderSpi.class);
        defaultInstance.registerServiceProvider(IMAGE_WRITER_PROVIDER, ImageWriterSpi.class);
    }

    public void deRegisterQoi() {
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        defaultInstance.deregisterServiceProvider(IMAGE_READER_PROVIDER, ImageReaderSpi.class);
        defaultInstance.deregisterServiceProvider(IMAGE_WRITER_PROVIDER, ImageWriterSpi.class);
    }
}
