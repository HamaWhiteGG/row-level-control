package com.hama.white.row.level;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import org.junit.Test;

import java.util.List;

/**
 * @description: MysqlRowLevelControlVisitorTest
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 11:26 PM
 */
public class MysqlRowLevelControlVisitorTest extends RowLevelControlTest {

    @Override
    protected String addRowLevelControl(String sql) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        if (!stmtList.isEmpty()) {
            MysqlRowLevelControlVisitor visitor = new MysqlRowLevelControlVisitor(rowLevelControl);
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
}