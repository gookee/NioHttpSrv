# NioHttpSrv
java Nio Http Server

#注册路由
	<!--Route路由设置-->
  <ROUTE>
    /,home/index,index
    /{controller}/{action},home/index,index
    /{controller},home/index,index
  </ROUTE>

#拦截器实现全局权限控制、注入、内容过滤等
	com.interceptor.AccessControl
  com.interceptor.ContentControl

#示例

##拦截器示例

	public class AccessControl {
    @AccessAttr(urlContain = "/")
    public String defaultControl(NioRequest request) {
      String url = request.getUrl().toLowerCase();
      if (url.contains("/admin"))
        return "您没有访问权限！IP：" + request.getRemoteIp();

      request.setAttribute("powerList", "add,edit,del");
      return "";
    }
  }
  
  @AccessAttr(urlContain = "/")
  public String defaultControl(NioRequest request, String html) {
    return html.replaceAll("flynn", "author");
  }

##controller示例（home控制器index页面 index & login 请求）

  public class Index extends Controller {
    @HttpMethod.HttpGet
    public String index() {
        return this.getView();
    }

    @HttpMethod.HttpPost
    public String login() {
      String password = this.getRequest().getRequestStr("password");
      String username = this.getRequest().getRequestStr("username", false);
      return checkLogin(username, password);
    }
  }

###Post请求

	@HttpMethod.HttpPost

###Get请求

	@HttpMethod.HttpGet //或不加注解则默认为Get请求
