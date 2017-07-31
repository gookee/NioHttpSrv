package com.core.helper;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.alibaba.fastjson.JSON;
import com.core.server.NioCore;
import com.core.server.NioRequest;
import com.core.server.NioResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Utility {
    public static ThreadLocal<NioRequest> threadLocalRequest = new ThreadLocal<NioRequest>();
    public static ThreadLocal<NioResponse> threadLocalResponse = new ThreadLocal<NioResponse>();
    public static String fileSeparator = System.getProperty("file.separator");
    private static Logger logger = null;

    public static NioRequest getRequest() {
        return threadLocalRequest.get();
    }

    public static NioResponse getResponse() {
        return threadLocalResponse.get();
    }

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return (str == null || str.trim().equals("") || str.length() == 0) ? true : false;
    }

    public static String getConfigValue(String name) {
        return getConfigValue(name, "");
    }

    public static String getConfigValue(String name, String defaultValue) {
        if (CacheManager.hasCache(name))
            return toStr(CacheManager.get(name));
        else {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder dbd = dbf.newDocumentBuilder();
                Document doc = dbd.parse(new FileInputStream(getRootPath() + fileSeparator + "resources" + fileSeparator + "config.xml"));
                XPathFactory f = XPathFactory.newInstance();
                XPath path = f.newXPath();
                String value = (String) path.evaluate("config/" + name, doc, XPathConstants.STRING);
                if (!value.equals(""))
                    defaultValue = value;
            } catch (Exception e) {
            }

            CacheManager.put(name, defaultValue, 60);
            return defaultValue;
        }
    }

    public static String getRootPath() {
        try {
            return URLDecoder.decode(ClassLoader.getSystemResource("").getPath(), NioCore.charsetName);
        } catch (Exception e) {
            return "";
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        if (logger == null) {
            logger = LoggerFactory.getLogger(clazz);
            System.setProperty("logbackdir", getRootPath());
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.reset();
            try {
                configurator.doConfigure(Utility.getRootPath() + Utility.fileSeparator + "resources" + Utility.fileSeparator + "logback.xml");
            } catch (JoranException e) {
                Utility.getLogger(Utility.class).error("", e);
            }
            StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        }

        return logger;
    }

    public static String toStr(Object str) {
        return toStr(str, "");
    }

    public static String toStr(Object str, String defaultValue) {
        String Result = String.valueOf(str);
        if (Result.equals("null"))
            return defaultValue;
        else
            return Result;
    }

    public static int toInt(Object str) {
        return toInt(str, 0);
    }

    public static int toInt(Object str, int defaultValue) {
        String tmp = String.valueOf(str);
        if (tmp.equals("null"))
            return defaultValue;
        else {
            try {
                return Integer.parseInt(tmp);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    public static float toFloat(Object str) {
        return toFloat(str, 0, -1);
    }

    public static float toFloat(Object str, float defaultValue) {
        return toFloat(str, defaultValue, -1);
    }

    public static float toFloat(Object str, int pointPos) {
        return toFloat(str, 0, pointPos);
    }

    public static float toFloat(Object str, float defaultValue, int pointPos) {
        String tmp = String.valueOf(str);
        float Result = defaultValue;
        if (tmp.equals("null"))
            Result = defaultValue;
        else {
            try {
                Result = Float.parseFloat(tmp);
            } catch (Exception e) {
                Result = defaultValue;
            }
        }

        if (pointPos != -1) {
            return new BigDecimal(Result).setScale(pointPos, BigDecimal.ROUND_HALF_UP).floatValue();
        } else {
            return Result;
        }
    }

    public static Date toDate(Object str) {
        return toDate(str, null, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date toDate(Object str, Date defaultValue) {
        return toDate(str, defaultValue, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date toDate(Object str, String formatStr) {
        return toDate(str, null, formatStr);
    }

    public static Date toDate(Object str, Date defaultValue, String formatStr) {
        SimpleDateFormat formatter = new SimpleDateFormat(formatStr);
        String tmp = String.valueOf(str);
        if (tmp.equals("null")) {
            try {
                return formatter.parse(formatter.format(defaultValue));
            } catch (ParseException e) {
                return null;
            }
        } else {
            try {
                return formatter.parse(tmp);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    public static String getDate() {
        return getDate("yyyy-MM-dd HH:mm:ss");
    }

    public static String getDate(String formatStr) {
        Calendar cal = Calendar.getInstance();
        return dateFormat(cal.getTime(), formatStr);
    }

    public static String dateFormat(Date date, String formatStr) {
        SimpleDateFormat formatter = new SimpleDateFormat(formatStr);
        return formatter.format(date);
    }

    public static String jsonEncode(String json) {
        return json.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r\n", "\\u000d\\u000a").replace("\n", "\\u000a");
    }

    public static String jsonDecode(String json) {
        return json.replace("\\'", "'").replace("\\\"", "\"")
                .replace("\\u000d\\u000a", "\r\n").replace("\\u000a", "\n");
    }

    public static <T> String beanToJson(T bean) {
        return JSON.toJSONString(bean);
    }

    public static <T> T jsonToBean(Class<?> clazz, String json) {
        return (T) JSON.parseObject(json, clazz);
    }

    public static String mapToJson(Map map) {
        return JSON.toJSONString(map);
    }

    public static Map jsonToMap(String json) {
        return (Map) JSON.parseObject(json, Map.class);
    }

    public static <T> String listToJson(List<T> list) {
        return JSON.toJSONString(list);
    }

    public static <T> List<T> jsonToList(Class<?> clazz, String json) {
        return (List<T>) JSON.parseArray(json, clazz);
    }

    // / <summary>
    // / 防sql注入处理
    // / </summary>
    // / <param name="sql">输入字符串</param>
    // / <returns>关键字符转全角处理</returns>
    public static String filterSQL(String sql) {
        return sql.replaceAll("(?i)and", "ａｎｄ")
                .replaceAll("(?i)or", "ｏｒ")
                .replaceAll("(?i)exec", "ｅｘｅｃ")
                .replaceAll("(?i)insert", "ｉｎｓｅｒｔ")
                .replaceAll("(?i)select", "ｓｅｌｅｃｔ")
                .replaceAll("(?i)delete", "ｄｅｌｅｔｅ")
                .replaceAll("(?i)update", "ｕｐｄａｔｅ")
                .replaceAll("(?i)chr", "ｃｈｒ")
                .replaceAll("(?i)mid", "ｍｉｄ")
                .replaceAll("(?i)truncate", "ｔｒｕｎｃａｔｅ")
                .replaceAll("(?i)char", "ｃｈａｒ")
                .replaceAll("(?i)declare", "ｄｅｃｌａｒｅ")
                .replaceAll("(?i)join", "ｊｏｉｎ").replaceAll("(?i)cmd", "ｃｍｄ")
                .replaceAll("(?i)xp_", "ｘｐ＿").replaceAll("(?i)sp_", "ｓｐ＿")
                .replaceAll("(?i)0x", "０ｘ").replace("'", "＇")
                .replace("\"", "＂").replace("<", "＜").replace(">", "＞")
                .replace(";", "；").replace("--", "－－").replace("&", "＆")
                .replace("@", "＠").replace("%", "％").replace("#", "＃");
    }

    // / <summary>
    // / 半角转全角
    // / </summary>
    // / <param name="input">被转换字符串</param>
    // / <returns>结果</returns>
    public static String toSBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 32) {
                c[i] = (char) 12288;
                continue;
            }
            if (c[i] < 127)
                c[i] = (char) (c[i] + 65248);
        }
        return new String(c);
    }

    // / <summary>
    // / 全角转半角
    // / </summary>
    // / <param name="input">被转换字符串</param>
    // / <returns>结果</returns>
    public static String toDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 12288) {
                c[i] = (char) 32;
                continue;
            }
            if (c[i] > 65280 && c[i] < 65375)
                c[i] = (char) (c[i] - 65248);
        }
        return new String(c);
    }

    // / <summary>
    // / 去除HTML标签
    // / </summary>
    // / <param name="Htmlstring">HTML字符串</param>
    // / <returns>去除HTML标签后的字符串</returns>
    public static String noHTML(String Htmlstring) {
        if (Htmlstring == "")
            return "";
        Htmlstring = Htmlstring.replace("\r\n", "")
                .replaceAll("(?!)<script.*?</script>", "")
                .replaceAll("(?!)<style.*?</style>", "")
                .replaceAll("<.*?>", "").replaceAll("<(.[^>]*)>", "")
                .replaceAll("[\\s]+", "").replace("-->", "")
                .replaceAll("<!--.*", "").replaceAll("(?!)&(quot|#34);", "\"")
                .replaceAll("(?!)&(amp|#38);", "&")
                .replaceAll("(?!)&(lt|#60);", "<")
                .replaceAll("(?!)&(gt|#62);", ">")
                .replaceAll("(?!)&(nbsp|#160);", "")
                .replaceAll("(?!)&(iexcl|#161);", "\\xa1")
                .replaceAll("(?!)&(cent|#162);", "\\xa2")
                .replaceAll("(?!)&(pound|#163);", "\\xa3")
                .replaceAll("(?!)&(copy|#169);", "\\xa9")
                .replaceAll("&#(\\d+);", "").replace("<", "").replace(">", "")
                .replace("<", "");
        return Htmlstring;
    }

    public static String trim(String str, char... trimChars) {
        if (str == null || str.equals("")) {
            return str;
        }
        if (trimChars == null || trimChars.length == 0) {
            return str;
        }

        for (int i = 0; i < trimChars.length; i++) {
            String strTrim = toStr(trimChars[i]).toLowerCase();
            if (str.toLowerCase().startsWith(strTrim)) {
                str = str.substring(1, str.length());
            }

            if (str.toLowerCase().endsWith(strTrim)) {
                str = str.substring(0, str.length() - 1);
            }
        }

        return str;
    }

    public static String trimStart(String str, char... trimChars) {
        if (str == null || str.equals("")) {
            return str;
        }
        if (trimChars == null || trimChars.length == 0) {
            return str;
        }

        for (int i = 0; i < trimChars.length; i++) {
            String strTrim = toStr(trimChars[i]).toLowerCase();
            if (str.toLowerCase().startsWith(strTrim)) {
                str = str.substring(1, str.length());
            }
        }

        return str;
    }

    public static String trimEnd(String str, char... trimChars) {
        if (str == null || str.equals("")) {
            return str;
        }
        if (trimChars == null || trimChars.length == 0) {
            return str;
        }

        for (int i = 0; i < trimChars.length; i++) {
            String strTrim = toStr(trimChars[i]).toLowerCase();
            if (str.toLowerCase().endsWith(strTrim)) {
                str = str.substring(0, str.length() - 1);
            }
        }

        return str;
    }

    public static Object eval(String str) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        if (engine == null) {
            getLogger(Utility.class).error("找不到JavaScript语言执行引擎");
            return null;
        }
        try {
            return engine.eval(str);
        } catch (ScriptException e) {
            Utility.getLogger(Utility.class).error("", e);
            return null;
        }
    }

    public static String ajaxPageList(int pageIndex, int takeNum, long count, int pageCount, Boolean resizeTakeNum) {
        return ajaxPageList(pageIndex, takeNum, count, pageCount, resizeTakeNum, 5,
                "共 {count} 条记录 {pageCount} 页 每页{takeNum}条 当前第 {pageIndex} 页",
                "首页|&lt;|&gt;|末页|GO");
    }

    public static String ajaxPageList(int pageIndex, int takeNum, long count, int pageCount, Boolean resizeTakeNum, int PageNum,
                                      String PageInfoText, String PageButtonText) {
        String html = "", midhtml = "";

        if (count > 0) {
            String s0 = "首页";
            String s1 = "上页";
            String s2 = "下页";
            String s3 = "末页";
            String s4 = "GO";
            try {
                s0 = PageButtonText.split("\\|")[0];
                s1 = PageButtonText.split("\\|")[1];
                s2 = PageButtonText.split("\\|")[2];
                s3 = PageButtonText.split("\\|")[3];
                s4 = PageButtonText.split("\\|")[4];
            } catch (Exception e) {
            }

            html = PageInfoText.replace("{count}", Utility.toStr(count))
                    .replace("{pageCount}", Utility.toStr(pageCount))
                    .replace("{pageIndex}", Utility.toStr(pageIndex));
            if (!resizeTakeNum)
                html = html.replace("{takeNum}", Utility.toStr(takeNum));
            else
                html = html
                        .replace(
                                "{takeNum}",
                                "<select onchange=\"bindData("
                                        + pageIndex
                                        + ",$(this).val());\">"
                                        + "<option"
                                        + (takeNum == 5 ? " selected=\"selected\""
                                        : "")
                                        + ">5</option>"
                                        + "<option"
                                        + (takeNum == 10 ? " selected=\"selected\""
                                        : "")
                                        + ">10</option>"
                                        + "<option"
                                        + (takeNum == 15 ? " selected=\"selected\""
                                        : "")
                                        + ">15</option>"
                                        + "<option"
                                        + (takeNum == 20 ? " selected=\"selected\""
                                        : "")
                                        + ">20</option>"
                                        + "<option"
                                        + (takeNum == 25 ? " selected=\"selected\""
                                        : "")
                                        + ">25</option>"
                                        + "<option"
                                        + (takeNum == 30 ? " selected=\"selected\""
                                        : "")
                                        + ">30</option>"
                                        + "<option"
                                        + (takeNum == 35 ? " selected=\"selected\""
                                        : "")
                                        + ">35</option>"
                                        + "<option"
                                        + (takeNum == 40 ? " selected=\"selected\""
                                        : "")
                                        + ">40</option>"
                                        + "<option"
                                        + (takeNum == 45 ? " selected=\"selected\""
                                        : "")
                                        + ">45</option>"
                                        + "<option"
                                        + (takeNum == 50 ? " selected=\"selected\""
                                        : "")
                                        + ">50</option>"
                                        + "<option"
                                        + (takeNum == 100 ? " selected=\"selected\""
                                        : "")
                                        + ">100</option>"
                                        + "<option"
                                        + (takeNum == 200 ? " selected=\"selected\""
                                        : "")
                                        + ">200</option>"
                                        + "<option"
                                        + (takeNum == 500 ? " selected=\"selected\""
                                        : "")
                                        + ">500</option>"
                                        + "<option"
                                        + (takeNum == 1000 ? " selected=\"selected\""
                                        : "") + ">1000</option>"
                                        + "</select>");
            int k = PageNum / 2;
            int j = pageIndex - k;
            if (j + PageNum > pageCount)
                j = pageCount - PageNum + 1;
            if (j <= 0)
                j = 1;
            for (int i = j; i < PageNum + j; i++) {
                if (i > pageCount)
                    break;
                midhtml += " <a href=\"javascript:void(0)\" onclick=\"bindData("
                        + i
                        + ","
                        + takeNum
                        + ");\""
                        + (pageIndex == i ? " disabled class=\"current\"" : "")
                        + ">"
                        + Utility.toStr(i) + "</a> ";
            }

            html += "<span><a href=\"javascript:void(0)\" onclick=\"bindData(1,"
                    + takeNum
                    + ");\""
                    + (pageIndex <= 1 ? " disabled" : "")
                    + ">"
                    + s0
                    + "</a> <a href=\"javascript:void(0)\" onclick=\"bindData("
                    + (pageIndex <= 1 ? 1 : pageIndex - 1)
                    + ","
                    + takeNum
                    + ");\""
                    + (pageIndex <= 1 ? " disabled" : "")
                    + ">"
                    + s1
                    + "</a> "
                    + midhtml
                    + " <a href=\"javascript:void(0)\" onclick=\"bindData("
                    + (pageIndex >= pageCount ? pageIndex : pageIndex + 1)
                    + ","
                    + takeNum
                    + ");\""
                    + (pageIndex >= pageCount ? " disabled" : "")
                    + ">"
                    + s2
                    + "</a> <a href=\"javascript:void(0)\" onclick=\"bindData("
                    + pageCount
                    + ","
                    + takeNum
                    + ");\""
                    + (pageIndex >= pageCount ? " disabled" : "")
                    + ">"
                    + s3
                    + "</a> <input type=\"text\" attr=\"num\" value=\""
                    + pageIndex
                    + "\"> <a href=\"javascript:void(0)\" onclick=\"bindData($(this).parent().find('input').first().val(),"
                    + takeNum + ");\">" + s4 + "</a></span>";
        }

        return html;
    }


    public static String getSearchSql(Map<String, String> parameterMap, Class cls) {
        String sql = "";
        if (parameterMap.size() > 0) {
            Constructor ct = null;
            Method m = null;
            try {
                ct = cls.getConstructor((Class[]) null);
                m = cls.getDeclaredMethod("searchExt", String.class, String.class);
            } catch (Exception e) {
            }

            for (Map.Entry<String, String> item : parameterMap.entrySet()) {
                if (m != null) {
                    try {
                        String result = (String) m.invoke(cls, new Object[]{item.getKey(), item.getValue()});
                        sql += result;
                        if (!result.equals(""))
                            continue;
                    } catch (Exception e) {
                    }
                }
                switch (item.getKey().substring(0, 5)) {
                    case "f_i_s":
                        if (item.getValue() != "")
                            sql += " and [" + item.getKey().substring(5, item.getKey().length()) + "] >= '" + item.getValue() + "'";
                        break;
                    case "f_i_e":
                        if (item.getValue() != "")
                            sql += " and [" + item.getKey().substring(5, item.getKey().length()) + "] <= '" + item.getValue() + "'";
                        break;
                    case "f_d_s":
                        if (item.getValue() != "")
                            sql += " and [" + item.getKey().substring(5, item.getKey().length()) + "] >= " + item.getValue();
                        break;
                    case "f_d_e":
                        if (item.getValue() != "")
                            sql += " and [" + item.getKey().substring(5, item.getKey().length()) + "] <= " + item.getValue();
                        break;
                    default:
                        if (item.getKey().startsWith("f_") && item.getValue() != "")
                            sql += " and [" + item.getKey().substring(2, item.getKey().length()) + "] like '%" + item.getValue() + "%'";
                        break;
                }
            }
        }

        return sql;
    }

    public static void saveFile(InputStream input, File dst) {
        OutputStream out = null;
        try {
            if (dst.exists()) {
                out = new BufferedOutputStream(new FileOutputStream(dst, true), 2048);
            } else {
                out = new BufferedOutputStream(new FileOutputStream(dst), 2048);
            }
            byte[] buffer = new byte[2048];
            int len = 0;
            while ((len = input.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            Utility.getLogger(Utility.class).error("", e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    Utility.getLogger(Utility.class).error("", e);
                }
            }
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                    Utility.getLogger(Utility.class).error("", e);
                }
            }
        }
    }
}
