package io.github.xiaozhuai.jetbrains.qoi;

import org.jetbrains.annotations.NotNull;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps an {@link ImageOutputStream} into an {@link OutputStream}.
 */
final class WrappedImageOutputStream extends OutputStream {

    private final ImageOutputStream output;

    public WrappedImageOutputStream(@NotNull ImageOutputStream output) {
        this.output = output;
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        this.output.write(b);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        this.output.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        this.output.write(b);
    }

}
