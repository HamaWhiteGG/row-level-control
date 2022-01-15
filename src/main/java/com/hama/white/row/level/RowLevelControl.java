package com.hama.white.row.level;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @description: RowLevelControl
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 8:20 PM
 */

@Builder
@AllArgsConstructor
public class RowLevelControl {

    /**
     * 限制访问的表名
     */
    private Set<String> tableNameSet;


    /**
     * 每个表的约束条件
     */
    private Map<String, String> tableConditionMap;


    /**
     * 判断是否与目标表名一致
     *
     * @param tableName ...
     * @return boolean
     */
    public boolean isTargetTable(String tableName) {
        return tableNameSet.contains(tableName);
    }

    /**
     * 组成条件
     *
     * @param tableName ...
     * @param alias     ...
     * @return ...
     */
    public String formCondition(String tableName, String alias) {
        String condition = tableConditionMap.get(tableName);
        return alias == null ? condition : alias + "." + condition;
    }


    public boolean visit(SQLUpdateStatement updateStatement, String dbType) {
        SQLTableSource tableSource = updateStatement.getTableSource();
        if (tableSource != null) {
            String alias = tableSource.getAlias();
            if (tableSource instanceof SQLExprTableSource) {
                String tableName = ((SQLExprTableSource) tableSource).getName().getSimpleName();
                if (isTargetTable(tableName)) {
                    updateStatement.addCondition(SQLUtils.toSQLExpr(formCondition(tableName, alias), dbType));
                }
            }
        }
        return true;
    }

    public boolean visit(SQLDeleteStatement deleteStatement, String dbType) {
        SQLTableSource tableSource = deleteStatement.getTableSource();
        if (tableSource != null) {
            String alias = tableSource.getAlias();
            if (tableSource instanceof SQLExprTableSource) {
                String tableName = ((SQLExprTableSource) tableSource).getName().getSimpleName();
                if (isTargetTable(tableName)) {
                    SQLExpr sqlExpr = SQLUtils.toSQLExpr(formCondition(tableName, alias), dbType);
                    deleteStatement.addCondition(sqlExpr);
                }
            }
        }
        return true;
    }
}
