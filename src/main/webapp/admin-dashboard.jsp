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
    // Функция обработки нажатия на кнопки
    function handleButtonClick(action) {
        console.log('Нажата кнопка:', action);

        // Здесь вы можете добавить свою логику перехода или отправки формы
        // Примеры:
        switch(action) {
            case 'search':
                alert('Переход к поиску на складе');
                // window.location.href = '/search';
                break;
            case 'operations':
                alert('Переход к операциям');
                // window.location.href = '/operations';
                break;
            case 'account_add':
                alert('Добавление аккаунта');
                // window.location.href = '/accounts/add';
                break;
            case 'account_delete':
                alert('Удаление аккаунта');
                // window.location.href = '/accounts/delete';
                break;
            case 'account_list':
                alert('Список аккаунтов');
                // window.location.href = '/accounts/list';
                break;
            case 'category_add':
                alert('Добавление категории');
                // window.location.href = '/categories/add';
                break;
            case 'category_delete':
                alert('Удаление категории');
                // window.location.href = '/categories/delete';
                break;
            case 'category_list':
                alert('Список категорий');
                // window.location.href = '/categories/list';
                break;
            case 'category_edit':
                alert('Редактирование категории');
                // window.location.href = '/categories/edit';
                break;
            case 'adjustment':
                alert('Корректировка остатков');
                // window.location.href = '/adjustment';
                break;
            default:
                alert('Действие: ' + action);
        }
    }

    // Функция для toggle dropdown на мобильных устройствах
    // function toggleDropdown(dropdownId) {
    //     // Проверяем ширину экрана для мобильной версии
    //     if (window.innerWidth <= 768) {
    //         const dropdown = document.getElementById(dropdownId);
    //         dropdown.classList.toggle('active');
    //     }
    // }

    // Закрываем выпадающие списки при клике вне области (для мобильной версии)
    // document.addEventListener('click', function(event) {
    //     if (window.innerWidth <= 768) {
    //         const dropdowns = document.querySelectorAll('.dropdown');
    //         dropdowns.forEach(dropdown => {
    //             if (!dropdown.contains(event.target)) {
    //                 dropdown.classList.remove('active');
    //             }
    //         });
    //     }
    // });
</script>

<div style="padding: 40px; text-align: center;">
    <h1>Добро пожаловать, ${empty sessionScope.userName ? "Гость" : sessionScope.userName}!</h1>
</div>
<img src="${pageContext.request.contextPath}/images/logo.svg"
     class="logo"
     alt="decorative">
</body>
</html>