<%@ page import="com.wmsjenty.model.Category" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.wmsjenty.model.User" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css">
    <meta charset="UTF-8">
    <title>WMS-Jenty Admin</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f5f5f5;
        }

        .navbar {
            background: linear-gradient(135deg, #59a950 0%, #28521a 100%);
            padding: 0 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            position: relative;
        }

        .nav-links {
            display: flex;
            gap: 5px;
            flex-wrap: wrap;
        }

        .nav-btn {
            background: transparent;
            color: white;
            border: none;
            padding: 14px 20px;
            font-size: 16px;
            cursor: pointer;
            transition: all 0.3s ease;
            border-radius: 6px;
            font-weight: 500;
            position: relative;
        }

        .nav-btn:hover {
            background: rgba(255,255,255,0.2);
            transform: translateY(-2px);
        }

        /* Контейнер для выпадающего меню */
        .dropdown {
            position: relative;
            display: inline-block;
        }

        .dropdown-content {
            display: none;
            position: absolute;
            top: 100%;
            left: 0;
            background-color: white;
            min-width: 160px;
            box-shadow: 0 8px 16px rgba(0,0,0,0.2);
            border-radius: 8px;
            z-index: 1000;
            animation: fadeIn 0.3s ease;
        }

        .dropdown-content button {
            color: #333;
            padding: 12px 16px;
            text-decoration: none;
            display: block;
            width: 100%;
            text-align: left;
            border: none;
            background: none;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.2s ease;
        }

        .dropdown-content button:hover {
            background-color: #f1f1f1;
            color: #667eea;
        }

        /* выпадающий список при наведении */
        .dropdown:hover .dropdown-content {
            display: block;
        }

        .user-info {
            color: white;
            font-size: 16px;
            font-weight: 500;
            background: rgba(255,255,255,0.2);
            padding: 8px 20px;
            border-radius: 25px;
            backdrop-filter: blur(5px);
        }

        /* Анимация появления */
        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(-10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        .logo {
            position: fixed;
            bottom: 40px;
            right: 40px;
            width: 200px;
            height: auto;
        }

        .category-container {
            margin: 20px auto;
            width: 80%;
            display: none;
        }

        .category-item {
            background-color: white;
            border: 1px solid #ddd;
            padding: 15px;
            margin-bottom: 5px;
            border-radius: 4px;
            box-shadow: 2px 2px 5px rgba(0,0,0,0.05);
            color: #333;
            font-weight: 500;
        }

        .child-category {
            margin-left: 2cm;
            background-color: #f9f9f9;
            border-left: 4px solid #59a950;
        }

        .user-table {
            width: 100%;
            border-collapse: collapse;
            background-color: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }

        .user-table th {
            background-color: #28521a;
            color: white;
            text-align: left;
            padding: 12px 15px;
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 1px;
        }

        .user-table td {
            padding: 12px 15px;
            border-bottom: 1px solid #eee;
            color: #333;
            font-size: 15px;
        }

        .user-table tr:hover {
            background-color: #f1f8f1;
        }

        .user-table tr:last-child td {
            border-bottom: none;
        }

        .role-badge {
            background-color: #59a950;
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
        }

    </style>
</head>
<body>
<div class="navbar">
    <div class="nav-links">
        <!-- 1. Поиск на складе -->
        <button class="nav-btn" onclick="handleButtonClick('search')"><i class="fas fa-search"></i> Поиск на складе</button>

        <!-- 2. Операции -->
        <button class="nav-btn" onclick="handleButtonClick('operations')"><i class="fa-solid fa-clipboard-list"></i>  Журнал операций</button>

        <!-- 3. Аккаунты с выпадающим списком -->
        <div class="dropdown" id="dropdownAccounts">
            <button class="nav-btn" onclick="toggleDropdown('dropdownAccounts')"><i class="fa-solid fa-users"></i> Аккаунты ▼</button>
            <div class="dropdown-content">
                <button onclick="handleButtonClick('account_add')"><i class="fa-solid fa-plus"></i> Добавить</button>
                <button onclick="handleButtonClick('account_delete')"><i class="fa-solid fa-ban"></i> Удалить</button>
                <button onclick="handleButtonClick('account_list')"><i class="fa-solid fa-list"></i> Список</button>
            </div>
        </div>

        <!-- 4. Категории с выпадающим списком -->
        <div class="dropdown" id="dropdownCategories">
            <button class="nav-btn" onclick="toggleDropdown('dropdownCategories')"><i class="fa-solid fa-layer-group"></i> Категории ▼</button>
            <div class="dropdown-content">
                <button onclick="handleButtonClick('category_add')"><i class="fa-solid fa-plus"></i> Добавить</button>
                <button onclick="handleButtonClick('category_delete')"><i class="fa-solid fa-ban"></i> Удалить</button>
                <button onclick="handleButtonClick('category_list')"><i class="fa-solid fa-list"></i> Список</button>
            </div>
        </div>

        <!-- 5. Корректировка остатков -->
        <button class="nav-btn" onclick="handleButtonClick('adjustment')"><i class="fa-solid fa-pencil"></i> Корректировка остатков</button>
    </div>

    <div class="user-info">
        <i class="fa-regular fa-user"></i> ${empty sessionScope.userName ? "Гость" : sessionScope.userName}
    </div>
</div>

<%-- Проверка сообщения об успехе операции --%>
<%
    Boolean success = (Boolean) session.getAttribute("categorySuccess");
    if (success != null && success) {
%>
<div id="successMessage" class="category-container" style="display: block; background-color: #d4edda; color: #155724; padding: 20px; border-radius: 8px; border: 1px solid #c3e6cb; width: 50%; text-align: center; margin-bottom: 20px;">
    <i class="fa-solid fa-circle-check"></i> Операция выполнена успешно!
</div>
<%
        session.removeAttribute("categorySuccess");
    }
    else if (success != null && success == false) {
%>
<div id="successMessage" class="category-container" style="display: block; background-color: #f12323; color: #000000; padding: 20px; border-radius: 8px; border: 1px solid #2b4620; width: 50%; text-align: center; margin-bottom: 20px;">
    <i class="fa-solid fa-circle-exclamation"></i> Операция не выполнена. Проверьте корректность введенных данных.
</div>
<%
    session.removeAttribute("categorySuccess");
    }
%>


<script>
    function hideAllSections() {
        var sections = ['successMessage', 'categoryListSection', 'addCategorySection',
            'deleteCategorySection', 'usersListSection'];
        sections.forEach(function(id) {
            var el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });
    }

    // Функция обработки нажатия на кнопки
    function handleButtonClick(action) {
        console.log('Нажата кнопка:', action);


        switch(action) {
            case 'search':
                hideAllSections();
                break;
            case 'operations':
                hideAllSections();
                break;
            case 'account_add':
                hideAllSections();
                break;
            case 'account_delete':
                hideAllSections();
                break;
            case 'account_list':
                hideAllSections();
                document.getElementById('usersListSection').style.display = 'block';
                break;
            case 'category_add':
                hideAllSections();
                document.getElementById('addCategorySection').style.display = 'block';
                break;
            case 'category_delete':
                hideAllSections();
                document.getElementById('deleteCategorySection').style.display = 'block';
                break;
            case 'category_list':
                hideAllSections();
                document.getElementById('categoryListSection').style.display = 'block';
                break;
            case 'adjustment':
                hideAllSections();
                break;
            default:
                alert('Действие: ' + action);
        }
    }

</script>

<!-- Секция "список категорий" -->
<div id="categoryListSection" class="category-container">
    <%
        ArrayList<Category> categories = (ArrayList<Category>) session.getAttribute("categories");

        if (categories != null && !categories.isEmpty()) {
            for (Category parent : categories) {
                if (parent.getParentId() == null) {
    %>
    <div class="category-item" style="background-color: #e8f5e9; border-left: 5px solid #28521a;">
        <strong><%= parent.getId() %> <%= parent.getName() %></strong>
    </div>
    <%
        for (Category child : categories) {
            if (child.getParentId() != null && child.getParentId().equals(parent.getId())) {
    %>
    <div class="category-item child-category">
        <%= child.getId() %> <%= child.getName() %>
    </div>
    <%
                    }
                }
            }
        }
    } else {
    %>
    <div class="category-item">Список категорий пуст</div>
    <%
        }
    %>
</div>

<!-- секция "добавить категорию -->
<div id="addCategorySection" class="category-container" style="background-color: white; padding: 25px; border-radius: 8px;width: 30%">
    <h2 style="margin-bottom: 20px; color: Black; text-align: center">Добавить новую категорию</h2>

    <form action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="add_category">

        <div style="margin-bottom: 15px;">
            <label style="display: block; margin-bottom: 5px; font-weight: bold;">Название категории:</label>
            <input type="text" name="categoryName" autocomplete="off" required placeholder="Название новой категории"
                   style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px;">
        </div>

        <div style="margin-bottom: 20px;">
            <label style="display: block; margin-bottom: 5px; font-weight: bold;">Родительская категория:</label>
            <select name="parentId" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px;">
                <option value="">-- Новая категория --</option>
                <%
                    ArrayList<Category> selectCategory = (ArrayList<Category>) session.getAttribute("categories");
                    if (selectCategory != null) {
                        for (Category c : selectCategory) {
                            if (c.getParentId() == null) {
                %>
                <option value="<%= c.getId() %>"><%= c.getName() %></option>
                <%
                            }
                        }
                    }
                %>
            </select>
        </div>

        <button type="submit" style="display: block; margin: 0 auto; background-color: #59a950; color: white; border: none; padding: 12px 25px; border-radius: 4px; cursor: pointer; font-weight: bold;">
            <i class="fa-solid fa-plus"></i> Добавить
        </button>
    </form>
</div>

<!-- секция "удалить категорию" -->
<div id="deleteCategorySection" class="category-container" style="background-color: white; padding: 25px; border-radius: 8px;width: 30%">
    <h2 style="margin-bottom: 20px; color: Black; text-align: center">Удаление категории</h2>
    <form action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="delete_category">

        <div style="margin-bottom: 20px;">
            <select name="id" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px;">
                <option value="">-- Не выбрано --</option>
                <%
                    ArrayList<Category> categoryList = (ArrayList<Category>) session.getAttribute("categories");
                    if (categoryList != null) {
                        for (Category c : categoryList) {
                %>
                <option value="<%= c.getId() %>"><%= c.getId()%>    <%= c.getName() %></option>
                <%
                        }
                    }
                %>
            </select>
        </div>
        <button type="submit" style="display: block; margin: 0 auto; background-color: #59a950; color: white; border: none; padding: 12px 25px; border-radius: 4px; cursor: pointer; font-weight: bold;">
            <i class="fa-solid fa-ban"></i> Удалить
        </button>
    </form>
</div>

<!-- секция "список пользователей" -->
<div id="usersListSection" class="category-container" style="width: 90%; margin-top: 30px;">
    <h2 style="margin-bottom: 20px; color: #000000; text-align: center">Список пользователей системы</h2>

    <%
        ArrayList<User> users = (ArrayList<User>) session.getAttribute("userList");

        if (users != null && !users.isEmpty()) {
    %>
    <table class="user-table">
        <thead>
        <tr>
            <th style="width: 10%;">ID</th>
            <th style="width: 40%;">ФИО / Имя</th>
            <th style="width: 25%;">Роль</th>
            <th style="width: 25%;">Логин</th>
        </tr>
        </thead>
        <tbody>
        <% for (User user : users) { %>
        <tr>
            <td><strong>#<%= user.getId() %></strong></td>
            <td><%= user.getName() %></td>
            <td><span class="role-badge"><%= user.getRole() %></span></td>
            <td style="font-family: monospace; color: #666;"><%= user.getLogin() %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <%
    } else {
    %>
    <div class="category-item">Список пользователей пуст или еще не загружен.</div>
    <%
        }
    %>
</div>

<img src="${pageContext.request.contextPath}/images/logo.svg"
     class="logo"
     alt="decorative">

</body>
</html>