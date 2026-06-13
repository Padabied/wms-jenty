/**
 * Скрытие секций.
 */
    function hideAllSections() {

    var sections = ['successMessage', 'logSelectSection',
    'logResults', 'searchSection', 'searchResultsSection', 'forecastSection', 'inventorySection',
    'addOutgoSection', 'outgoLogSelect', 'outgoLogResults', 'addIncomeSection'];
    sections.forEach(function(id) {
    var el = document.getElementById(id);
    if (el) el.style.display = 'none';
});
}

/**
 * Функция собирает данные о товарах, подлежащих закупке (функция инвенторизации кладовщика) и записывает
 * в .txt файл, оформляя текст в установленном формате.
 */
    function exportInventory() {
    var table = document.querySelector('#inventorySection .user-table');
    var rows = table.querySelectorAll('tbody tr');

    var data = [];

    var colWidths = {
    id: 10,
    name: 35,
    article: 15,
    brand: 15,
    value: 11,
    actual: 11
};

    var today = new Date();
    var dateForFilename = today.toISOString().slice(0, 10).replace(/-/g, '_');

    // выравнивание текста добавлением пробелов
    function padRight(str, length) {
    if (!str) str = "";
    if (str.length > length) {
    return str.substring(0, length - 3) + "...";
}
    return str + " ".repeat(length - str.length);
}

    function makeSeparator() {
    var line = "";
    line += "+" + "-".repeat(colWidths.id) + "+";
    line += "-".repeat(colWidths.name) + "+";
    line += "-".repeat(colWidths.article) + "+";
    line += "-".repeat(colWidths.brand) + "+";
    line += "-".repeat(colWidths.value) + "+";
    line += "-".repeat(colWidths.actual) + "+";
    return line;
}

    data.push("Текущий остаток на складе");
    data.push("Дата: " + new Date().toLocaleDateString('ru-RU'));
    data.push("");
    data.push(makeSeparator());

    var header = "|" + padRight("ID", colWidths.id) + "|";
    header += padRight("Наименование", colWidths.name) + "|";
    header += padRight("Артикул", colWidths.article) + "|";
    header += padRight("Бренд", colWidths.brand) + "|";
    header += padRight("Количество", colWidths.value) + "|";
    header += padRight("Фактическое", colWidths.actual) + "|";
    data.push(header);
    data.push(makeSeparator());

    // данные
    for (var i = 0; i < rows.length; i++) {
    var cols = rows[i].querySelectorAll('td');
    if (cols.length === 5) {
    var line = "|" + padRight(cols[0].innerText, colWidths.id) + "|";
    line += padRight(cols[1].innerText, colWidths.name) + "|";
    line += padRight(cols[2].innerText, colWidths.article) + "|";
    line += padRight(cols[3].innerText, colWidths.brand) + "|";
    line += padRight(cols[4].innerText, colWidths.value) + "|";
    line += padRight("", colWidths.actual) + "|";
    data.push(line);
}
}

    data.push(makeSeparator());
    data.push("");
    data.push("Всего позиций: " + rows.length);

    var blob = new Blob([data.join("\n")], {type: "text/plain;charset=utf-8"});
    var link = document.createElement("a");
    var url = URL.createObjectURL(blob);
    link.href = url;
    link.download = "Остатки на " + dateForFilename + ".txt";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

/**
 * Функция добавляет товар в список расходной накладной. Производится проверка корректности введенных
 * данных, затем в случае успеха обновляется таблица добавленных файлов в накладной.
 */
    function addItem() {
    var article = document.getElementById('article').value.trim();
    var value = document.getElementById('value').value;
    var errorDiv = document.getElementById('itemError');

    if (!article || !value || parseInt(value) <= 0) {
    errorDiv.innerText = "Введите положительное число";
    errorDiv.style.display = 'block';
    return;
}

    if (!article || !value) {
    alert("Заполните все поля");
    return;
}

    var tbody = document.getElementById('itemsBody');
    var rows = Array.from(tbody.rows);

    var isAlreadyAdded = rows.some(function(row) {
    return row.cells[0] && row.cells[0].innerText === article;
});

    if (isAlreadyAdded) {
    errorDiv.innerText = "Этот товар уже добавлен";
    errorDiv.style.display = 'block';
    return;
}

    var url = "/storekeeper/dashboard?action=check_item&article=" + encodeURIComponent(article) + "&value=" + encodeURIComponent(value);

    fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
    .then(function(response) { return response.json(); })
    .then(function(data) {
    if (data.status === "success") {
    errorDiv.style.display = 'none';
    updateTableBody(data);
    document.getElementById('article').value = '';
    document.getElementById('value').value = '';
} else {
    errorDiv.innerText = data.message;
    errorDiv.style.display = 'block';
}
})
    .catch(function(err) {
    console.error(err);
    alert("Ошибка связи с сервером");
});
}

/**
 * Обновление списка добавленных товаров в расходной накладной.
 */
    function updateTableBody(item) {
    var tbody = document.getElementById('itemsBody');

    if (tbody.rows.length === 1 && tbody.rows[0].cells.length < 3) {
    tbody.innerHTML = '';
}

    var row = tbody.insertRow();
    row.innerHTML = '<td>' + item.article + '</td>' +
    '<td>' + item.name + '</td>' +
    '<td>' + item.value + '</td>';
}

/**
 * Очистка полей формы и внутреннего списка в сервлете после закрытия формы расхода.
 */

    function clearOutgoData() {
    var tbody = document.getElementById('itemsBody');
    if (tbody) {
    tbody.innerHTML = '<tr><td colspan="3" style="text-align: center;">Товары не добавлены</td></tr>';
}

    var itemForm = document.getElementById('itemForm');
    if (itemForm) {
    itemForm.reset();
}
    var receiverForm = document.getElementById('receiverForm');
    if (receiverForm) {
    receiverForm.reset();
}

    var errorDiv = document.getElementById('itemError');
    if (errorDiv) {
    errorDiv.style.display = 'none';
}

    fetch('/storekeeper/dashboard?action=clear_outgo', {
    headers: { 'X-Requested-With': 'XMLHttpRequest' }
}).catch(err => console.error('Ошибка при очистке списка на сервере:', err));
}

/**
 * Оформление расходной накладной. Функция производит сбор входных данных из полей формы,
 * затем оформляет запрос на оформление накладной к серверу.
 */

    function submitOutgo() {
    const receiver = document.getElementsByName('receiverName')[0].value.trim();
    const regNum = document.getElementsByName('regNumber')[0].value.trim();

    if (!receiver || !regNum) {
    alert("Заполните данные");
    return;
}

    //динамическое создание формы для отправки на сервлет
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/storekeeper/dashboard';

    const params = {
    'action': 'confirm_outgo',
    'receiverName': receiver,
    'regNumber': regNum
};

    for (let key in params) {
    let input = document.createElement('input');
    input.type = 'hidden';
    input.name = key;
    input.value = params[key];
    form.appendChild(input);
}

    document.body.appendChild(form);
    form.submit();
}

/**
 * Добавление товара в список приходной накладной. Данные собираются из формы, после чего происзводится
 * проверка корректности введенных данных. В случае успеха выполняется обновление таблицы добавленных
 * товаров.
 */

    function addIncomeItem() {
    var article = document.getElementById('incomeItemArticle').value.trim();
    var value = document.getElementById('incomeItemValue').value;
    var errorDiv = document.getElementById('incomeItemError');

    if (!article || !value || parseInt(value) <= 0) {
    errorDiv.innerText = "Введите положительное число";
    errorDiv.style.display = 'block';
    return;
}

    if (!article || !value) {
    alert("Заполните все поля");
    return;
}

    var tbody = document.getElementById('incomeItemsBody');
    var rows = Array.from(tbody.rows);

    var isAlreadyAdded = rows.some(function(row) {
    return row.cells[0] && row.cells[0].innerText === article;
});

    if (isAlreadyAdded) {
    errorDiv.innerText = "Этот товар уже добавлен";
    errorDiv.style.display = 'block';
    return;
}

    var url = "/storekeeper/dashboard?action=check_income_item&article=" + encodeURIComponent(article) + "&value=" + encodeURIComponent(value);

    fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
    .then(function(response) { return response.json(); })
    .then(function(data) {
    if (data.status === "success") {
    errorDiv.style.display = 'none';

    data.value = value;
    updateIncomeTableBody(data);
    document.getElementById('incomeItemArticle').value = '';
    document.getElementById('incomeItemValue').value = '';
} else if (data.status === "not_found") {
    document.getElementById('newItemModal').style.display = 'block';
    document.getElementById('modalOverlay').style.display = 'block';
    errorDiv.style.display = 'none';
} else {
    errorDiv.innerText = data.message;
    errorDiv.style.display = 'block';
}
})
    .catch(function(err) {
    console.error(err);
    alert("Ошибка связи с сервером");
});
}

/**
 * Обновление списка добавленных товаров в приходной накладной.
 */

    function updateIncomeTableBody(item) {
    var tbody = document.getElementById('incomeItemsBody');

    if (tbody.rows.length === 1 && tbody.rows[0].cells.length < 3) {
    tbody.innerHTML = '';
}

    var row = tbody.insertRow();
    row.innerHTML = '<td>' + item.article + '</td>' +
    '<td>' + item.name + '</td>' +
    '<td>' + item.value + '</td>';
}

/**
 * Сохранение товара, ранее не существующего в таблице item в список newItems для
 * дальнейшего оформления накладной прихода.
 */

    function saveNewItemToList() {
    const article = document.getElementById('incomeItemArticle').value.trim();
    const value = document.getElementById('incomeItemValue').value;

    const params = new URLSearchParams();
    params.append('action', 'add_new_item_temp');
    params.append('article', article);
    params.append('value', value);
    params.append('name', document.getElementById('newName').value);
    params.append('category', document.getElementById('newCategory').value);
    params.append('brand', document.getElementById('newBrand').value);
    params.append('minVal', document.getElementById('newMinVal').value);
    params.append('recVal', document.getElementById('newRecVal').value);

    fetch('/storekeeper/dashboard', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params
})
    .then(response => response.json())
    .then(data => {
    if (data.status === "success") {
    updateIncomeTableBody({
    article: article,
    name: document.getElementById('newName').value,
    value: value
});
    closeModal();
    // очистка основных полей
    document.getElementById('incomeItemArticle').value = '';
    document.getElementById('incomeItemValue').value = '';
} else {
    alert("Ошибка при сохранении: " + data.message);
}
});
}

