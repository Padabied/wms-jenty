
    function hideAllSections() {
    var sections = ['successMessage', 'logSelectSection',
    'logResults', 'searchSection', 'searchResultsSection', 'forecastSection', 'inventorySection',
    'addOutgoSection', 'outgoLogSelect', 'outgoLogResults', 'addIncomeSection'];
    sections.forEach(function(id) {
    var el = document.getElementById(id);
    if (el) el.style.display = 'none';
});
}

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

    <!-- добавление товара в список в расходной накладной -->
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

    <!-- обновление списка добавленных товаров в расходной накладной -->
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

    <!-- очистка формы и внутреннего списка сервлета после закрытия формы расхода-->
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

    <!-- отправка данных для оформления расхода -->
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

    <!-- добавление товара в список приходной накладной -->
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

    <!-- обновление списка добавленных товаров в приходной накладной -->
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

    <!-- сохранение нового товара в список приходной накладной-->
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

    function closeModal() {
    document.getElementById('newItemModal').style.display = 'none';
    document.getElementById('modalOverlay').style.display = 'none';
    // Очистить поля модалки
    document.getElementById('newName').value = '';
    document.getElementById('newBrand').value = '';
}

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


    function handleButtonClick(action) {

    hideAllSections();
    if (action !== 'outgo_add') {
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