
    function hideAllSections() {
    var sections = ['successMessage', 'categoryListSection', 'addCategorySection',
    'deleteCategorySection', 'usersListSection', 'addUserSection', 'deleteUserSection', 'logSelectSection',
    'logResults', 'adjustmentSection', 'searchSection', 'searchResultsSection'];
    sections.forEach(function(id) {
    var el = document.getElementById(id);
    if (el) el.style.display = 'none';
});
}

    // функция обработки нажатия на кнопки
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

    // получить данные о товаре
    fetch(CONTEXT_PATH + '/admin/dashboard?action=getItemInfo&itemId=' + itemId)
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
