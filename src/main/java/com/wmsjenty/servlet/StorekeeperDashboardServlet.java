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
//        if ("check_item".equals(action)) {
//            String itemArticle = request.getParameter("article");
//            String itemValue = request.getParameter("value");
//            Item outgoItem = DBDataLoader.checkItemAvailable(itemArticle, Integer.parseInt(itemValue));
//
//            if (outgoItem != null) {
//                outgoItems.put(outgoItem, Integer.parseInt(itemValue));
//                session.setAttribute("outgoItems", outgoItems);
//                session.setAttribute("check_item_success", true);
//            }
//            else {
//                session.setAttribute("check_item_success", false);
//            }
//        }
        if ("check_item".equals(action)) {
            String itemArticle = request.getParameter("article");
            String itemValue = request.getParameter("value");

            Map<Item, Integer> outgoItems = (Map<Item, Integer>) session.getAttribute("outgoItems");
            if (outgoItems == null) {
                outgoItems = new HashMap<>();
            }

            Item outgoItem = DBDataLoader.checkItemAvailable(itemArticle, Integer.parseInt(itemValue));

            if (outgoItem != null) {
                outgoItems.put(outgoItem, Integer.parseInt(itemValue));
                session.setAttribute("outgoItems", outgoItems);
                session.setAttribute("check_item_success", true);
            } else {
                session.setAttribute("check_item_success", false);
            }

            response.sendRedirect(request.getContextPath() + "/storekeeper/dashboard");
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

    public void clearOutgoItemsList() {
        outgoItems.clear();
    }
}
