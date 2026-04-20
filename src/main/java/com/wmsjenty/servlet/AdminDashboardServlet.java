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
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            String filterUserId = request.getParameter("userId");
            String opType = request.getParameter("operationType");
            String sql = "SELECT * FROM operations_log\n" +
                    "WHERE operation_date BETWEEN ? AND ?\n" +
                    "  AND ( ? = '' OR user_id = ? )\n" +
                    "  AND ( ? = '' OR operation_type = ? )\n" +
                    "ORDER BY operation_date DESC";

            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDateTime startDateTime;
            java.time.LocalDateTime endDateTime;

            try {
                if ((startDateStr == null || startDateStr.isEmpty()) && (endDateStr == null || endDateStr.isEmpty())) {
                    startDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
                    endDateTime = now.atTime(23, 59, 59);
                } else if (endDateStr == null || endDateStr.isEmpty()) {
                    startDateTime = java.time.LocalDate.parse(startDateStr).atStartOfDay();
                    endDateTime = now.atTime(23, 59, 59);

                    if (startDateTime.toLocalDate().isAfter(now)) {
                        request.getSession().setAttribute("successMessage", false);
                        response.sendRedirect("/admin/dashboard");
                        return;
                    }
                } else if (startDateStr == null || startDateStr.isEmpty()) {
                    startDateTime = java.time.LocalDate.parse(endDateStr).atStartOfDay();
                    endDateTime = java.time.LocalDate.parse(endDateStr).atTime(23, 59, 59);
                } else {
                    startDateTime = java.time.LocalDate.parse(startDateStr).atStartOfDay();
                    endDateTime = java.time.LocalDate.parse(endDateStr).atTime(23, 59, 59);
                }

                if (startDateTime.isAfter(endDateTime)) {
                    request.getSession().setAttribute("successMessage", false);
                    response.sendRedirect("/admin/dashboard");
                    return;
                }

                try (Connection conn = DBConnector.getConnection()) {
                    PreparedStatement pstmt = conn.prepareStatement(sql);

                    pstmt.setObject(1, startDateTime);
                    pstmt.setObject(2, endDateTime);
                    pstmt.setString(3, filterUserId);
                    pstmt.setInt(4, "".equals(filterUserId) ? 0 : Integer.parseInt(filterUserId));
                    pstmt.setString(5, opType);
                    pstmt.setString(6, opType);

                    ResultSet rs = pstmt.executeQuery();
                    ArrayList<Operation> logs = new ArrayList<>();

                    while (rs.next()) {
                        Operation op = new Operation();
                        op.setOperationDate(rs.getTimestamp("operation_date").toString());
                        op.setUserId(rs.getInt("user_id"));
                        op.setOperationType(rs.getString("operation_type"));
                        op.setComment(rs.getString("comment"));

                        if (rs.getString("operation_type").equals("приход")) {
                            String getNoteNumber = "SELECT * FROM incoming_invoices WHERE id = ?";
                            PreparedStatement psNoteNumber = conn.prepareStatement(getNoteNumber);
                            psNoteNumber.setInt(1, rs.getInt("document_id"));
                            ResultSet noteNumber = psNoteNumber.executeQuery();
                            while (noteNumber.next()) {
                                op.setInvoiceNumber(noteNumber.getString("invoice_number"));
                            }
                        }

                        logs.add(op);
                    }
                    request.getSession().setAttribute("logs", logs);
                    request.getSession().removeAttribute("successMessage");
                }
            } catch (Exception e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
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
                request.getSession().removeAttribute("logs");

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
                request.getSession().removeAttribute("logs");

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

            // проверки корректности
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
                request.getSession().removeAttribute("logs");

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
        else if (action.equals("delete_user")) {
            String idString = request.getParameter("id");
            Integer id;
            String name = null;
            boolean operationsExist = false;
            if (idString != null && !idString.isEmpty()) {
                id = Integer.parseInt(idString);
                ArrayList<User> users = (ArrayList<User>) request.getSession().getAttribute("userList");
                for (User user : users) {
                    if (user.getId() == id) {
                        name = user.getName();
                        operationsExist = DBDataLoader.hasUserOperations(id);
                        break;
                    }
                }
            }
            else {
                request.getSession().setAttribute("successMessage", false);
                response.sendRedirect("/admin/dashboard");
                return;
            }
            if (!operationsExist) {
                sqlStatement = "DELETE FROM users WHERE id IN (?)";
            }
            else {
                sqlStatement = "UPDATE users SET isActive = false WHERE id = ?";
            }

            try (Connection conn = DBConnector.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                request.getSession().setAttribute("successMessage", true);
                request.getSession().removeAttribute("logs");

                //внесение логов
                sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer userId = (Integer) request.getSession().getAttribute("userId");
                String operationType = "удаление аккаунта";
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
        else if (action.equals("adjustment")) {
            String itemIdString = request.getParameter("itemId");
            String newValueString = request.getParameter("value");
            String comment = request.getParameter("comment");
            Integer itemId;
            Integer newValue;

            if (itemIdString != null && !itemIdString.isEmpty() &&
            newValueString != null && !newValueString.isEmpty()) {
                itemId = Integer.parseInt(itemIdString);
                newValue = Integer.parseInt(newValueString);
                if (newValue < 0) {
                    request.getSession().setAttribute("successMessage", false);
                    response.sendRedirect("/admin/dashboard");
                    return;
                }
            }
            else {
                request.getSession().setAttribute("successMessage", false);
                response.sendRedirect("/admin/dashboard");
                return;
            }

            String ifItemExistSql = "SELECT * FROM item WHERE id = ?";
            String setNewItemValue = "UPDATE item SET value = ? WHERE id = ?";

            try (Connection conn = DBConnector.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(ifItemExistSql);
                pstmt.setInt(1, itemId);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    request.getSession().setAttribute("successMessage", false);
                    response.sendRedirect("/admin/dashboard");
                    return;
                }
                String itemName = rs.getString("name");
                String oldValue = String.valueOf(rs.getInt("value"));

                pstmt = conn.prepareStatement(setNewItemValue);
                pstmt.setInt(1, newValue);
                pstmt.setInt(2, itemId);
                pstmt.executeUpdate();

                //внесение логов
                sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer userId = (Integer) request.getSession().getAttribute("userId");
                String operationType = "корректировка остатков";
                pstmt = conn.prepareStatement(sqlStatement);
                comment = comment + "\n" + "Наименование: " + itemName + "\n" + "Старое значение: " +
                        oldValue + "\n" + "Новое значение: " + newValueString;
                pstmt.setInt (1, userId);
                pstmt.setString(2, operationType);
                pstmt.setString(3, comment);
                pstmt.executeUpdate();
                request.getSession().setAttribute("successMessage", true);
                request.getSession().removeAttribute("logs");
            }
            catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("successMessage", false);
        }
            response.sendRedirect("/admin/dashboard");
        }
    }
}
