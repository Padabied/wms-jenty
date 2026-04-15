package com.wmsjenty.service;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.Item;
import com.wmsjenty.model.OutgoItem;
import com.wmsjenty.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBDataLoader {

    public static List<Category> loadAllCategories() {
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

    public static List<User> loadAllUsers() {
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
                boolean isActive = rs.getBoolean("isActive");

                users.add(new User(id, name, login, role, isActive));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static boolean hasUserOperations(int id) {
        String sqlStatement = "SELECT COUNT(*) FROM operations_log WHERE user_id = ?";
        try (Connection conn = DBConnector.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void handleGetItemInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String itemId = request.getParameter("itemId");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection conn = DBConnector.getConnection()) {
            String sql = "SELECT name, value FROM item WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(itemId));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String itemName = rs.getString("name").replace("\"", "\\\"");
                int itemValue = rs.getInt("value");

                response.getWriter().write("{\"name\":\"" + itemName + "\", \"value\":" + itemValue + "}");
            } else {
                response.getWriter().write("{\"error\":\"not_found\"}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"error\":\"server_error\"}");
        }
    }

public static void handleSearchItems(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession();
    User user = (User) session.getAttribute("user");

    String categoryId = request.getParameter("searchCategory");
    String namePart = request.getParameter("searchName");
    String article = request.getParameter("searchArticle");
    ArrayList<Category> allCategories = (ArrayList<Category>) session.getAttribute("categories");

    // сбор всех дочерних категорий + родительская
    List<Integer> targetCategoryId = new ArrayList<>();
    if (categoryId != null && !categoryId.isEmpty()) {
        int selectedId = Integer.parseInt(categoryId);
        targetCategoryId.add(selectedId);

        if (allCategories != null) {
            for (Category c : allCategories) {
                if (c.getParentId() != null && c.getParentId() == selectedId) {
                    targetCategoryId.add(c.getId());
                }
            }
        }
    }

    ArrayList<Item> foundItems = new ArrayList<>();
    StringBuilder sql = new StringBuilder("SELECT * FROM item WHERE 1=1");

    if (!targetCategoryId.isEmpty()) {
        sql.append(" AND category_id IN (");
        for (int i = 0; i < targetCategoryId.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(")");
    }

    if (namePart != null && !namePart.isEmpty()) sql.append(" AND LOWER(name) LIKE LOWER(?)");
    if (article != null && !article.isEmpty()) sql.append(" AND article = ?");

    try (Connection conn = DBConnector.getConnection()) {
        PreparedStatement pstmt = conn.prepareStatement(sql.toString());
        int paramIdx = 1;

        if (!targetCategoryId.isEmpty()) {
            for (Integer id : targetCategoryId) {
                pstmt.setInt(paramIdx++, id);
            }
        }

        if (namePart != null && !namePart.isEmpty()) pstmt.setString(paramIdx++, "%" + namePart + "%");
        if (article != null && !article.isEmpty()) pstmt.setString(paramIdx++, article);

        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            Item it = new Item();
            it.setId(rs.getInt("id"));
            it.setArticle(rs.getString("article"));
            it.setBrand(rs.getString("brand"));
            it.setName(rs.getString("name"));
            it.setValue(rs.getInt("value"));

            if (allCategories != null) {
                for (Category category : allCategories) {
                    if (category.getId() == rs.getInt("category_id")) {
                        it.setCategoryName(category.getName());
                        break;
                    }
                }
            }
            foundItems.add(it);
        }

        session.setAttribute("foundItems", foundItems);

        String path = user.getRole().equals("администратор") ? "/admin-dashboard.jsp" : "/storekeeper-dashboard.jsp";
        request.getRequestDispatcher(path).forward(request, response);

    } catch (Exception e) {
        e.printStackTrace();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}

    public static String getUserId(Integer userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnector.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                return rs.getString("name");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<Item> getForecast() {
        ArrayList<Item> result = new ArrayList<>();
        String sql = "SELECT * FROM item WHERE value <= min_value";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = new Item();
                item.setId(rs.getInt("id"));
                item.setName(rs.getString("name"));
                item.setArticle(rs.getString("article"));
                item.setBrand(rs.getString("brand"));
                item.setValue(rs.getInt("value"));
                item.setRecommendedValue(rs.getInt("recommended_value"));

                result.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<Item> getInventoryList() {
        ArrayList<Item> result = new ArrayList<>();
        String sql = "SELECT * FROM item WHERE value > 0";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = new Item();
                item.setId(rs.getInt("id"));
                item.setName(rs.getString("name"));
                item.setArticle(rs.getString("article"));
                item.setBrand(rs.getString("brand"));
                item.setValue(rs.getInt("value"));

                result.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Item checkItemAvailable(String article, Integer value) {
        String sql = "SELECT * FROM item WHERE article = ? AND value >= ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, article);
            pstmt.setInt(2, value);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Item item = new Item();
                    item.setId(rs.getInt("id"));
                    item.setArticle(rs.getString("article"));
                    item.setName(rs.getString("name"));
                    item.setValue(rs.getInt("value"));
                    return item;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Item checkItemExists(String article) {
        try (Connection conn = DBConnector.getConnection()) {
            String sql = "SELECT * FROM item WHERE article = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, article);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Item item = new Item();
                item.setId(rs.getInt("id"));
                item.setArticle(rs.getString("article"));
                item.setName(rs.getString("name"));
                item.setBrand(rs.getString("brand"));
                return item;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // создание записей в outgoing_invoices, outgoing_items, уменьшение количества товаров в item
    // возвращает document_id в случае успеха и -1 в случае ошибки
    public static int saveFullInvoice(String receiver, String regNum, int userId, HashMap<Item, Integer> items) {
        Connection conn = null;
        int generatedId = -1;

        try {
            conn =DBConnector.getConnection();
            conn.setAutoCommit(false);

            // создание записи в outgoing_invoices
            String sqlInvoice = "INSERT INTO outgoing_invoices (receiver_name, truck_reg_number, outgo_date, outgo_creator_id) VALUES (?, ?, NOW(), ?)";
            PreparedStatement psInvoice = conn.prepareStatement(sqlInvoice, Statement.RETURN_GENERATED_KEYS);
            psInvoice.setString(1, receiver);
            psInvoice.setString(2, regNum);
            psInvoice.setInt(3, userId);
            psInvoice.executeUpdate();

            // получение сгенерированного ID накладной
            int invoiceId = -1;
            ResultSet rs = psInvoice.getGeneratedKeys();
            if (rs.next()) {
                invoiceId = rs.getInt(1);
                generatedId = rs.getInt(1);
            }

            // добавление товаров в outgoing_items и уменьшение значения на складе
            String sqlItems = "INSERT INTO outgoing_items (outgo_invoice_id, outgo_item_id, value) VALUES (?, ?, ?)";
            String sqlUpdateStock = "UPDATE item SET value = value - ? WHERE id = ?";

            PreparedStatement psItems = conn.prepareStatement(sqlItems);
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateStock);

            for (Map.Entry<Item, Integer> entry : items.entrySet()) {
                Item item = entry.getKey();
                int qty = entry.getValue();

                // запись в outgoing_items
                psItems.setInt(1, invoiceId);
                psItems.setInt(2, item.getId());
                psItems.setInt(3, qty);
                psItems.addBatch();

                // корректирование остатков в таблице item
                psUpdate.setInt(1, qty);
                psUpdate.setInt(2, item.getId());
                psUpdate.addBatch();
            }

            psItems.executeBatch();
            psUpdate.executeBatch();

            conn.commit();
            return generatedId;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); }
                catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return -1;
        }
    }

    public static HashMap<String, ArrayList<OutgoItem>> getOutgoInvoicesByRegNumber(String regNumber) {
        HashMap<String, ArrayList<OutgoItem>> result = new HashMap<>();
        String getOutgoInvoices = "SELECT * FROM outgoing_invoices WHERE truck_reg_number = ?";
        String getOutgoItems = "SELECT * FROM outgoing_items WHERE outgo_invoice_id = ?";
        String getItemName = "SELECT * FROM item WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getOutgoInvoices)) {
            pstmt.setString(1, regNumber);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String outgoDate = rs.getString("outgo_date");
                ArrayList<OutgoItem> items = new ArrayList<>();
                PreparedStatement stmtToGetItems = conn.prepareStatement(getOutgoItems);
                stmtToGetItems.setInt(1, rs.getInt("id"));
                ResultSet outgoingItemsSet = stmtToGetItems.executeQuery();
                while (outgoingItemsSet.next()) {
                    OutgoItem itemToList = new OutgoItem();
                    itemToList.setId(outgoingItemsSet.getInt("outgo_item_id"));
                    itemToList.setValueForOutgo(outgoingItemsSet.getInt("value"));
                    PreparedStatement stmtToGetItemName = conn.prepareStatement(getItemName);
                    stmtToGetItemName.setInt(1, outgoingItemsSet.getInt("outgo_item_id"));
                    ResultSet itemNames = stmtToGetItemName.executeQuery();
                    while (itemNames.next()) {
                        itemToList.setName(itemNames.getString("name"));
                    }
                    items.add(itemToList);
                }
                result.put(outgoDate, items);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
        }

        public static int processIncome (String noteNumber, String supplierName, HashMap<Item, Integer> incomeItems ,
                                             ArrayList<Item> newItems, int userId) {
            Connection conn = null;
            try {
                conn = DBConnector.getConnection();
                conn.setAutoCommit(false);

                // создаем запись в таблице накладных (income_invoices)
                String createIncomeInvoice = "INSERT INTO incoming_invoices (invoice_number, supplier_name, income_date, income_creator_id) VALUES (?, ?, NOW(), ?)";
                PreparedStatement psCreateIncomeInvoice = conn.prepareStatement(createIncomeInvoice, Statement.RETURN_GENERATED_KEYS);
                psCreateIncomeInvoice.setString(1, noteNumber);
                psCreateIncomeInvoice.setString(2, supplierName);
                psCreateIncomeInvoice.setInt(3, userId);
                psCreateIncomeInvoice.executeUpdate();

                //получаем id накладной
                int invoiceId = -1;
                ResultSet rs = psCreateIncomeInvoice.getGeneratedKeys();
                if (rs.next()) {
                    invoiceId = rs.getInt(1);
                }

                String sqlNewItem = "INSERT INTO item (article, name, brand, category_id, min_value, recommended_value, value) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement psNewItem = conn.prepareStatement(sqlNewItem, Statement.RETURN_GENERATED_KEYS);

                for (Item item : newItems) {
                    // добавление товаров из списка ранее несуществующих
                    psNewItem.setString(1, item.getArticle());
                    psNewItem.setString(2, item.getName());
                    psNewItem.setString(3, item.getBrand());
                    psNewItem.setInt(4, item.getCategoryId());
                    psNewItem.setInt(5, item.getMinValue());
                    psNewItem.setInt(6, item.getRecommendedValue());
                    psNewItem.setInt(7, item.getValue());
                    psNewItem.executeUpdate();

                    //получение id нового item
                    int newItemId = -1;
                    ResultSet rsItem = psNewItem.getGeneratedKeys();
                    if (rsItem.next()) {
                        newItemId = rsItem.getInt(1);
                    }

                    //добавление записи в incoming_items
                    String createIncomingItem = "INSERT INTO incoming_items (income_invoice_id, income_item_id, value) VALUES (?, ?, ?)";
                    PreparedStatement psIncomingItem = conn.prepareStatement(createIncomingItem);
                    psIncomingItem.setInt(1, invoiceId);
                    psIncomingItem.setInt(2, newItemId);
                    psIncomingItem.setInt(3, item.getValue());
                    psIncomingItem.executeUpdate();
                }

                // обновление количества ранее существующих товаров
                String updateStock = "UPDATE item SET value = value + ? WHERE id = ?";
                PreparedStatement psUpdateStock = conn.prepareStatement(updateStock);

                for (Map.Entry<Item, Integer> entry : incomeItems.entrySet()) {
                    //добавление записи в incoming_items
                    String createIncomingItem = "INSERT INTO incoming_items (income_invoice_id, income_item_id, value) VALUES (?, ?, ?)";
                    PreparedStatement psIncomingItem = conn.prepareStatement(createIncomingItem);
                    psIncomingItem.setInt(1, invoiceId);
                    psIncomingItem.setInt(2, entry.getKey().getId());
                    psIncomingItem.setInt(3, entry.getValue());
                    psIncomingItem.executeUpdate();

                    // обновление количества товара на складе
                    psUpdateStock.setInt(1, entry.getValue());
                    psUpdateStock.setInt(2, entry.getKey().getId());
                    psUpdateStock.executeUpdate();
                }

                conn.commit();
                return invoiceId;

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); }
                    catch (SQLException ex) { ex.printStackTrace(); }
                }
                e.printStackTrace();
                return -1;
            }
        }
    }

