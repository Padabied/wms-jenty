package com.wmsjenty.service;

import com.wmsjenty.model.Category;
import com.wmsjenty.model.Item;
import com.wmsjenty.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
