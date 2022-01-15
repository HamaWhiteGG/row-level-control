package com.hama.white.row.level;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description: QuickStart
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/16 12:28 AM
 */
public class QuickStart {

    public static void main(String[] args) {
        Set<String> targetTables = new HashSet<String>() {{
            add("bas_api");
            add("bas_task");
        }};

        Map<String, String> tableConditionMap = new HashMap<String, String>() {{
            put("bas_api", "api_id = 20160620");
            put("bas_task", "taskName = 'tmp_hive'");
        }};

        RowLevelControl rowLevelControl = RowLevelControl.builder()
                .tableNameSet(targetTables)
                .tableConditionMap(tableConditionMap)
                .build();

        String inputSql = "SELECT\n" +
                "   api.api_id ,\n" +
                "   task.task_code\n" +
                "FROM\n" +
                "   bas_api api\n" +
                "JOIN bas_task task ON api.api_id = task.task_id\n" +
                "WHERE\n" +
                "   api.api_name = 'test'";

        List<SQLStatement> stmtList = SQLUtils.parseStatements(inputSql, JdbcConstants.MYSQL);

        MysqlRowLevelControlVisitor visitor = new MysqlRowLevelControlVisitor(rowLevelControl);
        for (SQLStatement stmt : stmtList) {
            stmt.accept(visitor);
        }

        /**
         * 输出是:
         *
         * SELECT api.api_id, task.task_code
         * FROM bas_api api
         * 	JOIN bas_task task ON api.api_id = task.task_id
         * WHERE api.api_name = 'test'
         * 	AND api.api_id = 20160620
         * 	AND task.taskName = 'tmp_hive'
         */
        System.out.println(stmtList.get(0).toString());
    }
}
