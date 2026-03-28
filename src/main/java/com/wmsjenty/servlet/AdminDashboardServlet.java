package com.wmsjenty.servlet;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.User;
import com.wmsjenty.service.DBConnector;

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
        List<Category> categories = loadAllCategories();
        session.setAttribute("categories", categories);
        session.setAttribute("userName", user.getName());
        request.getRequestDispatcher("/admin-dashboard.jsp").forward(request, response);
    }

    private List<Category> loadAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, parent_id FROM category ORDER BY id";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int parentIdInt = rs.getInt("parent_id");
                Integer parentId = rs.wasNull() ? null : parentIdInt;

                categories.add(new Category(id, name, parentId));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categories;
    }
}
