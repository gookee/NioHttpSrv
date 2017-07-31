package com.core.server;

import com.core.helper.Utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AccessControl {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String execAccessControl(NioRequest request) {
        try {
            Class cls = Class.forName("com.interceptor.AccessControl");
            if (cls == null)
                return "";

            String url = request.getUrl().split("\\?")[0];
            if (cls != null) {
                Constructor ct = cls.getConstructor((Class[]) null);
                Object obj = ct.newInstance((Object[]) null);
                Method[] methods = cls.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method m = methods[i];
                    AccessAttr pa = m.getAnnotation(AccessAttr.class);
                    if (pa != null) {
                        if (CheckContainUrl(url, pa)) {
                            String result = String.valueOf(m.invoke(obj, new Object[]{request}));
                            if (!result.equals("")) {
                                return result;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Utility.getLogger(AccessControl.class).error("", e);
        }

        return "";
    }

    public static String execContentControl(NioRequest request, String html) {
        try {
            Class cls = Class.forName("com.interceptor.ContentControl");
            if (cls == null)
                return html;

            String url = request.getUrl().split("\\?")[0];
            if (cls != null) {
                Constructor ct = cls.getConstructor((Class[]) null);
                Object obj = ct.newInstance((Object[]) null);
                Method[] methods = cls.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    AccessAttr pa = method.getAnnotation(AccessAttr.class);
                    if (pa != null) {
                        if (CheckContainUrl(url, pa)) {
                            return String.valueOf(method.invoke(obj, new Object[]{request, html}));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Utility.getLogger(AccessControl.class).error("", e);
        }

        return html;
    }

    static boolean CheckContainUrl(String url, AccessAttr pa) {
        String urlContain = pa.urlContain();
        String urlFilter = pa.urlFilter();
        if (!urlContain.trim().equals("")
                && Contained(url, urlContain.split(","))
                && (urlFilter.trim().equals("") || !Contained(url,
                urlFilter.split(",")))) {
            return true;
        } else
            return false;
    }

    static boolean Contained(String url, String[] u) {
        if (u == null)
            return false;

        for (int i = 0; i < u.length; i++) {
            if (!u[i].trim().equals("")
                    && url.trim().toLowerCase().startsWith(u[i].trim().toLowerCase()))
                return true;
        }
        return false;
    }
}