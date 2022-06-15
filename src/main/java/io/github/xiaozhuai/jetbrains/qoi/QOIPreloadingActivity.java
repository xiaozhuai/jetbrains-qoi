package io.github.xiaozhuai.jetbrains.qoi;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

public class QOIPreloadingActivity extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        ensureQoiRegistered();
    }

    public static void ensureQoiRegistered() {
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        defaultInstance.registerServiceProvider(new QOIImageReaderSpi(), ImageReaderSpi.class);
        defaultInstance.registerServiceProvider(new QOIImageWriterSpi(), ImageWriterSpi.class);
    }
}
