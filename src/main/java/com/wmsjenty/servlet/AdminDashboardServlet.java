package com.wmsjenty.servlet;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.User;
import com.wmsjenty.service.DBConnector;
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
        List<User> users = loadAllUsers();
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
        String sqlStatement;

        if (action.equals("add_category")) {
            String name = request.getParameter("categoryName");
            String pIdString = request.getParameter("parentId");

            //Проверка на ячейки с пробелами
            if (name == null || name.trim().isEmpty()) {
                request.getSession().setAttribute("successMessage", false);
                response.sendRedirect("/admin/dashboard");
                return;
            }
            Integer parentId = null;
            if (pIdString != null && !pIdString.isEmpty()) {
                parentId = Integer.parseInt(pIdString);
            }

            sqlStatement = "INSERT INTO category (name, parent_id) VALUES (?, ?)";

            try (Connection conn = DBConnector.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setString(1, name);
                if (parentId == null) pstmt.setNull(2, java.sql.Types.INTEGER);
                else {
                    pstmt.setInt(2, parentId);
                }
                pstmt.executeUpdate();
                request.getSession().setAttribute("successMessage", true);

                //внесение лога
                sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer id = (Integer) request.getSession().getAttribute("userId");
                String operationType = "добавление категории";
                pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setInt (1, id);
                pstmt.setString(2, operationType);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("delete_category")) {
            String idString = request.getParameter("id");
            Integer id;
            String name = null;
            if (idString != null && !idString.isEmpty()) {
                id = Integer.parseInt(idString);
                ArrayList<Category> categories = (ArrayList<Category>) request.getSession().getAttribute("categories");
                for (Category c : categories) {
                    if (c.getId() == id) {
                        name = c.getName();
                        break;
                    }
                }
            }
            else {
                request.getSession().setAttribute("successMessage", false);
                response.sendRedirect("/admin/dashboard");
                return;
            }
            sqlStatement = "DELETE FROM category WHERE id IN (?)";
            try (Connection conn = DBConnector.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                request.getSession().setAttribute("successMessage", true);

                //внесение логов
                sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer userId = (Integer) request.getSession().getAttribute("userId");
                String operationType = "удаление категории";
                pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setInt (1, userId);
                pstmt.setString(2, operationType);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
            response.sendRedirect("/admin/dashboard");
        }
        else if (action.equals("add_user")) {
            String name = request.getParameter("name");
            String login = request.getParameter("login");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirm_password");
            String role = request.getParameter("role");

            //Проверки корректности
            if (name == null || name.trim().isEmpty() ||
            login == null || login.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty() ||
            role == null || !password.equals(confirmPassword)) {
                request.getSession().setAttribute("successMessage", false);
                response.sendRedirect("/admin/dashboard");
                return;
            }
            String hashedPassword = PasswordHasher.encode(password);
            String sql = "INSERT INTO users (name, role, login, password) VALUES (?, ?, ?, ?)";

            try (Connection conn = DBConnector.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, role);
                pstmt.setString(3, login);
                pstmt.setString(4, hashedPassword);
                pstmt.executeUpdate();
                request.getSession().setAttribute("successMessage", true);

                //внесение логов
                sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer userId = (Integer) request.getSession().getAttribute("userId");
                String operationType = "добавление аккаунта";
                pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setInt (1, userId);
                pstmt.setString(2, operationType);
                pstmt.setString(3, login + ": " + name);
                pstmt.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
            response.sendRedirect("/admin/dashboard");
        }
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

    private List<User> loadAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                String login = rs.getString("login");

                users.add(new User(id, name, login, role));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
