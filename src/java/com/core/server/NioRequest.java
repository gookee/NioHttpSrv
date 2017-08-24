package com.core.server;

import com.core.helper.Utility;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NioRequest {
    private ParameterList list = new ParameterList();
    private String host = "";
    private String url = "";
    private String remoteAddr = "";
    private boolean isGizpCompress = false;
    private byte[] bytes = null;
    private String content = "";
    private String boundary = "";
    private String headerStr = "";
    private String postParams = "";
    private SelectionKey key;
    private String httpMethod = "";
    private String path = "";
    private Cookie[] cookies;
    private Session session;
    private Map<String, Object> attribute = new HashMap<>();
    private String controllerName = "";
    private String actionName = "";
    private String routeValue = "";
    private NioResponse response = null;
    private boolean disableCache = false;

    public NioRequest(SelectionKey key) {
        this.key = key;
        Map<String, Object> obj = (Map<String, Object>) key.attachment();
        ByteArrayOutputStream baos = (ByteArrayOutputStream) obj.get("baos");
        this.bytes = baos.toByteArray();
        this.response = new NioResponse(key, this);
    }

    public String isReceiveOver() {
        int STREAM_SIZE = Utility.toInt(Utility.getConfigValue("STREAM_SIZE", "10240"));
        try {
            this.content = new String(bytes, NioCore.charsetName);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            return sw.toString();
        }

        Pattern pattern = Pattern.compile("(GET|POST) (.+?) HTTP");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            this.httpMethod = matcher.group(1);
            this.url = matcher.group(2);
            this.path = Utility.trim(url, '/').split("\\?")[0];
            if (matcher.group(1).equals("POST")) {
                String[] tmp = content.split("\\r\\n");
                String headerStr = "";
                for (String item : tmp) {
                    headerStr += item + "\r\n";
                    if (item.equals(""))
                        break;
                }

                String boundary = getBoundary(headerStr);
                this.boundary = boundary;
                int contentLength = Utility.toInt(getValueByPattern("Content-Length: (.+)", headerStr));
                if (contentLength > STREAM_SIZE * 1024 || bytes.length - headerStr.length() > STREAM_SIZE * 1024) {
                    return "STREAM_SIZE in excess of " + STREAM_SIZE + " KB";
                }

                if (boundary.equals("")) {
                    if ((contentLength > 0 && bytes.length - headerStr.length() < contentLength) || !content.contains("\r\n\r\n") || (contentLength > 0 && content.endsWith("\r\n\r\n")))
                        return "";
                } else {
                    if (!content.endsWith("--" + boundary + "--\r\n"))
                        return "";
                }

                this.headerStr = headerStr;
                this.postParams = content.substring(headerStr.length());
            } else {
                if (!content.endsWith("\r\n\r\n"))
                    return "";

                this.headerStr = content;
            }
        }

        return null;
    }

    public void doSrv() {
        Utility.threadLocalRequest.set(this);
        Utility.threadLocalResponse.set(response);
        this.host = getValueByPattern("Host: (.+)", headerStr);
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            this.remoteAddr = sc.getRemoteAddress().toString();
        } catch (Exception e) {
        }
        this.isGizpCompress = getValueByPattern("Accept-Encoding: (.+)", headerStr).contains("gzip");
        getParamValues();

        if (!this.httpMethod.equals("")) {
            if (execAccessControl(response))
                return;

            String ext = StaticFile.getExt(this.url);
            execRoute(ext);
            if (!this.controllerName.equals(""))
                execute(response);
            else if (ext.equals("")) {
                response.setStatusCode(404);
                response.setContentType("application/json");
                response.write("{\"msg\":\"controller not found\"}");
                response.end();
            } else
                parseStaticFile(response, ext);
        } else {
            response.setStatusCode(400);
            response.setContentType("application/json");
            response.write("{\"msg\":\"not support httpMethod\"}");
            response.end();
        }

    }

    boolean execAccessControl(NioResponse response) {
        String result = AccessControl.execAccessControl(this);
        if (!result.equals("")) {
            if (result.startsWith("[") || result.startsWith("{"))
                response.setContentType("application/json");
            else
                response.setContentType("text/html");
            response.setStatusCode(200);
            response.write(result);
            response.end();

            return true;
        }

        return false;
    }

    void execRoute(String ext) {
        if (HttpRoute.httpRoute.size() == 0) {
            String[] tmp = path.split("/");
            if (tmp.length == 1 && !tmp[0].equals("")) {
                controllerName = tmp[0] + "/index";
            } else if (tmp.length == 2) {
                controllerName = tmp[0] + "/" + tmp[1];
            } else if (tmp.length >= 3) {
                controllerName = tmp[0] + "/" + tmp[1];
                actionName = tmp[2];
            }
            StringBuilder arr = new StringBuilder();
            for (int i = 3; i < tmp.length; i++) {
                if (i > 3)
                    arr.append("/");
                arr.append(tmp[i]);
            }
            routeValue = arr.toString();
        } else {
            for (HttpRoute.RouteInfo ri : HttpRoute.httpRoute) {
                String reStr = ri.getUrl().replaceAll("(?i)\\{controller\\}", "([^/\\.]+)/([^/\\.]+)").replaceAll("(?i)\\{action\\}", "([^/\\.]+)");
                Pattern re = Pattern.compile(reStr + (ext.equals("") ? "(/.+|)" : "(/.+(?<!" + ext + ")$|)"), Pattern.CASE_INSENSITIVE);
                Matcher m = re.matcher("/" + this.path);
                if (m.matches()) {
                    if (m.groupCount() == 0) {
                        controllerName = ri.getController();
                        actionName = ri.getAction();
                        routeValue = "";
                    } else if (ri.getUrl().contains("{controller}")) {
                        controllerName = m.group(1) + "/" + m.group(2);
                        if (ri.getUrl().contains("{action}")) {
                            actionName = m.group(3);
                            routeValue = Utility.trimStart(m.group(4), '/');
                        } else {
                            actionName = ri.getAction();
                            routeValue = Utility.trimStart(m.group(3), '/');
                        }
                    } else if (ri.getUrl().contains("{action}")) {
                        controllerName = ri.getController();
                        actionName = m.group(1);
                        routeValue = Utility.trimStart(m.group(2), '/');
                    } else {
                        controllerName = ri.getController();
                        actionName = ri.getAction();
                        routeValue = Utility.trimStart(m.group(1), '/');
                    }

                    break;
                }
            }
        }
    }

    Controller findController() {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL url = loader.getResource("com/controller/");
            File urlFile = new File(url.toURI());
            File[] files = urlFile.listFiles();
            String[] controller = controllerName.split("/");
            for (File f : files) {
                if (f.getName().equalsIgnoreCase(controller[0])) {
                    files = f.listFiles();
                    for (File fi : files) {
                        if (fi.getName().equalsIgnoreCase(controller[1] + ".class")) {
                            Controller obj = (Controller) Class.forName("com.controller." + f.getName() + "." + fi.getName().substring(0, fi.getName().length() - 6)).newInstance();
                            if (obj != null) {
                                return obj;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    void execute(NioResponse response) {
        try {
            Controller obj = findController();
            if (obj != null) {
                obj.setRequest(this);
                obj.setResponse(response);
                obj.setControllerName(controllerName);
                obj.setActionName(actionName);
                obj.setRouteValue(routeValue.split("/"));
                Method[] methods = obj.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    boolean isHttpMethod = false;
                    if (httpMethod.equals("POST")) {
                        isHttpMethod = method.getAnnotation(HttpMethod.HttpPost.class) != null;
                    } else {
                        isHttpMethod = method.getAnnotation(HttpMethod.HttpPost.class) == null;
                    }
                    if (method.getName().equalsIgnoreCase(actionName) && method.getReturnType() == String.class && isHttpMethod) {
                        Object[] parameTypes = method.getParameterTypes();
                        Object[] paramValues = new Object[parameTypes.length];
                        String result = Utility.toStr(method.invoke(obj, paramValues));
                        if (result.startsWith("[") || result.startsWith("{"))
                            response.setContentType("application/json");
                        else
                            response.setContentType("text/html");
                        response.setStatusCode(200);
                        response.write(result);
                        response.end();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Utility.getLogger(this.getClass()).error("", e);
            response.setStatusCode(500);
            response.setContentType("application/json");
            response.write("{\"msg\":\"internal error\"}");
            response.end();
            return;
        }

        String path = actionName.equalsIgnoreCase("index") ? (controllerName + ".html") : (controllerName.split("/")[0] + "/" + actionName + ".html");
        byte[] html = StaticFile.readFileContent(path, "html");
        if (html != null && html.length > 0) {
            response.setStatusCode(200);
            response.setContentType("text/html");
            response.write(html);
            response.end();
        } else {
            response.setStatusCode(404);
            response.setContentType("application/json");
            response.write("{\"msg\":\"action not found\"}");
            response.end();
        }
    }

    boolean isExpires(String gmtStr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date d = format.parse(gmtStr);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.YEAR, 1);
            if (c.getTimeInMillis() >= System.currentTimeMillis())
                return false;
            else
                return true;
        } catch (Exception e) {
            return true;
        }
    }

    void parseStaticFile(NioResponse response, String ext) {
        response.setContentType(MIME.getMimeByPath(this.path));
        String headerEtag = Utility.trim(getValueByPattern("If-None-Match: (.+)", this.headerStr), '\"');
        String headerExpires = getValueByPattern("If-Modified-Since: (.+)", this.headerStr);
        String etag = StaticFile.getETag(this.path);
        if (!this.disableCache && this.attribute.size() == 0 && etag.equals(headerEtag) && !isExpires(headerExpires)) {
            response.setStatusCode(304);
            response.end();
            return;
        }
        response.setETag(etag);
        boolean isFileSend = StaticFile.readFileContent(response, this.path, ext);
        if (!isFileSend) {
            byte[] html = StaticFile.readFileContent(this.path, ext);
            if (html != null && html.length > 0) {
                response.setStatusCode(200);
                response.write(html);
                response.end();
            } else {
                response.setStatusCode(404);
                response.setContentType("application/json");
                response.write("{\"msg\":\"file not found\"}");
                response.end();
            }
        }
    }

    void execCookie(boolean create) {
        String cookieHeader = getValueByPattern("Cookie: (.+)", this.headerStr);
        if (!cookieHeader.equals("")) {
            cookies = Cookie.saxToCookie(cookieHeader);
            String jsessionid = Cookie.getJSessionId(cookieHeader);
            if (jsessionid != null)
                session = Session.getSession(jsessionid);
        }
        if (create && session == null) {
            if (cookies == null) {
                cookies = new Cookie[1];
            } else {
                cookies = new Cookie[cookies.length + 1];
            }
            Cookie cookie = new Cookie(create);
            String jsessionid = UUID.randomUUID().toString();
            cookie.setName(Cookie.JSESSIONID);
            cookie.setPath("/");
            cookie.setValue(jsessionid);
            cookies[cookies.length - 1] = cookie;
            session = new Session(jsessionid);
            Session.setSession(jsessionid, session);
        }
    }

    public Cookie[] getCookies() {
        if (cookies == null)
            execCookie(false);
        return cookies;
    }

    public Session getSession() {
        if (session == null)
            execCookie(true);
        return session;
    }

    public boolean getDisableCache() {
        return this.disableCache;
    }

    public void setDisableCache(boolean disableCache) {
        this.disableCache = disableCache;
    }

    public NioResponse getResponse() {
        return this.response;
    }

    public boolean checkGizpCompress() {
        return this.isGizpCompress;
    }

    public String getRemoteAddr() {
        return this.remoteAddr;
    }

    public String getRemoteIp() {
        int lastIndex = this.remoteAddr.lastIndexOf(":");
        return this.remoteAddr.substring(1, lastIndex);
    }

    public String getHost() {
        return this.host;
    }

    public String getUrl() {
        return this.url;
    }

    public String getPath() {
        return this.path;
    }

    public String getHttpMethod() {
        return this.httpMethod;
    }

    public void setAttribute(String key, Object value) {
        this.attribute.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attribute.get(key);
    }

    public int getRequestInt(String key) {
        return Utility.toInt(getParameter(key));
    }

    public String getRequestStr(String key) {
        return getRequestStr(key, true);
    }

    public String getRequestStr(String key, boolean filterSql) {
        if (filterSql)
            return Utility.filterSQL(Utility.toStr(getParameter(key)));
        else
            return Utility.toStr(getParameter(key));
    }

    public Date getRequestDate(String key) {
        return getRequestDate(key, "yyyy-MM-dd HH:mm:ss");
    }

    public Date getRequestDate(String key, String format) {
        return Utility.toDate(getRequestStr(key), format);
    }

    String getParameter(String key) {
        return list.get(key);
    }

    public Map<String, String> getRequestMap() {
        return getRequestMap(true);
    }

    public Map<String, String> getRequestMap(boolean filterSql) {
        Map<String, String> map = new ConcurrentHashMap<>();
        list.getList().parallelStream().forEach(c -> {
            map.put(c.getKey(), filterSql ? Utility.filterSQL(c.getValue()) : c.getValue());
        });
        return map;
    }

    public <T> T getRequestList(Class<?> clazz) {
        return getRequestList(clazz, true);
    }

    public <T> T getRequestList(Class<?> clazz, boolean filterSql) {
        final String[] json = {""};
        list.getList().parallelStream().forEach(c -> {
            json[0] += "\"" + c.getKey() + "\":\"" + (filterSql ? Utility.filterSQL(c.getValue()) : c.getValue()) + "\",";
        });
        json[0] = json[0].substring(0, json[0].length() - 1) + "}";
        return Utility.jsonToBean(clazz, json[0]);
    }

    public Map<String, File> saveFiles(String path) {
        return saveFiles(path, false);
    }

    public Map<String, File> saveFiles(String path, boolean autoReName) {
        Map<String, File> map = new HashMap<>();
        Pattern p = Pattern.compile("(--" + boundary + "\\r\\n" +
                "Content-Disposition: form-data; name=\"(.+?)\"; filename=\"(.+?)\"\\r\\n" +
                "Content-Type: (.+?)\\r\\n\\r\\n)([\\s\\S]+)(--" + boundary + "--)");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String filename = m.group(3);
            if (autoReName) {
                String ext = StaticFile.getExt(filename);
                filename = UUID.randomUUID().toString() + ext;
            }
            String headStr = content.substring(0, content.indexOf(m.group()));
            String footStr = "";
            if (content.indexOf(m.group()) < content.length() - m.group().length())
                footStr = content.substring(content.indexOf(m.group()) + m.group().length());
            File f = new File(Utility.getRootPath() + "/" + Utility.trim(path, '/', '\\', ' ') + "/" + filename);
            if (!f.getParentFile().isDirectory())
                f.getParentFile().mkdirs();
            try {
                int len = bytes.length - headStr.getBytes(NioCore.charsetName).length - footStr.getBytes(NioCore.charsetName).length - m.group(1).getBytes(NioCore.charsetName).length - m.group(6).getBytes(NioCore.charsetName).length - "\r\n".length();
                byte[] bs = new byte[len];
                System.arraycopy(bytes, headStr.getBytes(NioCore.charsetName).length + m.group(1).getBytes(NioCore.charsetName).length, bs, 0, len);
                FileOutputStream fos = new FileOutputStream(f);
                FileChannel ch = fos.getChannel();
                ByteBuffer byteBuffer = ByteBuffer.wrap(bs);
                ch.write(byteBuffer);
                ch.close();
                fos.close();

                map.put(m.group(2), f);
            } catch (Exception e) {
                Utility.getLogger(this.getClass()).error("", e);
            }
        }

        return map;
    }

    void getParamValues() {
        String[] tmp = url.split("\\?");
        if (tmp.length > 1) {
            parseParamValues(url.substring(tmp[0].length() + 1, url.length()));
        }

        if (!postParams.equals("")) {
            if (boundary.equals("")) {
                parseParamValues(postParams);
            } else {
                String body = postParams.substring(boundary.length() + "\r\n".length() + 2, postParams.length() - boundary.length() - "\r\n".length() - 4);
                parseBoundaryValues(body);
            }
        }
    }

    void parseParamValues(String str) {
        String[] tmp = str.split("&");
        for (int i = 0; i < tmp.length; i++) {
            String[] t = tmp[i].split("=");
            if (t.length > 1)
                try {
                    list.put(t[0], URLDecoder.decode(tmp[i].substring(t[0].length() + 1, tmp[i].length()), NioCore.charsetName));
                } catch (Exception e) {
                }
        }
    }

    void parseBoundaryValues(String body) {
        String[] tmp = body.split("\\r\\n--" + boundary + "\\r\\n");
        for (String item : tmp) {
            String[] itemTmp = item.split("\\r\\n");
            String name = getValueByPattern("name=\"(.+?)\"", itemTmp[0]);
            if (!name.equalsIgnoreCase("file")) {
                String tmpStr = "";
                for (String it : itemTmp) {
                    if (it.equals(""))
                        break;

                    tmpStr += it + "\r\n";
                }
                String value = item.substring(tmpStr.length() + "\r\n".length());
                try {
                    list.put(name, URLDecoder.decode(value, NioCore.charsetName));
                } catch (Exception e) {
                    list.put(name, value);
                }
            }
        }
    }

    String getBoundary(String content) {
        Pattern pattern = Pattern.compile("Content-Type: ([^;]+?); boundary=(.+)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase("multipart/form-data"))
                return matcher.group(2);
            else
                return "";
        } else
            return "";
    }

    String getValueByPattern(String re, String content) {
        Matcher matcher = Pattern.compile(re, Pattern.CASE_INSENSITIVE).matcher(content);
        if (matcher.find())
            return matcher.group(1);
        else
            return "";
    }
}