/**
 * Закрытие модального окна создания нового товара в таблице item базы данных.
 */
    function closeModal() {
    document.getElementById('newItemModal').style.display = 'none';
    document.getElementById('modalOverlay').style.display = 'none';
    document.getElementById('newName').value = '';
    document.getElementById('newBrand').value = '';
}

/**
 * Оформление накладной прихода. Функция собирает информацию из формы,производит проверку корректности
 * введенных данных, после чего выполняет запрос на сервлет.
 */

    function submitIncome() {
    const noteNumber = document.getElementsByName('receiptNoteNumber')[0].value.trim();
    const supplier = document.getElementsByName('supplierName')[0].value.trim();

    if (!noteNumber || !supplier) {
    alert("Заполните номер накладной и наименование поставщика");
    return;
}

    const tbody = document.getElementById('incomeItemsBody');
    if (tbody.rows.length === 1 && tbody.rows[0].cells.length < 3) {
    alert("Заполните накладную");
    return;
}

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/storekeeper/dashboard';

    const params = {
    'action': 'confirm_income',
    'noteNumber': noteNumber,
    'supplierName': supplier
};

    for (let key in params) {
    let input = document.createElement('input');
    input.type = 'hidden';
    input.name = key;
    input.value = params[key];
    form.appendChild(input);
}

    document.body.appendChild(form);
    form.submit();
}

