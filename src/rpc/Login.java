package rpc;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;

/**
 * Servlet implementation class Login
 */
@WebServlet("/login")
public class Login extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		DBConnection conn = DBConnectionFactory.getConnection();
		try {
			// obj is used to return  
			JSONObject obj = new JSONObject();
			// false: 假设登陆以后，获得用户信息，不需要再创建新的session
			HttpSession session = request.getSession(false);
			if(session == null) {
				// 403: unauthorized 
				response.setStatus(403);
				obj.put("status", "Session Invalid");
			} else {
				// if session do exits, we get the infos
				String userId = (String) session.getAttribute("user_id");
				// return full name info to the front end 
				// for the top right welcome msg
				String name = conn.getFullname(userId);
				obj.put("status", "OK");
				obj.put("user_id", userId);
				obj.put("name", name);
			}
			
			RpcHelper.writeJsonObject(response, obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}finally {
			conn.close();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// 获得与数据库的关系
		DBConnection conn = DBConnectionFactory.getConnection();
		try {
			JSONObject input = RpcHelper.readJSONObject(request);
			String userId = input.getString("user_id");
			String pwd = input.getString("password");
			
			JSONObject obj = new JSONObject();
			// 判断登陆是否成功
			if (conn.verifyLogin(userId, pwd)) {
				HttpSession session = request.getSession();
				session.setAttribute("user_id", userId);
				// 设置session的有效期
				// session信息是明文的，是保存在server端的
				// 本地是在cookie中存一个session id，我们发送给server
				// unit is second
				session.setMaxInactiveInterval(10 * 60);
				// Get user name
				String name = conn.getFullname(userId);
				obj.put("status", "OK");
				obj.put("user_id", userId);
				obj.put("name", name);
			} else {
				response.setStatus(401);
			}
			RpcHelper.writeJsonObject(response, obj);
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			conn.close();
		}
	}
}
