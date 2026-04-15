package com.wmsjenty.servlet;

import com.wmsjenty.model.*;
import com.wmsjenty.service.DBConnector;
import com.wmsjenty.service.DBDataLoader;
import com.wmsjenty.service.PasswordHasher;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "StorekeeperDashboardServlet", value = "/storekeeper/dashboard")
public class StorekeeperDashboardServlet extends HttpServlet {

    private HashMap<Item, Integer> outgoItems = new HashMap<>();
//    private HashMap<Item, Integer> incomeItems = new HashMap<>();
//    private ArrayList<Item> newItems = new ArrayList<>();
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
        if ("get_logs".equals(action)) {
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            String opType = request.getParameter("operationType");
            String userId = String.valueOf(session.getAttribute("userId"));
            String sql = "SELECT * FROM operations_log\n" +
                    "WHERE operation_date BETWEEN ? AND ?\n" +
                    "  AND user_id = ?\n" +
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
                        response.sendRedirect("/storekeeper/dashboard");
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
                    response.sendRedirect("/storekeeper/dashboard");
                    return;
                }

                try (Connection conn = DBConnector.getConnection()) {
                    PreparedStatement pstmt = conn.prepareStatement(sql);

                    pstmt.setObject(1, startDateTime);
                    pstmt.setObject(2, endDateTime);
                    pstmt.setString(3, userId);
                    pstmt.setString(4, opType);
                    pstmt.setString(5, opType);

                    ResultSet rs = pstmt.executeQuery();
                    ArrayList<Operation> logs = new ArrayList<>();

                    while (rs.next()) {
                        Operation op = new Operation();
                        op.setOperationDate(rs.getTimestamp("operation_date").toString());
                        op.setOperationType(rs.getString("operation_type"));
                        op.setDocumentId(rs.getInt("document_id"));
                        op.setComment(rs.getString("comment"));
                        logs.add(op);
                    }
                    request.getSession().setAttribute("logs", logs);
                    request.getSession().removeAttribute("successMessage");
                }
            } catch (Exception e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
            response.sendRedirect("/storekeeper/dashboard");
            return;
        }
        if ("check_item".equals(action)) {
            String itemArticle = request.getParameter("article");
            String itemValue = request.getParameter("value");

            int quantity = (itemValue != null && !itemValue.isEmpty()) ? Integer.parseInt(itemValue) : 0;

            if (quantity <= 0) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":\"error\", \"message\":\"Недопустимое количество\"}");
                return;
            }

            Item outgoItem = DBDataLoader.checkItemAvailable(itemArticle, quantity);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if (outgoItem != null) {
                outgoItems.put(outgoItem, quantity);
                session.setAttribute("outgoItems", outgoItems);

                String json = "{\"status\":\"success\", \"article\":\"" + outgoItem.getArticle() +
                        "\", \"name\":\"" + outgoItem.getName() +
                        "\", \"value\":" + quantity + "}";
                response.getWriter().write(json);
            }
            else {
                response.getWriter().write("{\"status\":\"error\", \"message\":\"Товар не найден или недостаточно на складе\"}");
            }
            return;
        }
        if ("clear_outgo".equals(action)) {
            clearOutgoItemsList();
            session.removeAttribute("outgoItems");
            session.removeAttribute("check_item_success");

            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setStatus(200);
                return;
            }
        }
        if ("outgo_log".equals(action)) {
            String regNumber = request.getParameter("regNumber");
            HashMap<String, ArrayList<OutgoItem>> outgoDateAndItems = DBDataLoader.getOutgoInvoicesByRegNumber(regNumber);
//            if (!outgoDateAndItems.isEmpty()) {
//                session.setAttribute("outgoLogs", outgoDateAndItems);
//            }
//            else {
//                request.getSession().setAttribute("successMessage", false);
//                response.sendRedirect("/storekeeper/dashboard");
//                return;
//            }
            session.setAttribute("outgoLogs", outgoDateAndItems);
            response.sendRedirect("/storekeeper/dashboard");
            return;
        }
        if ("check_income_item".equals(action)) {
            HashMap<Item, Integer> incomeItems = (HashMap<Item, Integer>) request.getSession().getAttribute("incomeItems");
            String article = request.getParameter("article");
            int value = Integer.parseInt(request.getParameter("value"));

            Item item = DBDataLoader.checkItemExists(article);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if (item != null) {
                incomeItems.put(item, value);
                String json = String.format(
                        "{\"status\":\"success\", \"article\":\"%s\", \"name\":\"%s\"}",
                        item.getArticle(), item.getName()
                );
                response.getWriter().write(json);
            } else {
                // открытие модального окна
                response.getWriter().write("{\"status\":\"not_found\"}");
            }
            return;
        }

        List<Category> categories = DBDataLoader.loadAllCategories();
        List<User> users = DBDataLoader.loadAllUsers();
        session.setAttribute("categories", categories);
        session.setAttribute("userName", user.getName());
        session.setAttribute("userList", users);
        session.setAttribute ("userId", user.getId());
        if (session.getAttribute("incomeItems") == null) {
            session.setAttribute("incomeItems", new HashMap<Item, Integer>());
        }
        if (session.getAttribute("newItems") == null) {
            session.setAttribute("newItems", new ArrayList<Item>());
        }

        request.getRequestDispatcher("/storekeeper-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        if ("confirm_outgo".equals(action)) {
            String receiver = request.getParameter("receiverName");
            String regNum = request.getParameter("regNumber");

            User user = (User) session.getAttribute("user");

            if (user != null && outgoItems != null && !outgoItems.isEmpty()) {
                int documentId = DBDataLoader.saveFullInvoice(receiver, regNum, user.getId(), outgoItems);

                if (documentId > 0) {
                    clearOutgoItemsList();
                    session.setAttribute("outgo_status", "success");
                    session.setAttribute("successMessage", true);
                    //внесение лога
                    try (Connection conn = DBConnector.getConnection()) {
                        String sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, document_id) VALUES (NOW(), ?, ?, ?)";
                        PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
                        int id = user.getId();
                        String operationType = "расход";
                        pstmt.setInt(1, id);
                        pstmt.setString(2, operationType);
                        pstmt.setInt(3, documentId);
                        pstmt.executeUpdate();
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    session.setAttribute("outgo_status", "error");
                }
            }
            response.sendRedirect(request.getContextPath() + "/storekeeper/dashboard");
        }
        if ("add_new_item_temp".equals(action)) {
            ArrayList<Item> newItems = (ArrayList<Item>) request.getSession().getAttribute("newItems");
            String article = request.getParameter("article");
            String name = request.getParameter("name");
            String brand = request.getParameter("brand");
            int categoryId = Integer.parseInt(request.getParameter("category"));
            int value = Integer.parseInt(request.getParameter("value"));
            int minVal = Integer.parseInt(request.getParameter("minVal"));
            int recVal = Integer.parseInt(request.getParameter("recVal"));

            Item newItem = new Item();
            newItem.setArticle(article);
            newItem.setName(name);
            newItem.setBrand(brand);
            newItem.setCategoryId(categoryId);
            newItem.setValue(value);
            newItem.setMinValue(minVal);
            newItem.setRecommendedValue(recVal);

            newItems.add(newItem);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":\"success\"}");
            return;
        }
        if ("confirm_income".equals(action)) {
            String noteNumber = request.getParameter("noteNumber");
            String supplierName = request.getParameter("supplierName");
            int userId = (int) session.getAttribute("userId");

            HashMap<Item, Integer> incomeItems = (HashMap<Item, Integer>) session.getAttribute("incomeItems");
            ArrayList<Item> newItems = (ArrayList<Item>) session.getAttribute("newItems");

            int documentId = DBDataLoader.processIncome(noteNumber, supplierName, incomeItems, newItems, userId);

            if (documentId != -1) {
                //внесение лога
                try (Connection conn = DBConnector.getConnection()) {
                    String sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, document_id) VALUES (NOW(), ?, ?, ?)";
                    PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
                    String operationType = "приход";
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, operationType);
                    pstmt.setInt(3, documentId);
                    pstmt.executeUpdate();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
                session.setAttribute("incomeItems", new HashMap<Item, Integer>());
                session.setAttribute("newItems", new ArrayList<Item>());

                response.sendRedirect("/storekeeper/dashboard?success=income");
            }
            else {
                request.setAttribute("error", "Ошибка при сохранении накладной");
                request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
            }
        }
    }

    public void clearOutgoItemsList() {
        outgoItems.clear();
    }
}
