package io.sniffy.sql;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

import static io.sniffy.sql.SqlUtil.normalizeInStatement;
import static org.junit.Assert.assertEquals;

public class SqlUtilTest {

    @Test
    @Features("issues/292")
    public void testNormalizeInStatement() {
        assertEquals(
                "select orders0_.customer_id as customer4_1_, orders0_.id as id1_, orders0_.id as id1_0_, orders0_.customer_id as customer4_1_0_, orders0_.description as descript2_1_0_, orders0_.orderId as orderId1_0_ from ORDERS orders0_ where orders0_.customer_id in (?)",
                normalizeInStatement("select orders0_.customer_id as customer4_1_, orders0_.id as id1_, orders0_.id as id1_0_, orders0_.customer_id as customer4_1_0_, orders0_.description as descript2_1_0_, orders0_.orderId as orderId1_0_ from ORDERS orders0_ where orders0_.customer_id in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        );
    }

    @Test
    @Features("issues/292")
    public void testNormalizeTwoInStatements() {
        assertEquals(
                "select orders0_.customer_id as customer4_1_, orders0_.id as id1_, orders0_.id as id1_0_, orders0_.customer_id as customer4_1_0_, orders0_.description as descript2_1_0_, orders0_.orderId as orderId1_0_ from ORDERS orders0_ where orders0_.customer_id in (?) or orders0_.customer_id in (?)",
                normalizeInStatement("select orders0_.customer_id as customer4_1_, orders0_.id as id1_, orders0_.id as id1_0_, orders0_.customer_id as customer4_1_0_, orders0_.description as descript2_1_0_, orders0_.orderId as orderId1_0_ from ORDERS orders0_ where orders0_.customer_id in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) or orders0_.customer_id in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        );
    }

}
