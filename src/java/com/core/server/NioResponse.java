package com.core.server;

import com.core.helper.Utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NioResponse {
    private SelectionKey key;
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private NioRequest request;
    private String contentType;
    private List<Cookie> cookieList = new ArrayList<>();
    private boolean isSendHeader = false;
    private boolean isCanGzip = true;
    private int statusCode = 200;
    private String etag = "";
    private Map<String, String> headerMap = new HashMap();

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setETag(String etag) {
        this.etag = etag;
    }

    public String getETag() {
        return etag;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addHeader(String name, String value) {
        headerMap.put(name, value);
    }

    public NioResponse(SelectionKey key, NioRequest request) {
        this.key = key;
        this.request = request;
    }

    public void write(byte[] bytes) {
        byteArrayOutputStream.write(bytes, 0, bytes.length);
    }

    public void write(String html) {
        try {
            write(html.getBytes(NioCore.charsetName));
        } catch (Exception e) {
        }
    }

    public void addCookie(Cookie cookie) {
        cookieList.add(cookie);
    }

    public void flush() {
        isCanGzip = false;
        send();
    }

    public void end() {
        try {
            send();

            byteArrayOutputStream.close();
            SocketChannel channel = (SocketChannel) key.channel();
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
            ChunkedStream chunkedStream = new ChunkedStream(tmpOut);
            chunkedStream.close();
            ByteBuffer byteBuffer = ByteBuffer.wrap(tmpOut.toByteArray());
            while (byteBuffer.hasRemaining())
                channel.write(byteBuffer);

            if (key.isValid())
                key.cancel();
            channel.shutdownInput();
            channel.shutdownOutput();
            channel.close();
        } catch (IOException e) {
        }
    }

    void send() {
        try {
            sendHeader();

            byte[] outBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream = new ByteArrayOutputStream();
            outBytes = execHtmlParse(outBytes);
            if (outBytes == null)
                return;

            if (isCanGzip && request.checkGizpCompress()) {
                outBytes = compress(outBytes);
            }

            SocketChannel channel = (SocketChannel) key.channel();
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
            ChunkedStream chunkedStream = new ChunkedStream(tmpOut);
            chunkedStream.write(outBytes);
            ByteBuffer byteBuffer = ByteBuffer.wrap(tmpOut.toByteArray());
            while (byteBuffer.hasRemaining()) {
                channel.write(byteBuffer);
            }
        } catch (IOException e) {
        }
    }

    byte[] execHtmlParse(byte[] bytes) {
        try {
            if (bytes.length > 0 && contentType != null && (contentType.equalsIgnoreCase("text/html") || contentType.equalsIgnoreCase("text/css") || contentType.equalsIgnoreCase("text/javascript") || contentType.equalsIgnoreCase("application/json"))) {
                String outHtml = new String(bytes, NioCore.charsetName);
                outHtml = AccessControl.execContentControl(request, outHtml);
                return outHtml.getBytes(NioCore.charsetName);
            }
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
        }

        return bytes;
    }

    String getStatusStr() {
        switch (statusCode) {
            case 200:
                return "OK";
            case 304:
                return "Not Modified";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            default:
                return "OK";
        }
    }

    void sendHeader() {
        if (isSendHeader)
            return;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 " + statusCode + " " + getStatusStr() + "\r\n");
            if (isCanGzip && request.checkGizpCompress())
                addHeader("Content-Encoding", "gzip");
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie != null && cookie.isCreate())
                        cookieList.add(cookie);
                }
            }
            for (Cookie cookie : cookieList)
                addHeader("Set-Cookie", cookie.toString());
            addHeader("Transfer-Encoding", "chunked");
            addHeader("Connection", "keep-alive");
            if (!etag.equals("")) {
                addHeader("Cache-Control", "max-age=36593149");
                addHeader("Last-Modified", getGMTString(System.currentTimeMillis()));
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                int PAGE_EXPIRES = Utility.toInt(Utility.getConfigValue("PAGE_EXPIRES", "604800"));
                c.add(Calendar.SECOND, PAGE_EXPIRES);
                addHeader("Expires", getGMTString(c.getTimeInMillis()));
                addHeader("ETag", "\"" + etag + "\"");
            }
            addHeader("Content-Type", contentType);
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                sb.append(header.getKey() + ": " + header.getValue() + "\r\n");
            }
            sb.append("\r\n");

            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer headByteBuffer = ByteBuffer.wrap(sb.toString().getBytes(NioCore.charsetName));
            while (headByteBuffer.hasRemaining())
                channel.write(headByteBuffer);

            isSendHeader = true;
        } catch (IOException e) {
        }
    }

    String getGMTString(Long millSec) {
        Date date = new Date(millSec);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    byte[] compress(byte[] bytes) {
        if (bytes.length == 0)
            return bytes;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(bytes);
            gzip.close();
        } catch (IOException e) {
            Utility.getLogger(this.getClass()).error("", e);
        }
        return out.toByteArray();
    }

    byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Utility.getLogger(this.getClass()).error("", e);
        }

        return out.toByteArray();
    }
}