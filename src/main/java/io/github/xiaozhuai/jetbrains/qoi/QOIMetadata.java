package io.github.xiaozhuai.jetbrains.qoi;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

public class QOIMetadata extends IIOMetadata {
    public static final String QOI_FORMAT_LOWER_CASE = "qoi";
    public static final String QOI_FORMAT_UPPER_CASE = "QOI";
    public static final String[] QOI_FORMAT_NAMES = new String[]{QOI_FORMAT_UPPER_CASE, QOI_FORMAT_LOWER_CASE};
    public static final String EXT_QOI = QOI_FORMAT_LOWER_CASE;
    public static final String[] QOI_SUFFIXES = new String[]{EXT_QOI};
    public static final String[] QOI_MIME_TYPES = new String[]{"image/qoi"};
    public static final String QOI_VENDOR = "QOI";

    public static final String QOI_LIBRARY_VERSION = "1.2.1";

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Node getAsTree(String formatName) {
        return new IIOMetadataNode(nativeMetadataFormatName);
    }

    @Override
    public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
    }

    @Override
    public void reset() {
    }
}
