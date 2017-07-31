package com.core.server;

import com.core.helper.Utility;

import java.util.HashMap;

public class MIME {
    static HashMap<String, String> mime = new HashMap<String, String>();

    public static MIME init() {
        return new MIME();
    }

    public MIME() {
        if (mime.size() > 0)
            return;

        String[] mimes = Utility.getConfigValue("MIME").trim().split("\\n");
        for (String m : mimes) {
            String[] tmp = m.trim().split(",");
            if (tmp.length == 2)
                mime.put(tmp[0].trim(), tmp[1].trim());
        }
    }

    public static String getMimeByExt(String name) {
        MIME.init();
        if (mime.containsKey(name))
            return mime.get(name);
        else
            return "";
    }

    public static String getMimeByPath(String path) {
        String[] names = path.split("\\.");
        String suffix = names[names.length - 1].toLowerCase();
        return getMimeByExt(suffix);
    }
}