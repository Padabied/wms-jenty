import com.wmsjenty.model.Item;
import com.wmsjenty.util.DBDataLoader;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class CheckItemAvailableTest {


    @Test
    @DisplayName("Позитивный тест проверки наличия: товар существует в нужном количества")
    void positiveCheckItemAvailable() {
        Item result = DBDataLoader.checkItemAvailable("140093", 5);

        assertNotNull(result, "При успешном прохождении теста должен вернуться Item");
        assertEquals("140093", result.getArticle());
        assertTrue(result.getValue() >= 5, "Количество товара должно быть >= запрошенного");
    }

    @Test
    @DisplayName("Негативный тест проверки наличия: артикул не существует")
    void wrongArticleCheckItemAvailable() {
        Item result = DBDataLoader.checkItemAvailable("111111", 5);
        assertNull(result, "Должен вернуться null для несуществующего артикула");
    }

    @Test
    @DisplayName("Негативный тест проверки наличия: недостаточное количество товара")
    void wrongValueCheckItemAvailable() {
        Item result = DBDataLoader.checkItemAvailable("140093", 100);
        assertNull(result, "Должен вернуться null, если на складе меньше товара, чем запрашивается");
    }
}
