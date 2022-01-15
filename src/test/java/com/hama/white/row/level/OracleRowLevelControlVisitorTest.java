package com.hama.white.row.level;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import org.junit.Test;

import java.util.List;

/**
 * @description: OracleRowLevelControlVisitorTest
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 8:47 PM
 */
public class OracleRowLevelControlVisitorTest extends RowLevelControlTest {


    @Override
    protected String addRowLevelControl(String sql) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, JdbcConstants.ORACLE);

        if (!stmtList.isEmpty()) {
            OracleRowLevelControlVisitor visitor = new OracleRowLevelControlVisitor(rowLevelControl);
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
     * expectedSql: JOIN rel_api_config rac ON ba.api_id = rac.api_id 后面增加一个空格
     */
    @Override
    public void testJoin() {
        String testName = "测试点: Join";

        String inputSql = "SELECT\n" +
                "   ba.api_id ,\n" +
                "   rac.config_value\n" +
                "FROM\n" +
                "   bas_api AS ba\n" +
                "JOIN rel_api_config rac\n" +
                "ON ba.api_id = rac.api_id";

        String expectedSql = "SELECT ba.api_id, rac.config_value\n" +
                "FROM bas_api ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id \n" +
                "WHERE ba.api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }

    /**
     * expectedSql: JOIN rel_api_config rac ON ba.api_id = rac.api_id 后面增加一个空格
     */
    @Override
    public void testJoinWithWhere() {
        String testName = "测试点: Join带Where";

        String inputSql = "SELECT\n" +
                "   ba.api_id ,\n" +
                "   rac.config_value\n" +
                "FROM\n" +
                "   bas_api ba\n" +
                "JOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE\n" +
                "   ba.api_name = 'test'\n" +
                "OR ba.invalid = 1";

        String expectedSql = "SELECT ba.api_id, rac.config_value\n" +
                "FROM bas_api ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id \n" +
                "WHERE (ba.api_name = 'test'\n" +
                "\t\tOR ba.invalid = 1)\n" +
                "\tAND ba.api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }


    /**
     * expectedSql: JOIN rel_api_config rac ON ba.api_id = rac.api_id 后面增加一个空格
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
                "\tSELECT *\n" +
                "\tFROM bas_api\n" +
                "\tWHERE invalid = 1\n" +
                "\t\tAND api_id = 20160620\n" +
                ") ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id \n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1";

        run(testName, inputSql, expectedSql);
    }

    /**
     * expectedSql: JJOIN bas_task task ON api.api_id = task.task_id 后面增加一个空格
     */
    @Override
    public void testJoinWithTwoTable() {
        String testName = "测试点: Join 限制两张表的行级权限";

        String inputSql = "SELECT\n" +
                "   api.api_id ,\n" +
                "   task.task_code\n" +
                "FROM\n" +
                "   bas_api api\n" +
                "JOIN bas_task task ON api.api_id = task.task_id\n" +
                "WHERE\n" +
                "   api.api_name = 'test'";

        String expectedSql = "SELECT api.api_id, task.task_code\n" +
                "FROM bas_api api\n" +
                "\tJOIN bas_task task ON api.api_id = task.task_id \n" +
                "WHERE api.api_name = 'test'\n" +
                "\tAND api.api_id = 20160620\n" +
                "\tAND task.taskName = 'tmp_hive'";

        run(testName, inputSql, expectedSql);
    }



    /**
     * expectedSql: JOIN rel_api_config rac ON ba.api_id = rac.api_id 后面增加一个空格
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
                "\tSELECT *\n" +
                "\tFROM bas_api\n" +
                "\tWHERE invalid = 1\n" +
                "\t\tAND api_id = 20160620\n" +
                ") ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id \n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1\n" +
                "UNION\n" +
                "SELECT ba.api_id, rac.config_value\n" +
                "FROM (\n" +
                "\tSELECT *\n" +
                "\tFROM bas_api\n" +
                "\tWHERE invalid = 1\n" +
                "\t\tAND api_id = 20160620\n" +
                ") ba\n" +
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id \n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1";

        run(testName, inputSql, expectedSql);
    }


}