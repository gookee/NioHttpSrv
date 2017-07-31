package com.core.server;

public class Controller {
    private NioRequest request;
    private NioResponse response;
    private String controllerName;
    private String actionName;

    public NioRequest getRequest() {
        return request;
    }

    public void setRequest(NioRequest request) {
        this.request = request;
    }

    public NioResponse getResponse() {
        return response;
    }

    public void setResponse(NioResponse response) {
        this.response = response;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    private String[] routeValue;

    public String[] getRouteValue() {
        return routeValue;
    }

    public void setRouteValue(String[] routeValue) {
        this.routeValue = routeValue;
    }

    public String getRouteValue(int index) {
        if (index < this.routeValue.length)
            return this.routeValue[index];
        else
            return "";
    }

    public String getView() {
        String path = this.actionName.equalsIgnoreCase("index") ? this.controllerName : (this.controllerName.split("/")[0] + "/" + this.actionName);
        return getView(path);
    }

    public String getView(String path) {
        try {
            return new String(StaticFile.readFileContent(path + ".html", "html"), NioCore.charsetName);
        } catch (Exception e) {
            return "";
        }
    }
}
