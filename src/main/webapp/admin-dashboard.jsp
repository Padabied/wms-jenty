<%@ page import="com.wmsjenty.model.Category" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.wmsjenty.model.User" %>
<%@ page import="com.wmsjenty.model.Operation" %>
<%@ page import="com.wmsjenty.model.Item" %>
<%@ page import="com.wmsjenty.service.DBDataLoader" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css">
    <meta charset="UTF-8">
    <title>WMS-Jenty Admin</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/images/logo.svg">
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

        .user-dropdown:hover .dropdown-content {
            display: block;
        }

        .logout-dropdown {
            right: 0;
            left: auto;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        }

        .role-badge {
            background-color: #59a950;
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
        }
        :root {
            --primary-color: #59a950;
            --primary-hover: #4a8e42;
            --bg-color: #f4f7f6;
            --border-color: #e0e0e0;
            --text-color: #333;
            --shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        .user-form-card {
            background-color: #ffffff;
            padding: 40px;
            border-radius: 12px;
            width: 100%;
            max-width: 400px;
            margin: 20px auto;
            box-shadow: var(--shadow);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        .user-form-card h2 {
            margin-top: 0;
            margin-bottom: 25px;
            color: var(--text-color);
            text-align: center;
            font-weight: 600;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-control {
            width: 100%;
            padding: 12px 15px;
            border: 1.5px solid var(--border-color);
            border-radius: 6px;
            font-size: 14px;
            transition: border-color 0.3s ease, box-shadow 0.3s ease;
            box-sizing: border-box; /* Важно для правильного расчета ширины */
        }

        .form-control:focus {
            outline: none;
            border-color: var(--primary-color);
            box-shadow: 0 0 0 3px rgba(89, 169, 80, 0.15);
        }

        .btn-submit {
            display: block;
            width: 100%;
            background-color: var(--primary-color);
            color: white;
            border: none;
            padding: 14px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 16px;
            font-weight: bold;
            transition: background-color 0.3s ease;
        }

        .btn-submit:hover {
            background-color: var(--primary-hover);
        }

        .btn-submit i {
            margin-right: 8px;
        }

        select.form-control {
            appearance: none;
            background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
            background-repeat: no-repeat;
            background-position: right 15px center;
            background-size: 15px;
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

    <div class="dropdown user-dropdown">
        <div class="user-info">
            <i class="fa-regular fa-user"></i>
            ${empty sessionScope.userName ? "Гость" : sessionScope.userName}
            <i class="fa-solid fa-chevron-down" style="font-size: 10px; margin-left: 5px;"></i>
        </div>

        <div class="dropdown-content logout-dropdown">
            <form action="${pageContext.request.contextPath}/login" method="POST" style="margin: 0;">
                <input type="hidden" name="action" value="logout">
                <button type="submit" style="display: block; width: 100%; border: none; background: none; text-align: left; padding: 12px 16px; cursor: pointer;">
                    <i class="fa-solid fa-right-from-bracket"></i> Выход
                </button>
            </form>
        </div>
    </div>
</div>

<%-- Проверка сообщения об успехе операции --%>
<%
    Boolean success = (Boolean) session.getAttribute("successMessage");
    if (success != null && success) {
%>
<div id="successMessage" class="category-container" style="display: block; background-color: #d4edda; color: #000000; padding: 20px; border-radius: 8px; border: 1px solid #c3e6cb; width: 50%; text-align: center; margin-bottom: 20px;">
    <i class="fa-solid fa-circle-check"></i> Операция выполнена успешно!
</div>
<%
        session.removeAttribute("successMessage");
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
            'deleteCategorySection', 'usersListSection', 'addUserSection', 'deleteUserSection', 'logSelectSection',
        'logResults', 'adjustmentSection', 'searchSection', 'searchResultsSection'];
        sections.forEach(function(id) {
            var el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });
    }

    // Функция обработки нажатия на кнопки
    function handleButtonClick(action) {

        switch(action) {
            case 'search':
                hideAllSections();
                document.getElementById('searchSection').style.display = 'block';
                break;
            case 'operations':
                hideAllSections();
                document.getElementById('logSelectSection').style.display = 'block';
                break;
            case 'account_add':
                hideAllSections();
                document.getElementById('addUserSection').style.display = 'block';
                break;
            case 'account_delete':
                hideAllSections();
                document.getElementById('deleteUserSection').style.display = 'block';
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
                document.getElementById('adjustmentSection').style.display = 'block';
                break;
            default:
                alert('Действие: ' + action);
        }
    }

    function showAdjustmentConfirm() {
        const form = document.getElementById('adjustmentForm');
        const itemId = form.itemId.value;
        const newValue = form.value.value;

        if (!itemId || !newValue) {
            alert("Заполните ID товара и новое значение");
            return;
        }

        document.getElementById('confirmModal').style.display = 'block';
        document.getElementById('confirmItemId').innerText = itemId;
        document.getElementById('confirmNewValue').innerText = newValue;
        document.getElementById('confirmItemName').innerText = "Загрузка...";
        document.getElementById('confirmOldValue').innerText = "...";

        // Фоновый запрос к сервлету для получения данных о товаре
        fetch('${pageContext.request.contextPath}/admin/dashboard?action=getItemInfo&itemId=' + itemId)
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    document.getElementById('confirmItemName').innerText = "Товар не найден";
                } else {
                    document.getElementById('confirmItemName').innerText = data.name;
                    document.getElementById('confirmOldValue').innerText = data.value;
                }
            })
            .catch(err => {
                document.getElementById('confirmItemName').innerText = "Ошибка загрузки";
            });

        document.getElementById('finalConfirmBtn').onclick = function() {
            form.submit();
        };
    }

    function closeModal() {
        document.getElementById('confirmModal').style.display = 'none';
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
<div id="addCategorySection" class="user-form-card category-container" style="display: none;">
    <h2>Добавить категорию</h2>

    <form action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="add_category">

        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                Название категории
            </label>
            <input type="text" name="categoryName" class="form-control" autocomplete="off"
                   required placeholder="Введите название...">
        </div>

        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                Родительская категория
            </label>
            <select name="parentId" class="form-control">
                <option value="">-- Новая (корневая) --</option>
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

        <button type="submit" class="btn-submit">
            <i class="fa-solid fa-plus"></i> Добавить категорию
        </button>
    </form>
</div>

<!-- секция "удалить категорию" -->
<div id="deleteCategorySection" class="user-form-card category-container" style="display: none;">
    <h2>Удаление категории</h2>

    <form action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="delete_category">

        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                Выберите категорию для удаления
            </label>
            <select name="id" class="form-control" required>
                <option value="" disabled selected>-- Не выбрано --</option>
                <%
                    ArrayList<Category> categoryList = (ArrayList<Category>) session.getAttribute("categories");
                    if (categoryList != null) {
                        for (Category c : categoryList) {
                %>
                <option value="<%= c.getId() %>"><%= c.getId() %> — <%= c.getName() %></option>
                <%
                        }
                    }
                %>
            </select>
            <small style="display: block; margin-top: 10px; color: #a94442; font-size: 12px;">
                <i class="fa-solid fa-triangle-exclamation"></i> Убедитесь, что в заданной категории нет товаров
            </small>
        </div>

        <button type="submit" class="btn-submit" style="background-color: #d9534f;">
            <i class="fa-solid fa-ban"></i> Удалить категорию
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

<!-- секция "добавление пользователя"-->
<div id="addUserSection" class="user-form-card category-container" style="display: none;">
    <h2>Создание аккаунта</h2>
    <form action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="add_user">

        <div class="form-group">
            <input type="text" name="name" class="form-control" autocomplete="off" required placeholder="Фамилия, Имя, Отчество">
        </div>

        <div class="form-group">
            <input type="text" name="login" class="form-control" autocomplete="off" required placeholder="Логин">
        </div>

        <div class="form-group">
            <input type="password" name="password" class="form-control" autocomplete="off" required placeholder="Пароль">
        </div>

        <div class="form-group">
            <input type="password" name="confirm_password" class="form-control" autocomplete="off" required placeholder="Подтвердите пароль">
        </div>

        <div class="form-group">
            <select name="role" class="form-control" required>
                <option value="" disabled selected>Выберите роль</option>
                <option value="администратор">Администратор</option>
                <option value="кладовщик">Кладовщик</option>
            </select>
        </div>

        <button type="submit" class="btn-submit">
            <i class="fa-solid fa-plus"></i> Создать аккаунт
        </button>
        <small style="display: block; margin-top: 10px; color: #337a2e; font-size: 12px;">
            <i class="fa-solid fa-triangle-exclamation"></i> Убедитесь, что пароль записан
        </small>
    </form>
</div>

<!-- секция "удаление аккаунта -->
<div id="deleteUserSection" class="user-form-card category-container" style="display: none;">
    <h2>Удаление аккаунта</h2>
    <form action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="delete_user">
        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                Выберите аккаунт для удаления
            </label>
            <select name="id" class="form-control" required>
                <option value="" disabled selected>-- Не выбрано --</option>
                <%
                    ArrayList<User> accountsList = (ArrayList<User>) session.getAttribute("userList");
                    if (accountsList != null) {
                        for (User user : accountsList) {
                %>
                <option value="<%= user.getId() %>"><%= user.getName() %></option>
                <%
                        }
                    }
                %>
            </select>
            <small style="display: block; margin-top: 10px; color: #a94442; font-size: 12px;">
                <i class="fa-solid fa-triangle-exclamation"></i> Если выбранный пользователь выполнял операции, аккаунт будет деактивирован
            </small>
        </div>

        <button type="submit" class="btn-submit" style="background-color: #d9534f;">
            <i class="fa-solid fa-ban"></i> Удалить аккаунт
        </button>
    </form>
</div>

<!-- секция "выбор операций для журнала" -->
<div id="logSelectSection" class="user-form-card category-container" style="display: none;">
    <h2>Журнал операций</h2>
    <form action="${pageContext.request.contextPath}/admin/dashboard" method="GET">
        <input type="hidden" name="action" value="get_logs">

        <div class="form-group">
            <label style="display: block; margin-bottom: 5px; font-size: 13px; color: #666;">Начальная дата</label>
            <input type="date" name="startDate" class="form-control" onclick="this.showPicker()">
        </div>

        <div class="form-group">
            <label style="display: block; margin-bottom: 5px; font-size: 13px; color: #666;">Конечная дата</label>
            <input type="date" name="endDate" class="form-control" onclick="this.showPicker()">
        </div>


        <div class="form-group">
            <select name="userId" class="form-control">
                <option value="">-- Все пользователи --</option>
                <%
                    ArrayList<User> logUsers = (ArrayList<User>) session.getAttribute("userList");
                    if (logUsers != null) {
                        for (User u : logUsers) {
                %>
                <option value="<%= u.getId() %>"><%= u.getName() %></option>
                <%
                        }
                    }
                %>
            </select>
        </div>

        <div class="form-group">
            <select name="operationType" class="form-control">
                <option value="">-- Все типы операций --</option>
                <option value="приход">Приход</option>
                <option value="расход">Расход</option>
                <option value="добавление аккаунта">Добавление аккаунта</option>
                <option value="удаление аккаунта">Удаление аккаунта</option>
                <option value="добавление категории">Добавление категории</option>
                <option value="удаление категории">Удаление категории</option>
                <option value="корректировка остатков">Корректировка остатков</option>
            </select>
        </div>

        <button type="submit" class="btn-submit">
            <i class="fa-solid fa-magnifying-glass"></i> Показать
        </button>
    </form>
</div>

<!-- секция "отображение журнала операций"-->
<div id="logResults" class="category-container" style="width: 95%; margin-top: 30px;
        display: <%= (session.getAttribute("logs") != null) ? "block" : "none" %>;">
    <h2 style="margin-bottom: 20px; color: #333; text-align: center">Журнал операций</h2>

    <%
        ArrayList<Operation> logs = (ArrayList<Operation>) session.getAttribute("logs");
        if (logs != null && !logs.isEmpty()) {
    %>
    <table class="user-table">
        <thead>
        <tr>
            <th>Дата</th>
            <th>Пользователь</th>
            <th>Операция</th>
            <th>Номер документа</th>
            <th>Комментарий</th>
        </tr>
        </thead>
        <tbody>
        <% for (Operation op : logs) { %>
        <tr>
            <td style="white-space: nowrap;"><%= op.getOperationDate() %></td>
            <td><%= DBDataLoader.getUserId(op.getUserId()) %></td>
            <td><span class="role-badge" style="background-color: #28521a;"><%= op.getOperationType() %></span></td>
            <td><%= op.getDocumentId() %></td>
            <td><%= op.getComment() != null ? op.getComment() : "" %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <%
        session.removeAttribute("logs");
    } else if (logs != null) {
    %>
    <div style="text-align: center; margin: 40px auto; font-size: 20px;">
        По заданным фильтрам операций не найдено
    </div>
    <%
        } %>
</div>

<!-- секция "корректировка остатков" -->
<div id="adjustmentSection" class="user-form-card category-container" style="display: none;">
    <h2>Корректировка остатков</h2>

    <form id="adjustmentForm" action="/admin/dashboard" method="POST">
        <input type="hidden" name="action" value="adjustment">

        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                ID товара на складе
            </label>
            <input type="text" name="itemId" class="form-control" autocomplete="off"
                   required placeholder="Введите ID товара">
        </div>

        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                Новое значение остатка
            </label>
            <input type="text" name="value" class="form-control" autocomplete="off"
                   required placeholder="Введите новое значение остатка">
        </div>

        <div class="form-group">
            <label style="display: block; margin-bottom: 8px; font-size: 14px; color: #666; font-weight: 500;">
                Причина корректировки
            </label>
            <input type="text" name="comment" class="form-control" autocomplete="off"
                   required placeholder="Оставьте комментарий">
        </div>

        <button type="button" onclick="showAdjustmentConfirm()" class="btn-submit">
            Корректировка
        </button>
    </form>
</div>

<!-- секция подтверждающего окна для функции корректировки остатков -->
<div id="confirmModal" style="display: none; position: fixed; z-index: 2000; left: 0; top: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); backdrop-filter: blur(3px);">
    <div class="user-form-card" style="position: relative; top: 50%; transform: translateY(-50%); max-width: 450px; margin: auto;">
        <h2 style="color: #28521a;"><i class="fa-solid fa-circle-question"></i> Подтверждение</h2>
        <div id="modalContent" style="margin-bottom: 25px; line-height: 1.6; font-size: 15px;">
            <p><strong>Наименование:</strong> <span id="confirmItemName">Загрузка...</span></p>
            <p><strong>ID товара:</strong> <span id="confirmItemId"></span></p>
            <p><strong>Старое значение:</strong> <span id="confirmOldValue">...</span></p>
            <p><strong>Новое значение:</strong> <span id="confirmNewValue" style="color: #d9534f; font-weight: bold;"></span></p>
        </div>
        <div style="display: flex; gap: 10px;">
            <button type="button" id="finalConfirmBtn" class="btn-submit" style="flex: 1;">Подтвердить</button>
            <button type="button" onclick="closeModal()" class="btn-submit" style="flex: 1; background-color: #666;">Отмена</button>
        </div>
    </div>
</div>

<!-- секция "поиск товара на складе -->
<div id="searchSection" class="category-container" style="display: none; width: 95%; background: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
    <form action="${pageContext.request.contextPath}/admin/dashboard" method="GET" style="display: flex; gap: 15px; align-items: flex-end; flex-wrap: wrap;">
        <input type="hidden" name="action" value="search_items">

        <div style="flex: 1; min-width: 200px;">
            <label style="font-size: 12px; color: #666; font-weight: bold; display: block; margin-bottom: 5px;">Категория</label>
            <select name="searchCategory" class="form-control" style="margin-bottom: 0;">
                <option value="">-- Все категории --</option>
                <%
                    ArrayList<Category> searchCategories = (ArrayList<Category>) session.getAttribute("categories");
                    if (searchCategories != null) {
                        for (Category c : searchCategories) {
                %>
                <option value="<%= c.getId() %>"><%= c.getName() %></option>
                <%
                        }
                    }
                %>
            </select>
        </div>

        <div style="flex: 2; min-width: 250px;">
            <label style="font-size: 12px; color: #666; font-weight: bold; display: block; margin-bottom: 5px;">Наименование</label>
            <input type="text" name="searchName" class="form-control" autocomplete="off" placeholder="Название или часть названия" style="margin-bottom: 0;">
        </div>

        <div style="flex: 1; min-width: 150px;">
            <label style="font-size: 12px; color: #666; font-weight: bold; display: block; margin-bottom: 5px;">Артикул</label>
            <input type="text" name="searchArticle" class="form-control" autocomplete="off" placeholder="Артикул" style="margin-bottom: 0;">
        </div>

        <button type="submit" class="btn-submit" style="width: auto; padding: 10px 30px; height: 43px;">
            <i class="fa-solid fa-magnifying-glass"></i> Показать
        </button>
    </form>
</div>

<!-- секция отображения результатов поиска товара на складе -->
<div id="searchResultsSection" class="category-container" style="width: 95%; display: <%= (session.getAttribute("foundItems") != null) ? "block" : "none" %>;">
    <h2 style="text-align: center; margin-bottom: 20px;">Результаты поиска</h2>
    <table class="user-table">
        <thead>
        <tr>
            <th>ID</th>
            <th>Категория</th>
            <th>Наименование</th>
            <th>Артикул</th>
            <th>Бренд</th>
            <th>Остаток</th>
        </tr>
        </thead>
        <tbody>
        <%
            ArrayList<Item> items = (ArrayList<Item>) session.getAttribute("foundItems");
            if (items != null && !items.isEmpty()) {
                for (Item it : items) {
        %>
        <tr>
            <td><strong>#<%= it.getId() %></strong></td>
            <td><%= it.getCategoryName() %></td>
            <td><%= it.getName() %></td>
            <td><%= it.getArticle() %></td>
            <td><%= it.getBrand() %></td>
            <td style="font-weight: bold; color: #28521a;"><%= it.getValue() %></td>
        </tr>
        <%
            }
            session.removeAttribute("foundItems");
        } else if (items != null) {
        %>
        <tr><td colspan="6" style="text-align: center;">Ничего не найдено</td></tr>
        <% } %>
        </tbody>
    </table>
</div>

<img src="${pageContext.request.contextPath}/images/logo.svg"
     class="logo"
     alt="decorative">

</body>
</html>