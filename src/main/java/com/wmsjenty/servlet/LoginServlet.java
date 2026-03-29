package com.wmsjenty.servlet;

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

@WebServlet(name = "LoginServlet", value = "/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Выход из системы
        String action = request.getParameter("action");

        if (action != null && action.equals("logout")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        //Вход в систему
        String login = request.getParameter("login");
        String password = request.getParameter("password");

        if (login == null || login.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Логин или пароль введены неверно");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try (Connection conn = DBConnector.getConnection();) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE login = ?");
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                String passwordHash = rs.getString("password");

                if (PasswordHasher.encode(password).equals(passwordHash)) {
                    HttpSession session = request.getSession();
                    User user = new User (id, name, login, role);
                    session.setAttribute("user", user);
                    switch (role){
                        case "администратор" : response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                            break;
                        case "кладовщик" : response.sendRedirect(request.getContextPath() + "/storekeeper/dashboard");
                            break;
                    }
                }
                else {
                    request.setAttribute("error", "Логин или пароль введены неверно");
                    request.getRequestDispatcher("/login.jsp").forward(request, response);
                }
            } else {
                request.setAttribute("error", "Логин или пароль введены неверно");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
