package com.wmsjenty.servlet;

import com.wmsjenty.model.*;
import com.wmsjenty.util.DBConnector;
import com.wmsjenty.util.DBDataLoader;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@WebServlet(name = "StorekeeperDashboardServlet", value = "/storekeeper/dashboard")
public class StorekeeperDashboardServlet extends HttpServlet {

    private HashMap<Item, Integer> outgoItems = new HashMap<>();
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
            DBDataLoader.handleGetLogsStorekeeper(request, response);
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
            TreeMap<String, ArrayList<OutgoItem>> sortedLogs = new TreeMap<>(Collections.reverseOrder());
            if (outgoDateAndItems != null) {
                sortedLogs.putAll(outgoDateAndItems);
            }
            session.setAttribute("outgoLogs", sortedLogs);
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
            DBDataLoader.handleAddNotExistingItemToListForIncome(request, response);
            return;
        }
        if ("confirm_income".equals(action)) {
            DBDataLoader.handleConfirmIncome(request, response);
        }
    }

    public void clearOutgoItemsList() {
        outgoItems.clear();
    }
}
