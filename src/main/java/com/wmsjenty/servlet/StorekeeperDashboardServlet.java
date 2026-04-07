package com.wmsjenty.servlet;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.Item;
import com.wmsjenty.model.Operation;
import com.wmsjenty.model.User;
import com.wmsjenty.service.DBConnector;
import com.wmsjenty.service.DBDataLoader;
import com.wmsjenty.service.PasswordHasher;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "StorekeeperDashboardServlet", value = "/storekeeper/dashboard")
public class StorekeeperDashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"кладовщик".equals(user.getRole())) {
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

        List<Category> categories = DBDataLoader.loadAllCategories();
        List<User> users = DBDataLoader.loadAllUsers();
        session.setAttribute("categories", categories);
        session.setAttribute("userName", user.getName());
        session.setAttribute("userList", users);
        session.setAttribute ("userId", user.getId());

        request.getRequestDispatcher("/storekeeper-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

}
