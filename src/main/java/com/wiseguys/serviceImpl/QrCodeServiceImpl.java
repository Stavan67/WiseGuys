package com.wiseguys.serviceImpl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.wiseguys.serviceI.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${app.base-url}")
    private String baseUrl;

    /** Original method: plain QR, no label (kept so existing callers don't break). */
    @Override
    public byte[] generateQrPng(String qrCodeValue, int size) {
        return generateQrPng(qrCodeValue, null, size);
    }

    /** New: QR with a label (e.g. "Table 5") drawn below it. */
    @Override
    public byte[] generateQrPng(String qrCodeValue, String label, int size) {
        String content = resolveMenuUrl(qrCodeValue);
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage qr = MatrixToImageWriter.toBufferedImage(matrix);

            BufferedImage result = (label == null || label.isBlank())
                    ? qr
                    : addLabelBelow(qr, label.trim());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(result, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code for value " + qrCodeValue, e);
        }
    }

    @Override
    public String resolveMenuUrl(String qrCodeValue) {
        // URL-encode so values with spaces/special characters don't break the link
        return baseUrl + "/menu?table=" + URLEncoder.encode(qrCodeValue, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------

    // Plain black/white to match the QR itself — no brand colour, so it stays crisp when printed or scanned
    private static final Color LABEL_COLOR = Color.BLACK;
    private static final Color DIVIDER_COLOR = new Color(0xDD, 0xDD, 0xDD);

    /** Georgia to match the site; falls back to the JVM's generic serif font if not installed. */
    private static Font baseFont(int size) {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase("Georgia")) {
                return new Font("Georgia", Font.BOLD, size);
            }
        }
        return new Font(Font.SERIF, Font.BOLD, size);
    }

    private BufferedImage addLabelBelow(BufferedImage qr, String label) {
        int w = qr.getWidth();
        int labelHeight = Math.max(48, w / 5);          // strip under the QR
        int h = qr.getHeight() + labelHeight;

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.drawImage(qr, 0, 0, null);

            // Thin gold divider between QR and label
            int lineW = (int) (w * 0.5);
            g.setColor(DIVIDER_COLOR);
            g.fillRect((w - lineW) / 2, qr.getHeight() + 2, lineW, Math.max(2, w / 200));

            // Largest font that fits the width (90%) and the strip height (55%)
            int maxTextWidth = (int) (w * 0.9);
            int fontSize = (int) (labelHeight * 0.55);
            Font font = baseFont(fontSize);
            FontRenderContext frc = g.getFontRenderContext();
            while (font.getStringBounds(label, frc).getWidth() > maxTextWidth && fontSize > 10) {
                fontSize -= 2;
                font = font.deriveFont((float) fontSize);
            }

            g.setFont(font);
            g.setColor(LABEL_COLOR);
            FontMetrics fm = g.getFontMetrics();
            int x = (w - fm.stringWidth(label)) / 2;
            int y = qr.getHeight() + (labelHeight - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(label, x, y);
        } finally {
            g.dispose();
        }
        return canvas;
    }
}