/**
 * Обработка нажатия на кнопки навигационной панели.
 * @param action определяет нажатую кнопку.
 */
    function handleButtonClick(action) {
    if (action !== 'income') {
        clearIncomeForm();
    }
    hideAllSections();
    if (action !== 'outgo_add' && action !== 'search') {
    clearOutgoData();
}

    switch(action) {
    case 'search':
    document.getElementById('searchSection').style.display = 'block';
    break;
    case 'operations':
    document.getElementById('logSelectSection').style.display = 'block';
    break;
    case 'outgo_add':
    document.getElementById('addOutgoSection').style.display = 'block';
    break;
    case 'outgo_log':
    document.getElementById('outgoLogSelect').style.display = 'block';
    break;
    case 'income':
    document.getElementById('addIncomeSection').style.display = 'block';
    break;
    case 'forecast':
    document.getElementById('forecastSection').style.display = 'block';
    break;
    case 'inventory':
    document.getElementById('inventorySection').style.display = 'block';
    break;
    default:
    alert('Действие: ' + action);
}
}

/**
 * Очистка формы прихода товара
 */
function clearIncomeForm() {
    document.getElementsByName('receiptNoteNumber')[0].value = '';
    document.getElementsByName('supplierName')[0].value = '';
    document.getElementById('incomeItemArticle').value = '';
    document.getElementById('incomeItemValue').value = '';
    document.getElementById('newCategory').value = '';
    document.getElementById('newMinVal').value = '';
    document.getElementById('newRecVal').value = '';

    var tbody = document.getElementById('incomeItemsBody');
    tbody.innerHTML = '<tr><td colspan="3" style="text-align: center;">Товары не добавлены</td></tr>';

    fetch('/storekeeper/dashboard?action=clear_income_items', {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
        .then(function(response) {
            if (response.ok) {
                console.log('Данные прихода очищены');
            }
        })
        .catch(function(err) {
            console.error('Ошибка при очистке:', err);
        });

    var errorDiv = document.getElementById('incomeItemError');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}