<%@ page import="com.wmsjenty.model.Category" %>
<%@ page import="java.util.ArrayList" %>
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

        /* Основной контейнер навигации */
        .navbar {
            background: linear-gradient(135deg, #59a950 0%, #28521a 100%);
            padding: 0 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            position: relative;
        }

        /* Левая часть с кнопками */
        .nav-links {
            display: flex;
            gap: 5px;
            flex-wrap: wrap;
        }

        /* Стили для кнопок */
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

        /* Содержимое выпадающего списка */
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

        /* Кнопки внутри выпадающего списка */
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

        /* Показываем выпадающий список при наведении или активном состоянии */
        .dropdown:hover .dropdown-content {
            display: block;
        }

        /* Правая часть с именем пользователя */
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
            width: 200px;        /* размер по желанию */
            height: auto;
        }

        /* Контейнер для всего списка */
        .category-container {
            margin: 20px auto;
            width: 80%;
            display: none; /* По умолчанию скрыт */
        }

        /* Общий стиль для всех прямоугольников */
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

        /* Отступ для подкатегорий (примерно 2 см) */
        .child-category {
            margin-left: 2cm;
            background-color: #f9f9f9;
            border-left: 4px solid #59a950; /* Зеленая полоска слева для красоты */
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

    <!-- Правая часть с именем пользователя -->
    <div class="user-info">
        <i class="fa-regular fa-user"></i> ${empty sessionScope.userName ? "Гость" : sessionScope.userName}
    </div>
</div>

<script>
    function hideAllSections() {
        var categoryBlock = document.getElementById('categoryListSection');
        if (categoryBlock) {
            categoryBlock.style.display = 'none';
        }

        // Завтра сюда добавим скрытие других блоков (аккаунты, поиск и т.д.)
        // document.getElementById('accountsSection').style.display = 'none';
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
                break;
            case 'category_add':
                hideAllSections();
                break;
            case 'category_delete':
                hideAllSections();
                break;
            case 'category_list':
                var block = document.getElementById('categoryListSection');
                if (block.style.display === "block") {
                    block.style.display = "none";
                } else {
                    block.style.display = "block";
                }
                break;
            case 'adjustment':
                hideAllSections();
                break;
            default:
                alert('Действие: ' + action);
        }
    }

</script>

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

<img src="${pageContext.request.contextPath}/images/logo.svg"
     class="logo"
     alt="decorative">
</body>
</html>