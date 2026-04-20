package com.wmsjenty.servlet;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.User;
import com.wmsjenty.util.DBDataLoader;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "AdminDashboardServlet", value = "/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"администратор".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if ("getItemInfo".equals(action)) {
            DBDataLoader.getItemInfo(request, response);
            request.getSession().removeAttribute("logs");
            return;
        }
        if ("search_items".equals(action)) {
            DBDataLoader.searchItems(request, response);
            request.getSession().removeAttribute("logs");
            return;
        }
        if ("get_logs".equals(action)) {
            DBDataLoader.getLogs(request, response);
            response.sendRedirect("/admin/dashboard");
            return;
        }

        List<Category> categories = DBDataLoader.loadAllCategories();
        List<User> users = DBDataLoader.loadAllUsers();
        session.setAttribute("categories", categories);
        session.setAttribute("userName", user.getName());
        session.setAttribute("userList", users);
        session.setAttribute ("userId", user.getId());

        request.getRequestDispatcher("/admin-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if (action.equals("add_category")) {
            DBDataLoader.addCategory(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("delete_category")) {
            DBDataLoader.deleteCategory(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("add_user")) {
            DBDataLoader.addUser(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("delete_user")) {
            DBDataLoader.deleteUser(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("adjustment")) {
            DBDataLoader.adjustItemValue(request, response);
            response.sendRedirect("/admin/dashboard");
        }
    }
}
