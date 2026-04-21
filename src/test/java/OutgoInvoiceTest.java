import com.wmsjenty.model.Item;
import com.wmsjenty.util.DBConnector;
import com.wmsjenty.util.DBDataLoader;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class OutgoInvoiceTest {

    private final int TEST_USER_ID = 1;
    private final int TEST_ITEM_ID = 1;
    private int generatedInvoiceId = -1;
    private int originalQuantity = -1;

    @BeforeEach
    void setUp() throws SQLException {
        // Запоминание исходного количества товара на складе
        originalQuantity = getStockQuantity(TEST_ITEM_ID);
        generatedInvoiceId = -1;
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Возвращение БД в исходное состояние
        try (Connection conn = DBConnector.getConnection()) {
            conn.setAutoCommit(false);

            // Удаление накладной
            if (generatedInvoiceId != -1) {

                try (PreparedStatement psItems = conn.prepareStatement("DELETE FROM outgoing_items WHERE outgo_invoice_id = ?")) {
                    psItems.setInt(1, generatedInvoiceId);
                    psItems.executeUpdate();
                }
                try (PreparedStatement psInv = conn.prepareStatement("DELETE FROM outgoing_invoices WHERE id = ?")) {
                    psInv.setInt(1, generatedInvoiceId);
                    psInv.executeUpdate();
                }
            }

            // Возвращение количества товара в исходное состояние
            if (originalQuantity != -1) {
                try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE item SET value = ? WHERE id = ?")) {
                    psUpdate.setInt(1, originalQuantity);
                    psUpdate.setInt(2, TEST_ITEM_ID);
                    psUpdate.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    @Test
    @DisplayName("Позитивный тест оформления расхода товара: списание 5 единиц товара со склада")
    void positiveSaveFullInvoiceMethod() throws SQLException {
        // Подготовка данных для списания 5 единиц
        HashMap<Item, Integer> itemsToSpend = new HashMap<>();
        Item item = new Item();
        item.setId(TEST_ITEM_ID);
        item.setArticle("140093");
        itemsToSpend.put(item, 5);

        generatedInvoiceId = DBDataLoader.processOutgo("Test Client", "TestNumber", TEST_USER_ID, itemsToSpend);

        assertNotEquals(-1, generatedInvoiceId, "Накладная должна быть успешно создана");

        int currentQty = getStockQuantity(TEST_ITEM_ID);
        assertEquals(originalQuantity - 5, currentQty, "Остаток на складе должен уменьшиться ровно на 5 ед.");

    }

    @Test
    @DisplayName("Негативный тест оформления расхода товара: попытка списания большего количества товара, чем есть на складе")
    void negativeSaveFullInvoiceMethod() throws SQLException {
        int currentStock = getStockQuantity(TEST_ITEM_ID);
        int tooMuchQty = currentStock + 100;

        HashMap<Item, Integer> itemsToSpend = new HashMap<>();
        Item item = new Item();
        item.setId(TEST_ITEM_ID);
        itemsToSpend.put(item, tooMuchQty);

        generatedInvoiceId = DBDataLoader.processOutgo("Test Client", "TestNumber", TEST_USER_ID, itemsToSpend);
        assertTrue(generatedInvoiceId == -1 || getStockQuantity(TEST_ITEM_ID) == currentStock,
                "Операция должна завершиться ошибкой или откатиться, если товара недостаточно");
    }
    private int getStockQuantity(int itemId) throws SQLException {
        String sql = "SELECT value FROM item WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("value") : -1;
        }
    }
}
