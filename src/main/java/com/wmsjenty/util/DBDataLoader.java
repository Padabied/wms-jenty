package com.wmsjenty.util;

import com.wmsjenty.model.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBDataLoader {
    /**
     * Функция для получения списка всех категорий.
     * @return список, содержащий все категории в таблице category.
     */
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

    /**
     * Функция для получения списка всех пользователей, содержащихся
     * в таблице users базы данных. Включает также деактивированные аккаунты.
     * @return список активных и неактивных аккаунтов системы.
     */
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

    /**
     * Определяет, вносил ли указанный пользователь какие либо изменения в БД,
     * например оформление прихода или расхода.
     * @param id id пользователя в системе.
     * @return boolean с результатом поиска.
     */
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

    /**
     * Функция для получения наименования и значения количества товара в БД.
     * Ответ возвращается  в JSON формате.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void getItemInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    /**
     * Функция используется при поиске товара на складе.
     * По заданным параметрам, например, категория, наименование, артикул, производится поиск в базе данных.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void searchItems(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    /**
     * Функция для получения имени пользователя с заданным ID.
     * @param userId ID пользователя в системе.
     * @return
     */
    public static String getUserNameById(Integer userId) {
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

    /**
     * Функция для получения списка товаров, значение количества которых
     * меньше либо равно минимальному (заданному параметром min_value).
     * @return список товаров.
     */
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

    /**
     * Функция для получения полного списка товаров, находящихся на складе,
     * значение количества которых больше нуля.
     * @return список найденных товаров.
     */
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

    /**
     * Функция для проверки наличия в базе данных достаточного количества товара с указанным артикулом.
     * Проверяется наличие товара заданного артикула, а также наличие указанного количества.
     * @param article артикул товара.
     * @param value запрашиваемое количество.
     * @return найденный товар, либо null, если товар не найден или запрашиваемое количество не соответствует
     * количеству товара в наличии.
     */
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

    /**
     * Проверка наличия товара с указанным артикулом в базе данных.
     * @param article артикул искомого товара.
     * @return найденный товар, либо null, если товара с заданным артикулом не существует в БД.
     */
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

    /**
     * Основная функция при добавлении новой расходной накладной.
     * Создание записей в таблицах outgoing_invoices, outgoing_items, уменьшение количества товаров в item.
     * @param receiver получатель.
     * @param regNum регистрационный номер автомобиля.
     * @param userId id пользователя, создавшего накладную.
     * @param items список товаров к списанию в формате "Товар-Количество".
     * @return id созданной накладной в случае успеха, либо -1 в случае ошибки.
     */
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

    /**
     * Функция для получения сведений о выдаче товаров по регистрационному номеру автомобиля.
     * @param regNumber регистрационный номер автомобиля.
     * @return таблица в формате "Дата-Список выданных товаров".
     */
    public static HashMap<String, ArrayList<OutgoItem>> getOutgoInvoicesByRegNumber(String regNumber) {
        HashMap<String, ArrayList<OutgoItem>> result = new HashMap<>();
        String getOutgoInvoices = "SELECT * FROM outgoing_invoices WHERE truck_reg_number = ?";
        String getOutgoItems = "SELECT * FROM outgoing_items WHERE outgo_invoice_id = ?";
        String getItemName = "SELECT * FROM item WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getOutgoInvoices)) {
            pstmt.setString(1, regNumber.trim());
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

    /**
     * Функция для оформления приходной накладной. Включает в себя добавление записей в incoming_invoices,
     * incoming_item, item, operations_log.
     * @param noteNumber номер товарно-транспортной накладной.
     * @param supplierName наименование поставщика.
     * @param incomeItems список товаров, уже присутствующих в базе данных.
     * @param newItems список товаров, ранее не присутствовавших в базе данных.
     * @param userId ID пользователя, создающего накладную.
     * @return номер документа, либо -1 в случае ошибки.
     */
        public static int processIncome (String noteNumber, String supplierName, HashMap<Item, Integer> incomeItems ,
                                             ArrayList<Item> newItems, int userId) {
            Connection conn = null;
            try {
                conn = DBConnector.getConnection();
                conn.setAutoCommit(false);

                // создание записи в incoming_invoices
                String createIncomeInvoice = "INSERT INTO incoming_invoices (invoice_number, supplier_name, income_date, income_creator_id) VALUES (?, ?, NOW(), ?)";
                PreparedStatement psCreateIncomeInvoice = conn.prepareStatement(createIncomeInvoice, Statement.RETURN_GENERATED_KEYS);
                psCreateIncomeInvoice.setString(1, noteNumber);
                psCreateIncomeInvoice.setString(2, supplierName);
                psCreateIncomeInvoice.setInt(3, userId);
                psCreateIncomeInvoice.executeUpdate();

                //получение id накладной
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

    /**
     * Функция производит поиск записей в таблице operations_log по заданным параметрам.
     * @param request запрос, содержащий входные данные.
     * @param response
     * @throws IOException
     */
        public static void getLogs(HttpServletRequest request, HttpServletResponse response) {
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
        }

    /**
     * Добавляет запись в таблице category, создавая новую категорию товаров.
     * @param request запрос, содержащий входные данные.
     * @param response
     * @throws IOException
     */
        public static void addCategory(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String name = request.getParameter("categoryName");
            String pIdString = request.getParameter("parentId");
            String sqlStatement = "INSERT INTO category (name, parent_id) VALUES (?, ?)";

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
                String logStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer id = (Integer) request.getSession().getAttribute("userId");
                String operationType = "добавление категории";
                pstmt = conn.prepareStatement(logStatement);
                pstmt.setInt (1, id);
                pstmt.setString(2, operationType);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
        }

    /**
     * Удаление записи из таблицы category.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void deleteCategory(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String idString = request.getParameter("id");
            Integer id;
            String name = null;
            String sqlStatement = "DELETE FROM category WHERE id IN (?)";
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

            try (Connection conn = DBConnector.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(sqlStatement);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                request.getSession().setAttribute("successMessage", true);
                request.getSession().removeAttribute("logs");

                //внесение логов
                String logStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
                Integer userId = (Integer) request.getSession().getAttribute("userId");
                String operationType = "удаление категории";
                pstmt = conn.prepareStatement(logStatement);
                pstmt.setInt (1, userId);
                pstmt.setString(2, operationType);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            }
            catch (SQLException e) {
                e.printStackTrace();
                request.getSession().setAttribute("successMessage", false);
            }
        }

    /**
     * Добавление нового пользователя в таблицу users.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void addUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

            String sqlStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
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
    }

    /**
     * Удаление записи из таблицы users. Если указанный пользователь совершал какие-либо операции над базой данных
     * и имеет записи в operations_log, аккаунт не будет удален, а будет деактиварован (флаг isActive = false).
     * @param request
     * @param response
     * @throws IOException
     */
    public static void deleteUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String idString = request.getParameter("id");
        Integer id;
        String name = null;
        String sqlStatement;
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
            String logStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String operationType = "удаление аккаунта";
            pstmt = conn.prepareStatement(logStatement);
            pstmt.setInt (1, userId);
            pstmt.setString(2, operationType);
            pstmt.setString(3, name);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
            request.getSession().setAttribute("successMessage", false);
        }
    }

    /**
     * Корректировка значения количества товара в таблице item.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void adjustItemValue(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
            String logStatement = "INSERT INTO operations_log (operation_date, user_id, operation_type, comment) VALUES (NOW(), ?, ?, ?)";
            Integer userId = (Integer) request.getSession().getAttribute("userId");
            String operationType = "корректировка остатков";
            pstmt = conn.prepareStatement(logStatement);
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
    }

    /**
     * Получение списка операций кладовщика по заданным датам.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void getLogsStorekeeper(HttpServletRequest request, HttpServletResponse response) {
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        String opType = request.getParameter("operationType");
        String userId = String.valueOf(request.getSession().getAttribute("userId"));
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
    }

    /**
     * Добавление товара, ранее не существовавшего в таблице item в список newItems.
     * @param request
     * @param response
     * @throws IOException
     */
    public static void addNotExistingItemToListForIncome(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
    }

    /**
     * Оформление прихода товара. Для товаров, ранее не существовавших в таблице item выполняется операция INSERT,
     * для товаров, уже существующих в БД, выполняется операция UPDATE с изменением актуального количества товара.
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    public static void confirmIncome(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String noteNumber = request.getParameter("noteNumber");
        String supplierName = request.getParameter("supplierName");
        int userId = (int) request.getSession().getAttribute("userId");

        HashMap<Item, Integer> incomeItems = (HashMap<Item, Integer>) request.getSession().getAttribute("incomeItems");
        ArrayList<Item> newItems = (ArrayList<Item>) request.getSession().getAttribute("newItems");

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
            request.getSession().setAttribute("incomeItems", new HashMap<Item, Integer>());
            request.getSession().setAttribute("newItems", new ArrayList<Item>());
            request.getSession().setAttribute("successMessage", true);
            response.sendRedirect("/storekeeper/dashboard?success=income");
        }
        else {
            request.setAttribute("error", "Ошибка при сохранении накладной");
            request.getRequestDispatcher("/storekeeper/dashboard.jsp").forward(request, response);
        }
    }
}

