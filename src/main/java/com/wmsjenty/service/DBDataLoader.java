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

    public static void handleSearchItems (HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        String categoryId = request.getParameter("searchCategory");
        String namePart = request.getParameter("searchName");
        String article = request.getParameter("searchArticle");

        ArrayList<Item> foundItems = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM item WHERE 1=1");
        if (categoryId != null && !categoryId.isEmpty()) sql.append(" AND category_id = ?");
        if (namePart != null && !namePart.isEmpty()) sql.append(" AND LOWER(name) LIKE LOWER(?)");
        if (article != null && !article.isEmpty()) sql.append(" AND article = ?");

        try (Connection conn = DBConnector.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            int paramIdx = 1;

            if (categoryId != null && !categoryId.isEmpty()) pstmt.setInt(paramIdx++, Integer.parseInt(categoryId));
            if (namePart != null && !namePart.isEmpty()) pstmt.setString(paramIdx++, "%" + namePart + "%");
            if (article != null && !article.isEmpty()) pstmt.setString(paramIdx++, article);

            ResultSet rs = pstmt.executeQuery();
            ArrayList<Category> categories = (ArrayList<Category>) session.getAttribute("categories");
            while (rs.next()) {
                Item it = new Item();
                it.setId(rs.getInt("id"));
                it.setArticle(rs.getString("article"));
                it.setBrand(rs.getString("brand"));
                it.setName(rs.getString("name"));
                it.setValue(rs.getInt("value"));

                for (Category category : categories) {
                    if (category.getId() == rs.getInt("category_id")) {
                        it.setCategoryName(category.getName());
                    }
                }

                foundItems.add(it);
            }
            session.setAttribute("foundItems", foundItems);
            if (user.getRole().equals("администратор")) {
                request.getRequestDispatcher("/admin-dashboard.jsp").forward(request, response);
            }
            else if (user.getRole().equals("кладовщик")) {
                request.getRequestDispatcher("/storekeeper-dashboard.jsp").forward(request, response);
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
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
}
