package com.wiseguys.serviceImpl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.wiseguys.serviceI.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${app.base-url}")
    private String baseUrl;

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /** Plain QR with no label (kept so existing callers don't break). */
    @Override
    public byte[] generateQrPng(String qrCodeValue, int size) {
        return generateQrPng(qrCodeValue, null, size);
    }

    /**
     * With a label (e.g. "Table 5") this returns the full A6 table card:
     * logo, "Scan this QR code to order", the QR code, and the table name.
     * The card has a fixed A6 size at 300 DPI, so {@code size} is only used for the plain QR.
     */
    @Override
    public byte[] generateQrPng(String qrCodeValue, String label, int size) {
        String content = resolveMenuUrl(qrCodeValue);
        try {
            if (label == null || label.isBlank()) {
                BufferedImage qr = encodeQr(content, size, 2, Color.BLACK.getRGB(), Color.WHITE.getRGB());
                return toPng(qr);
            }
            return toPng(drawCard(content, label.trim()));
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
    // Card design (all sizes in pixels at 300 DPI; A6 = 105 x 148 mm)
    // ---------------------------------------------------------------------

    private static final int DPI = 300;
    private static final int CARD_W = 1240;
    private static final int CARD_H = 1748;

    // Theme colours (same as the website / admin)
    private static final Color MAROON = new Color(0x3B, 0x14, 0x10);
    private static final Color MAROON_DARK = new Color(0x2A, 0x0D, 0x0A);
    private static final Color CREAM = new Color(0xED, 0xE7, 0xDA);
    private static final Color GOLD = new Color(0xC9, 0xA9, 0x6A);

    // Header band that holds the logo. Change to CREAM if your logo is dark on a light background.
    private static final Color HEADER_BG = MAROON;
    private static final int HEADER_H = 360;
    private static final int LOGO_MAX_W = 780;
    private static final int LOGO_MAX_H = 220;
    private static final String LOGO_RESOURCE = "/static/images/logo.png";

    private static final String HEADING = "Scan this QR code to order";

    private static final int QR_SIZE = 740;
    private static final int PANEL_PAD = 80;           // white quiet zone around the QR
    private static final int PANEL_TOP = 520;

    private volatile BufferedImage logo;
    private volatile boolean logoLoaded;

    private BufferedImage drawCard(String content, String tableLabel) throws WriterException {
        BufferedImage qr = encodeQr(content, QR_SIZE, 0, MAROON_DARK.getRGB(), Color.WHITE.getRGB());

        BufferedImage card = new BufferedImage(CARD_W, CARD_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Plain grayscale smoothing: LCD sub-pixel smoothing adds colour fringes to printed text
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Background
            g.setColor(CREAM);
            g.fillRect(0, 0, CARD_W, CARD_H);

            // Header band + logo + gold rule
            g.setColor(HEADER_BG);
            g.fillRect(0, 0, CARD_W, HEADER_H);
            drawLogoOrName(g);
            g.setColor(GOLD);
            g.fillRect(0, HEADER_H, CARD_W, 10);

            // Heading
            Font headingFont = fitFont(1000, 76, 40, g, HEADING, Font.BOLD);
            g.setFont(headingFont);
            g.setColor(MAROON);
            drawCentered(g, HEADING, CARD_W / 2, HEADER_H + 10, PANEL_TOP - (HEADER_H + 10));

            // White panel with gold border, then the QR on top
            int panelSize = QR_SIZE + 2 * PANEL_PAD;
            int panelX = (CARD_W - panelSize) / 2;
            g.setColor(GOLD);
            g.fill(new RoundRectangle2D.Float(panelX - 8, PANEL_TOP - 8, panelSize + 16, panelSize + 16, 56, 56));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(panelX, PANEL_TOP, panelSize, panelSize, 48, 48));
            g.drawImage(qr, panelX + PANEL_PAD, PANEL_TOP + PANEL_PAD, null);

            // Table name in a maroon pill
            int pillH = 130;
            int pillTop = PANEL_TOP + panelSize + 50;
            Font tableFont = fitFont(800, 96, 40, g, tableLabel, Font.BOLD);
            g.setFont(tableFont);
            FontMetrics fm = g.getFontMetrics();
            int pillW = Math.min(CARD_W - 160, fm.stringWidth(tableLabel) + 160);
            int pillX = (CARD_W - pillW) / 2;
            g.setColor(MAROON);
            g.fill(new RoundRectangle2D.Float(pillX, pillTop, pillW, pillH, pillH, pillH));
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(5f));
            g.draw(new RoundRectangle2D.Float(pillX + 2.5f, pillTop + 2.5f, pillW - 5, pillH - 5, pillH - 5, pillH - 5));
            g.setColor(CREAM);
            drawCentered(g, tableLabel, CARD_W / 2, pillTop, pillH);

            // Small website line at the bottom
            String site = siteLabel();
            if (!site.isEmpty()) {
                Font siteFont = baseFont().deriveFont(Font.PLAIN, 40f);
                g.setFont(siteFont);
                g.setColor(new Color(MAROON.getRed(), MAROON.getGreen(), MAROON.getBlue(), 190));
                int zoneTop = pillTop + pillH;
                drawCentered(g, site, CARD_W / 2, zoneTop, CARD_H - 30 - zoneTop);
            }

            // Thin gold frame around the whole card
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(5f));
            g.draw(new RoundRectangle2D.Float(28, 28, CARD_W - 56, CARD_H - 56, 40, 40));
        } finally {
            g.dispose();
        }
        return card;
    }

    private void drawLogoOrName(Graphics2D g) {
        BufferedImage img = loadLogo();
        if (img == null) {
            // No logo file found: fall back to the name in text
            Font f = fitFont(LOGO_MAX_W, 120, 40, g, "WiseGuy's", Font.BOLD);
            g.setFont(f);
            g.setColor(GOLD);
            drawCentered(g, "WiseGuy's", CARD_W / 2, 0, HEADER_H);
            return;
        }
        double scale = Math.min((double) LOGO_MAX_W / img.getWidth(), (double) LOGO_MAX_H / img.getHeight());
        int lw = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int lh = Math.max(1, (int) Math.round(img.getHeight() * scale));
        BufferedImage scaled = scaleSmooth(img, lw, lh);
        g.drawImage(scaled, (CARD_W - lw) / 2, (HEADER_H - lh) / 2, null);
    }

    /** Downscales in halving steps so large logos stay smooth instead of jagged. */
    private static BufferedImage scaleSmooth(BufferedImage src, int targetW, int targetH) {
        BufferedImage cur = src;
        while (cur.getWidth() / 2 >= targetW && cur.getHeight() / 2 >= targetH) {
            cur = resize(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        }
        return resize(cur, targetW, targetH);
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private BufferedImage loadLogo() {
        if (!logoLoaded) {
            synchronized (this) {
                if (!logoLoaded) {
                    try (InputStream in = QrCodeServiceImpl.class.getResourceAsStream(LOGO_RESOURCE)) {
                        if (in != null) {
                            logo = ImageIO.read(in);
                        }
                    } catch (IOException ignored) {
                        // fall back to text
                    }
                    logoLoaded = true;
                }
            }
        }
        return logo;
    }

    private String siteLabel() {
        if (baseUrl == null) return "";
        return baseUrl.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private BufferedImage encodeQr(String content, int size, int margin, int onColor, int offColor)
            throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, margin);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig(onColor, offColor));
    }

    /** Georgia to match the site; falls back to the JVM's generic serif font if not installed. */
    private static Font baseFont() {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase("Georgia")) {
                return new Font("Georgia", Font.PLAIN, 1);
            }
        }
        return new Font(Font.SERIF, Font.PLAIN, 1);
    }

    /** Largest font (up to startSize) whose text fits in maxWidth. */
    private static Font fitFont(int maxWidth, int startSize, int minSize, Graphics2D g, String text, int style) {
        FontRenderContext frc = g.getFontRenderContext();
        int size = startSize;
        Font font = baseFont().deriveFont(style, (float) size);
        while (font.getStringBounds(text, frc).getWidth() > maxWidth && size > minSize) {
            size -= 2;
            font = font.deriveFont((float) size);
        }
        return font;
    }

    /** Draws text horizontally centred on cx, and vertically centred inside the zone [zoneTop, zoneTop+zoneH]. */
    private static void drawCentered(Graphics2D g, String text, int cx, int zoneTop, int zoneH) {
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        int y = zoneTop + (zoneH - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
    }

    /** Writes a PNG and stores 300 DPI in it, so printing it at "actual size" gives A6. */
    private static byte[] toPng(BufferedImage img) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), param);

            String mmPerPixel = Double.toString(25.4 / DPI);
            IIOMetadataNode h = new IIOMetadataNode("HorizontalPixelSize");
            h.setAttribute("value", mmPerPixel);
            IIOMetadataNode v = new IIOMetadataNode("VerticalPixelSize");
            v.setAttribute("value", mmPerPixel);
            IIOMetadataNode dim = new IIOMetadataNode("Dimension");
            dim.appendChild(h);
            dim.appendChild(v);
            IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
            root.appendChild(dim);
            meta.mergeTree("javax_imageio_1.0", root);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(img, null, meta), param);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}