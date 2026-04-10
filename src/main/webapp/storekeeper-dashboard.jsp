<%@ page import="com.wmsjenty.model.Category" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.wmsjenty.model.User" %>
<%@ page import="com.wmsjenty.model.Operation" %>
<%@ page import="com.wmsjenty.model.Item" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css">
    <meta charset="UTF-8">
    <title>WMS-Jenty Storekeeper</title>
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
        <!-- поиск на складе -->
        <button class="nav-btn" onclick="handleButtonClick('search')"><i class="fas fa-search"></i> Поиск на складе</button>

        <!-- журнал операций -->
        <button class="nav-btn" onclick="handleButtonClick('operations')"><i class="fa-solid fa-clipboard-list"></i>  Журнал операций</button>

        <!-- расход -->
        <div class="dropdown" id="dropdownAccounts">
            <button class="nav-btn" onclick="toggleDropdown('dropdownOutgo')"><i class="fa-solid fa-arrow-turn-up"></i> Расход ▼</button>
            <div class="dropdown-content">
                <button onclick="handleButtonClick('outgo_add')"><i class="fa-solid fa-plus"></i> Оформление</button>
                <button onclick="handleButtonClick('outgo_log')"><i class="fa-solid fa-list"></i>  Журнал</button>
            </div>
        </div>

        <!-- приход -->
        <button class="nav-btn" onclick="handleButtonClick('income')"><i class="fa-solid fa-arrow-turn-down"></i> Приход</button>

        <!-- прогнозирование -->
        <button class="nav-btn" onclick="handleButtonClick('forecast')"><i class="fa-solid fa-chart-line"></i> Прогнозирование</button>

        <!-- инвентаризация -->
        <button class="nav-btn" onclick="handleButtonClick('inventory')"><i class="fa-solid fa-pen-to-square"></i> Инвентаризация</button>

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
        var sections = ['successMessage', 'logSelectSection',
            'logResults', 'searchSection', 'searchResultsSection'];
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
            case 'outgo_add':
                hideAllSections();
                document.getElementById('addOutgoSection').style.display = 'block';
                break;
            case 'outgo_log':
                hideAllSections();
                document.getElementById('outgoLogSection').style.display = 'block';
                break;
            case 'income':
                hideAllSections();
                document.getElementById('incomeSection').style.display = 'block';
                break;
            case 'category_add':
                hideAllSections();
                document.getElementById('addCategorySection').style.display = 'block';
                break;
            case 'forecast':
                hideAllSections();
                document.getElementById('forecastSection').style.display = 'block';
                break;
            case 'inventory':
                hideAllSections();
                document.getElementById('inventorySection').style.display = 'block';
                break;
            default:
                alert('Действие: ' + action);
        }
    }
</script>



<!-- секция "поиск товара на складе -->
<div id="searchSection" class="category-container" style="display: none; width: 95%; background: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
    <form action="${pageContext.request.contextPath}/storekeeper/dashboard" method="GET" style="display: flex; gap: 15px; align-items: flex-end; flex-wrap: wrap;">
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

<!-- секция "выбор операций для журнала" -->
<div id="logSelectSection" class="user-form-card category-container" style="display: none;">
    <h2>Журнал операций</h2>
    <form action="${pageContext.request.contextPath}/storekeeper/dashboard" method="GET">
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
            <select name="operationType" class="form-control">
                <option value="">-- Все типы операций --</option>
                <option value="приход">Приход</option>
                <option value="расход">Расход</option>
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
            <th>Операция</th>
            <th>Номер документа</th>
            <th>Комментарий</th>
        </tr>
        </thead>
        <tbody>
        <% for (Operation op : logs) { %>
        <tr>
            <td style="white-space: nowrap;"><%= op.getOperationDate() %></td>
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





<img src="${pageContext.request.contextPath}/images/logo.svg"
     class="logo"
     alt="decorative">

</body>
</html>
