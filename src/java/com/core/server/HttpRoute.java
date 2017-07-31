package com.core.server;

import java.util.ArrayList;
import java.util.List;

public class HttpRoute {
    public static List<RouteInfo> httpRoute = new ArrayList<RouteInfo>();

    public static void mapRoute(String url, String controller, String action) {
        RouteInfo ri = new RouteInfo();
        ri.setUrl(url);
        ri.setController(controller);
        ri.setAction(action);
        httpRoute.add(ri);
    }

    static class RouteInfo {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        private String controller;

        public String getController() {
            return controller;
        }

        public void setController(String controller) {
            this.controller = controller;
        }

        private String action;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
