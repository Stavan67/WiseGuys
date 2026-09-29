package com.wiseguys.serviceI;

public interface QrCodeService {
    byte[] generateQrPng(String qrCodeValue, int size);

    /** QR code with a text label (e.g. the table name) drawn below it. */
    byte[] generateQrPng(String qrCodeValue, String label, int size);

    String resolveMenuUrl(String qrCodeValue);
}