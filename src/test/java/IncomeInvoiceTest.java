import com.wmsjenty.model.Item;
import com.wmsjenty.util.DBConnector;
import com.wmsjenty.util.DBDataLoader;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

public class IncomeInvoiceTest {
    private final int EXISTING_ITEM_ID = 1;
    private int originalValue = -1;
    private int generatedInvoiceId = -1;
    private int newCreatedItemId = -1;

    @BeforeEach
    void setUp() throws SQLException {
        generatedInvoiceId = -1;
        newCreatedItemId = -1;

        // Текущее состояние существующего товара
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT value FROM item WHERE id = ?")) {
            ps.setInt(1, EXISTING_ITEM_ID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) originalValue = rs.getInt("value");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DBConnector.getConnection()) {
            conn.setAutoCommit(false);

            // Удаление созданных записей в таблицах incoming_items и incoming_invoices
            if (generatedInvoiceId != -1) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM incoming_items WHERE income_invoice_id = ?")) {
                    ps.setInt(1, generatedInvoiceId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM incoming_invoices WHERE id = ?")) {
                    ps.setInt(1, generatedInvoiceId);
                    ps.executeUpdate();
                }
            }

            // Удаление товара
            if (newCreatedItemId != -1) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM item WHERE id = ?")) {
                    ps.setInt(1, newCreatedItemId);
                    ps.executeUpdate();
                }
            }

            // Возврат исходного количества товара
            if (originalValue != -1) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE item SET value = ? WHERE id = ?")) {
                    ps.setInt(1, originalValue);
                    ps.setInt(2, EXISTING_ITEM_ID);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        }
    }

    @Test
    @DisplayName("Позитивный тест добавления прихода товара: один существующий товар + один новый")
    void positiveProcessIncome() throws SQLException {
        HashMap<Item, Integer> existingItems = new HashMap<>();
        Item existingItem = new Item();
        existingItem.setId(EXISTING_ITEM_ID);
        existingItems.put(existingItem, 10);

        ArrayList<Item> newItems = new ArrayList<>();
        Item newItem = new Item();
        newItem.setArticle("testArticle");
        newItem.setName("Test Product");
        newItem.setBrand("TEST");
        newItem.setCategoryId(1);
        newItem.setMinValue(5);
        newItem.setRecommendedValue(10);
        newItem.setValue(100);
        newItems.add(newItem);

        generatedInvoiceId = DBDataLoader.processIncome("INV-999", "Test Supplier", existingItems, newItems, 1);

        assertNotEquals(-1, generatedInvoiceId, "Накладная должна быть создана");

        // Проверка существующего товара (+10)
        int updatedValue = getStockValue(EXISTING_ITEM_ID);
        assertEquals(originalValue + 10, updatedValue, "Остаток старого товара должен увеличиться на 10");

        // Проверка создания нового товара в базе
        newCreatedItemId = getItemIdByArticle("testArticle");
        assertNotEquals(-1, newCreatedItemId, "Новый товар должен быть найден в базе");
        assertEquals(100, getStockValue(newCreatedItemId), "Количество нового товара должно быть 100");
    }

    private int getStockValue(int id) throws SQLException {
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT value FROM item WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("value") : -1;
        }
    }

    private int getItemIdByArticle(String article) throws SQLException {
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM item WHERE article = ?")) {
            ps.setString(1, article);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }
}
