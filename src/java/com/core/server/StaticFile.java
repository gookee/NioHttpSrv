package com.core.server;

import com.core.helper.CacheManager;
import com.core.helper.Utility;

import java.io.*;
import java.net.URLDecoder;

public class StaticFile {
    public static String getExt(String path) {
        String[] names = path.split("\\?")[0].split("/");
        String name = "";
        if (names.length > 1)
            name = names[names.length - 1].toLowerCase();
        else
            return "";

        String[] exts = name.split("\\.");
        String ext = "";
        if (exts.length > 1)
            ext = exts[exts.length - 1].toLowerCase();
        return ext;
    }

    public static boolean readFileContent(NioResponse response, String path, String ext) {
        path = Utility.getRootPath() + "webapp" + Utility.fileSeparator + path;
        try {
            path = URLDecoder.decode(path, NioCore.charsetName);
        } catch (Exception e) {
        }
        try {
            File file = new File(path);
            int FILE_READ_SIZE = Utility.toInt(Utility.getConfigValue("FILE_READ_SIZE", "10240")) * 1024;
            if (file.length() > FILE_READ_SIZE) {
                response.setStatusCode(404);
                response.write("{\"msg\":\"file too large\"}".getBytes(NioCore.charsetName));
                response.end();
                return true;
            }

            int FILE_CHUNKED_SIZE = Utility.toInt(Utility.getConfigValue("FILE_CHUNKED_SIZE", "1024")) * 1024;
            if (file.length() < FILE_CHUNKED_SIZE)
                return false;

            if (MIME.getMimeByExt(ext).equals("")) {
                response.setStatusCode(404);
                response.write("{\"msg\":\"not support MIME\"}".getBytes(NioCore.charsetName));
                response.end();
            } else {
                response.setStatusCode(200);
                FileInputStream fileInputStream = new FileInputStream(path);
                int size = 0;
                int len = fileInputStream.available();
                byte[] bytes;
                if (len < NioCore.bufferSize)
                    bytes = new byte[len];
                else
                    bytes = new byte[NioCore.bufferSize];
                while ((size = fileInputStream.read(bytes)) != -1) {
                    response.write(bytes);
                    response.flush();

                    len -= size;
                    if (len == 0)
                        break;
                    else if (len < size)
                        bytes = new byte[fileInputStream.available()];
                }
                fileInputStream.close();
                response.end();
            }
        } catch (Exception e) {
        }

        return true;
    }

    public static String getETag(String path) {
        path = Utility.getRootPath() + "webapp" + Utility.fileSeparator + path;
        StringBuilder sb = new StringBuilder();
        sb.append("M/\"");
        File file = new File(path);
        sb.append(file.lastModified());
        sb.append('"');
        return sb.toString();
    }

    public static byte[] readFileContent(String path, String ext) {
        path = Utility.getRootPath() + "webapp" + Utility.fileSeparator + path;
        try {
            path = URLDecoder.decode(path, NioCore.charsetName);
        } catch (Exception e) {
        }
        byte[] html = null;
        int CACHE_TIMEOUT = Utility.toInt(Utility.getConfigValue("CACHE_TIMEOUT", "604800"));
        if (isCached(ext) && CACHE_TIMEOUT > 0 && CacheManager.hasCache(path)) {
            html = (byte[]) CacheManager.get(path);
        } else {
            try {
                if (MIME.getMimeByExt(ext).equals("")) {
                    html = "{\"msg\":\"not support MIME\"}".getBytes(NioCore.charsetName);
                } else {
                    FileInputStream fileInputStream = new FileInputStream(path);
                    int size = 0;
                    byte[] bytes = new byte[NioCore.bufferSize];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((size = fileInputStream.read(bytes)) != -1) {
                        baos.write(bytes, 0, size);
                    }
                    baos.close();
                    fileInputStream.close();
                    html = baos.toByteArray();
                }
            } catch (Exception e) {
                html = null;
            }
            if (CACHE_TIMEOUT > 0 && isCached(ext))
                CacheManager.put(path, html, CACHE_TIMEOUT);
        }
        return html;
    }

    static boolean isCached(String ext) {
        if (ext.equals("html") || ext.equals("htm") || ext.equals("js") || ext.equals("css"))
            return true;
        else
            return false;
    }
}
