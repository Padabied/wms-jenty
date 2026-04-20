package com.wmsjenty.servlet;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.Operation;
import com.wmsjenty.model.User;
import com.wmsjenty.util.DBConnector;
import com.wmsjenty.util.DBDataLoader;
import com.wmsjenty.util.PasswordHasher;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
            DBDataLoader.handleGetItemInfo(request, response);
            request.getSession().removeAttribute("logs");
            return;
        }
        if ("search_items".equals(action)) {
            DBDataLoader.handleSearchItems(request, response);
            request.getSession().removeAttribute("logs");
            return;
        }
        if ("get_logs".equals(action)) {
            DBDataLoader.handleGetLogs(request, response);
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
            DBDataLoader.handleAddCategory(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("delete_category")) {
            DBDataLoader.handleDeleteCategory (request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("add_user")) {
            DBDataLoader.handleAddUser(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("delete_user")) {
            DBDataLoader.handleDeleteUser(request, response);
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("adjustment")) {
            DBDataLoader.handleAdjustItemValue (request, response);
            response.sendRedirect("/admin/dashboard");
        }
    }
}
