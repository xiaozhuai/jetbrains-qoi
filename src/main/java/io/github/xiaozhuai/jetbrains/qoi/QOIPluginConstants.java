package io.github.xiaozhuai.jetbrains.qoi;

final class QOIPluginConstants {
    static final byte[] QOI_HEADER = {'q', 'o', 'i', 'f'};
    static final String QOI_VENDOR_NAME = "QOI";
    static final String QOI_VERSION = "1.2.1";
    static final String QOI_EXTENSION = "qoi";
    static final String[] QOI_FORMAT_NAMES = {QOI_EXTENSION, QOI_EXTENSION.toUpperCase()};
    static final String[] QOI_SUFFIXES = {QOI_EXTENSION};
    static final String[] QOI_MIME_TYPES = new String[]{"image/qoi"};
}
