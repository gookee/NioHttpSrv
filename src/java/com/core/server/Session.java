package com.core.server;

import com.core.helper.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
    private static Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
    private static int sessionTimeOut = Utility.toInt(Utility.getConfigValue("SESSION_TIMEOUT", "30"));

    public static Session getSession(String sessionID) {
        Session session = sessionMap.get(sessionID);
        if (session != null) {
            if (isTimeOut(session)) {
                session.clear();
                return null;
            }

            session.setLastAcessTime(new Date());
        }
        return session;
    }

    public static void setSession(String sessionID, Session session) {
        session.setLastAcessTime(new Date());
        sessionMap.put(sessionID, session);
    }

    //每1小时检查session是否过期，并清理过期的session
    public static Runnable checkTimeOut() {
        return new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        for (Map.Entry<String, Session> item : sessionMap.entrySet()) {
                            Session session = item.getValue();
                            if (isTimeOut(session)) {
                                session.clear();
                            }
                        }

                        Thread.sleep(60 * 60 * 1000);
                    } catch (Exception e) {
                        Utility.getLogger(this.getClass()).error("", e);
                    }
                }
            }
        };
    }

    static boolean isTimeOut(Session session) {
        Date date = session.getLastAcessTime();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MINUTE, -sessionTimeOut);
        long ndate = c.getTimeInMillis();
        if (date == null || date.getTime() < ndate) {
            return true;
        }

        return false;
    }

    private String sessionId;
    private Date lastAcessTime;

    public Date getLastAcessTime() {
        return lastAcessTime;
    }

    public void setLastAcessTime(Date lastAcessTime) {
        this.lastAcessTime = lastAcessTime;
    }

    private Map<String, Object> attrMap = Collections.synchronizedMap(new HashMap<String, Object>());

    public Session(String sessionID) {
        this.sessionId = sessionID;
    }

    public void setAttribute(String name, Object value) {
        attrMap.put(name, value);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Object getAttribute(String name) {
        return attrMap.get(name);
    }

    public void removeAttribute(String name) {
        attrMap.remove(name);
    }

    public void clear() {
        sessionMap.remove(sessionId);
    }

    public Map getAttributeMap() {
        return attrMap;
    }
}