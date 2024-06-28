package io.github.xiaozhuai.jetbrains.qoi;

import org.jetbrains.annotations.NotNull;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an {@link ImageInputStream} into an {@link InputStream}.
 */
final class WrappedImageInputStream extends InputStream {

    private final ImageInputStream input;

    public WrappedImageInputStream(@NotNull ImageInputStream input) {
        this.input = input;
    }

    @Override
    public int read() throws IOException {
        return this.input.read();
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        return this.input.read(b);
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        return this.input.read(b, off, len);
    }

}
