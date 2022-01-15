package com.hama.white.row.level;

import org.junit.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @description: RowLevelControlTest
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 8:49 PM
 */
public abstract class RowLevelControlTest {

    private Set<String> targetTables = new HashSet<String>(){{
        add("bas_api");
        add("bas_task");
    }};

    private Map<String, String> tableConditionMap = new HashMap<String, String>() {{
        put("bas_api", "api_id = 20160620");
        put("bas_task", "taskName = 'tmp_hive'");
    }};

    protected RowLevelControl rowLevelControl = RowLevelControl.builder()
            .tableNameSet(targetTables)
            .tableConditionMap(tableConditionMap)
            .build();



    protected abstract String addRowLevelControl(String sql);


    protected void run(String testName, String inputSql, String expectedSql) {
        System.out.println("----------------- " + testName + " -----------------------");

        String result = addRowLevelControl(inputSql);

        System.out.println("输入SQL: \n" + inputSql + "\n");
        System.out.println("结果SQL: \n" + result + "\n\n");


        Assert.assertEquals(expectedSql, result);
    }

    protected void doTest() {

        testSelectSql();

        testSelectSqlWithWhere();

        testSelectSqlWithGroupBy();

        testJoin();

        testJoinWithWhere();

        testJoinSubQuery();

        testJoinWithTwoTable();

        testUnionWithJoinSubQuery();

        testDeleteSql();

        testUpdateSql();
    }



    private void testSelectSql() {
        String testName = "测试点:单表查询";

        String inputSql = "SELECT\n" +
                "   *\n" +
                "FROM\n" +
                "   bas_api";

        String expectedSql = "SELECT *\n" +
                "FROM bas_api\n" +
                "WHERE api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }


    private void testSelectSqlWithWhere() {
        String testName = "测试点:单表查询带Where";

        String inputSql = "SELECT\n" +
                "   *\n" +
                "FROM\n" +
                "   bas_api\n" +
                "WHERE\n" +
                "   api_id IS NOT NULL\n" +
                "OR invalid = 1";


        String expectedSql = "SELECT *\n" +
                "FROM bas_api\n" +
                "WHERE (api_id IS NOT NULL\n" +
                "\t\tOR invalid = 1)\n" +
                "\tAND api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }


    private void testSelectSqlWithGroupBy() {
        String testName = "测试点:单表查询带GROUP BY";


        String inputSql = "SELECT\n" +
                "   id ,\n" +
                "   count(*) AS cnt\n" +
                "FROM\n" +
                "   bas_api\n" +
                "WHERE\n" +
                "   user_name = 'hello'\n" +
                "GROUP BY\n" +
                "   id";

        String expectedSql = "SELECT id, count(*) AS cnt\n" +
                "FROM bas_api\n" +
                "WHERE user_name = 'hello'\n" +
                "\tAND api_id = 20160620\n" +
                "GROUP BY id";

        run(testName, inputSql, expectedSql);
    }

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
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE ba.api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }

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
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE (ba.api_name = 'test'\n" +
                "\t\tOR ba.invalid = 1)\n" +
                "\tAND ba.api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }


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
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1";

        run(testName, inputSql, expectedSql);
    }


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
                "\tJOIN bas_task task ON api.api_id = task.task_id\n" +
                "WHERE api.api_name = 'test'\n" +
                "\tAND api.api_id = 20160620\n" +
                "\tAND task.taskName = 'tmp_hive'";

        run(testName, inputSql, expectedSql);
    }



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
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
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
                "\tJOIN rel_api_config rac ON ba.api_id = rac.api_id\n" +
                "WHERE ba.api_name = 'test'\n" +
                "\tOR ba.invalid = 1";

        run(testName, inputSql, expectedSql);
    }


    private void testDeleteSql() {
        String testName = "测试点:单表删除";

        String inputSql = "DELETE\n" +
                "FROM\n" +
                "   bas_api\n" +
                "WHERE\n" +
                "   user_name = 'hello'";

        String expectedSql = "DELETE FROM bas_api\n" +
                "WHERE user_name = 'hello'\n" +
                "\tAND api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }


    private void testUpdateSql() {
        String testName = "测试点:单表更新";

        String inputSql = "UPDATE bas_api\n" +
                "SET user_name = 'hello'\n" +
                "WHERE\n" +
                "   city = 'hangzhou'";

        String expectedSql = "UPDATE bas_api\n" +
                "SET user_name = 'hello'\n" +
                "WHERE city = 'hangzhou'\n" +
                "\tAND api_id = 20160620";

        run(testName, inputSql, expectedSql);
    }
}