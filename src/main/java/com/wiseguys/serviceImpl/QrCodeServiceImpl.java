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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public byte[] generateQrPng(String qrCodeValue, int size) {
        return generateQrPng(qrCodeValue, null, size);
    }

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

    private static final int DPI = 300;
    private static final int CARD_W = 1240;
    private static final int CARD_H = 1748;

    // Theme colours (same as the website / admin)
    private static final Color MAROON = new Color(0x3B, 0x14, 0x10);
    private static final Color MAROON_DARK = new Color(0x2A, 0x0D, 0x0A);
    private static final Color CREAM = new Color(0xED, 0xE7, 0xDA);
    private static final Color GOLD = new Color(0xC9, 0xA9, 0x6A);
    private static final Color HEADER_BG = MAROON;
    private static final int HEADER_H = 400;
    private static final int LOGO_MAX_W = 980;
    private static final int LOGO_MAX_H = 280;
    private static final String LOGO_RESOURCE = "/static/images/logo.png";
    private static final String FONT_RESOURCE = "/static/fonts/card-font.ttf";

    private static final String HEADING = "Scan the QR menu to order";
    private static final String FOOTER = "wiseguys";
    private static final int HEADING_SIZE = 62;
    private static final int TABLE_SIZE = 54;
    private static final int FOOTER_SIZE = 38;

    private static final int QR_SIZE = 720;
    private static final int PANEL_PAD = 70;           // white quiet zone around the QR
    private static final int PANEL_TOP = 610;           // pushed down to leave a clear gap below the logo header

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
            g.setFont(fitFont(1040, HEADING_SIZE, 36, g, HEADING, Font.BOLD));
            g.setColor(MAROON);
            drawCentered(g, HEADING, CARD_W / 2, HEADER_H + 10, PANEL_TOP - (HEADER_H + 10));

            // White panel with gold border (the only gold border on the card), then the QR on top
            int panelSize = QR_SIZE + 2 * PANEL_PAD;
            int panelX = (CARD_W - panelSize) / 2;
            g.setColor(GOLD);
            g.fill(new RoundRectangle2D.Float(panelX - 8, PANEL_TOP - 8, panelSize + 16, panelSize + 16, 56, 56));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(panelX, PANEL_TOP, panelSize, panelSize, 48, 48));
            g.drawImage(qr, panelX + PANEL_PAD, PANEL_TOP + PANEL_PAD, null);

            // Table name in a plain maroon pill (no outline)
            int pillH = 104;
            int pillTop = PANEL_TOP + panelSize + 50;
            g.setFont(fitFont(700, TABLE_SIZE, 32, g, tableLabel, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            int pillW = Math.min(CARD_W - 240, fm.stringWidth(tableLabel) + 140);
            int pillX = (CARD_W - pillW) / 2;
            g.setColor(MAROON);
            g.fill(new RoundRectangle2D.Float(pillX, pillTop, pillW, pillH, pillH, pillH));
            g.setColor(CREAM);
            drawCentered(g, tableLabel, CARD_W / 2, pillTop, pillH);

            // Small footer text, centred in the space below the pill
            g.setFont(baseFont().deriveFont(Font.PLAIN, (float) FOOTER_SIZE));
            g.setColor(new Color(MAROON.getRed(), MAROON.getGreen(), MAROON.getBlue(), 200));
            int zoneTop = pillTop + pillH;
            drawCentered(g, FOOTER, CARD_W / 2, zoneTop, CARD_H - zoneTop);
        } finally {
            g.dispose();
        }
        return card;
    }

    private void drawLogoOrName(Graphics2D g) {
        BufferedImage img = loadLogo();
        if (img == null) {
            // No logo file found: fall back to the name in text
            g.setFont(fitFont(LOGO_MAX_W, 130, 40, g, "WiseGuy's", Font.BOLD));
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
                            BufferedImage raw = ImageIO.read(in);
                            logo = raw == null ? null : trimEmptyEdges(raw);
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

    /**
     * Crops transparent (or flat background-coloured) margins around the logo.
     * Empty padding inside the PNG is what makes a logo look small and off-centre.
     */
    private static BufferedImage trimEmptyEdges(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int ref = src.getRGB(0, 0);
        boolean refOpaque = (ref >>> 24) > 200;

        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isContent(src.getRGB(x, y), ref, refOpaque)) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return src; // nothing found, keep as is
        }
        return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isContent(int argb, int ref, boolean refOpaque) {
        if ((argb >>> 24) < 20) return false;
        if (!refOpaque) return true;
        int dr = Math.abs(((argb >> 16) & 0xFF) - ((ref >> 16) & 0xFF));
        int dg = Math.abs(((argb >> 8) & 0xFF) - ((ref >> 8) & 0xFF));
        int db = Math.abs((argb & 0xFF) - (ref & 0xFF));
        return dr > 24 || dg > 24 || db > 24;
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

    /** Resolved once; every piece of text on the card uses this same family. */
    private static final class CardFont {
        static final Font BASE = load();

        private static Font load() {
            // 1) A bundled font file wins, so the card looks identical on every server
            try (InputStream in = QrCodeServiceImpl.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in != null) {
                    return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.PLAIN, 1f);
                }
            } catch (IOException | FontFormatException ignored) {
                // fall through to installed fonts
            }
            // 2) First installed font from the preferred list
            Set<String> installed = new HashSet<>();
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                installed.add(name.toLowerCase());
            }
            String[] preferred = {"Georgia", "Playfair Display", "Cambria", "Times New Roman", "DejaVu Serif", "Liberation Serif"};
            for (String name : preferred) {
                if (installed.contains(name.toLowerCase())) {
                    return new Font(name, Font.PLAIN, 1);
                }
            }
            // 3) Generic serif
            return new Font(Font.SERIF, Font.PLAIN, 1);
        }
    }

    private static Font baseFont() {
        return CardFont.BASE;
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