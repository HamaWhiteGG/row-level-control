package com.hama.white.row.level;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import org.junit.Test;

import java.util.List;

/**
 * @description: PGRowLevelControlVisitorTest
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 11:26 PM
 */
public class PGRowLevelControlVisitorTest extends RowLevelControlTest {

    @Override
    protected String addRowLevelControl(String sql) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL);

        if (!stmtList.isEmpty()) {
            PGRowLevelControlVisitor visitor = new PGRowLevelControlVisitor(rowLevelControl);
            for (SQLStatement stmt : stmtList) {
                stmt.accept(visitor);
            }
            return stmtList.get(0).toString();
        }
        return null;
    }

    @Test
    public void testRowLevelControl() {
        doTest();
    }

    /**
     * expectedSql: 子查询增加()
     */
    @Override
    public void testJoinSubQuery() {
        String testName = "测试点: Join带子查询";

        String inputSql = "SELECT " +
                "   ba.api_id ," +
                "   rac.config_value\n" +
                "FROM\n" +
                "   (\n" +
                "       SELECT\n" +
                "           *\n" +
                "       FROM\n" +
                "           bas_api\n" +
                "       WHERE\n" +
                "           invalid = 1\n" +
                "   ) ba\n" +
                "JOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE\n" +
                "   ba.api_name = 'test'\n" +
                "OR ba.invalid = 1";

        String expectedSql = "SELECT ba.api_id, rac.config_value\n" +
                "FROM (\n" +
                "\t(SELECT *\n" +
                "\tFROM bas_api\n" +
                "\tWHERE invalid = 1\n" +
                "\t\tAND api_id = 20160620)\n" +
                ") ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1";

        run(testName, inputSql, expectedSql);
    }


    /**
     * expectedSql: 子查询增加()
     */
    @Override
    public void testUnionWithJoinSubQuery() {
        String testName = "测试点: Union混合Join带子查询";

        String inputSql = "SELECT\n" +
                "   ba.api_id ,\n" +
                "   rac.config_value\n" +
                "FROM\n" +
                "   (\n" +
                "       SELECT\n" +
                "           *\n" +
                "       FROM\n" +
                "           bas_api\n" +
                "       WHERE\n" +
                "           invalid = 1\n" +
                "   ) ba\n" +
                "JOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE\n" +
                "   ba.api_name = 'test'\n" +
                "OR ba.invalid = 1\n" +
                "UNION\n" +
                "   SELECT\n" +
                "       ba.api_id ,\n" +
                "       rac.config_value\n" +
                "   FROM\n" +
                "       (\n" +
                "           SELECT\n" +
                "               *\n" +
                "           FROM\n" +
                "               bas_api\n" +
                "           WHERE\n" +
                "               invalid = 1\n" +
                "       ) ba\n" +
                "   JOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "   WHERE\n" +
                "       ba.api_name = 'test'\n" +
                "   OR ba.invalid = 1";

        String expectedSql = "SELECT ba.api_id, rac.config_value\n" +
                "FROM (\n" +
                "\t(SELECT *\n" +
                "\tFROM bas_api\n" +
                "\tWHERE invalid = 1\n" +
                "\t\tAND api_id = 20160620)\n" +
                ") ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1\n" +
                "UNION\n" +
                "SELECT ba.api_id, rac.config_value\n" +
                "FROM (\n" +
                "\t(SELECT *\n" +
                "\tFROM bas_api\n" +
                "\tWHERE invalid = 1\n" +
                "\t\tAND api_id = 20160620)\n" +
                ") ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1";

        run(testName, inputSql, expectedSql);
    }
}