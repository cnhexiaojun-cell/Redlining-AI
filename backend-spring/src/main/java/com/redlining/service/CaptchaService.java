package com.redlining.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService implements CaptchaServiceInterface {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final long EXPIRE_MS = 5 * 60 * 1000L; // 5 minutes

    private final Random random = new Random();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private static class Entry {
        final String code;
        final long createdAt;

        Entry(String code) {
            this.code = code;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    public Map<String, String> createCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = generateCode();
        store.put(captchaId, new Entry(code));
        removeExpired();

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // noise lines
        g.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 4; i++) {
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        int x = 12;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(80) + 50, random.nextInt(80) + 50, random.nextInt(80) + 50));
            g.drawString(String.valueOf(code.charAt(i)), x, 28);
            x += 26;
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return Map.of(
                    "captchaId", captchaId,
                    "image", "data:image/png;base64," + base64
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }

    @Override
    public boolean validate(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() - entry.createdAt > EXPIRE_MS) {
            return false;
        }
        return entry.code.equalsIgnoreCase(userInput.trim());
    }

    private void removeExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > EXPIRE_MS);
    }
}
