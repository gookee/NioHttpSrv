package com.core.helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

public class VerifyCode {
    private Random random = new Random();
    private String randString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";// 随机产生的字符串

    private int width = 80;// 图片宽
    private int height = 26;// 图片高
    private int lineSize = 40;// 干扰线数量
    private int stringNum = 4;// 随机产生字符数量
    private String sessionKey;
    private byte[] imageBytes;

    public String getSessionKey() {
        return sessionKey;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    private Color getRandColor(int fc, int bc) {
        if (fc > 255)
            fc = 255;
        if (bc > 255)
            bc = 255;
        int r = fc + random.nextInt(bc - fc - 16);
        int g = fc + random.nextInt(bc - fc - 14);
        int b = fc + random.nextInt(bc - fc - 18);
        return new Color(r, g, b);
    }

    public void getVerifyCode() {
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_BGR);
        Graphics g = image.getGraphics();
        g.fillRect(0, 0, width, height);
        g.setFont(new Font("Times New Roman", Font.ROMAN_BASELINE, 20));
        g.setColor(getRandColor(110, 133));

        for (int i = 0; i <= lineSize; i++) {
            drowLine(g);
        }

        sessionKey = "";
        for (int i = 1; i <= stringNum; i++) {
            sessionKey = drowString(g, sessionKey, i);
        }
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "GIF", out);
            imageBytes = out.toByteArray();
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
        }
    }

    private String drowString(Graphics g, String randomString, int i) {
        g.setFont(new Font("Fixedsys", Font.CENTER_BASELINE, 20));
        g.setColor(new Color(random.nextInt(101), random.nextInt(111), random
                .nextInt(121)));
        String rand = String.valueOf(getRandomString(random.nextInt(randString
                .length())));
        randomString += rand;
        g.translate(random.nextInt(3), random.nextInt(3));
        g.drawString(rand, 13 * i, 15 + random.nextInt(4));
        return randomString;
    }

    private void drowLine(Graphics g) {
        int x = random.nextInt(width);
        int y = random.nextInt(height);
        int xl = random.nextInt(13);
        int yl = random.nextInt(15);
        g.drawLine(x, y, x + xl, y + yl);
    }

    private String getRandomString(int num) {
        return String.valueOf(randString.charAt(num));
    }
}
