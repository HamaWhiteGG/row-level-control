package com.hama.white.row.level;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.util.JdbcConstants;

/**
 * @description: MysqlRowLevelControlVisitor
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 7:27 PM
 */
public class MysqlRowLevelControlVisitor extends MySqlASTVisitorAdapter {

    /**
     * 数据库类型
     */
    private final String dbType = JdbcConstants.MYSQL;

    private RowLevelControl rowLevelControl;


    public MysqlRowLevelControlVisitor(RowLevelControl rowLevelControl) {
        this.rowLevelControl = rowLevelControl;
    }


    @Override
    public boolean visit(MySqlUpdateStatement updateStatement) {
        return rowLevelControl.visit(updateStatement,dbType);
    }

    @Override
    public boolean visit(MySqlDeleteStatement deleteStatement) {
        return rowLevelControl.visit(deleteStatement,dbType);
    }

    /**
     * 出口状态
     *
     * @return boolean
     */
    @Override
    public boolean visit(SQLExprTableSource reference) {
        String tableName=reference.getName().getSimpleName();
        // 符合目标表源名
        if (rowLevelControl.isTargetTable(tableName)) {
            SQLObject parent = reference.getParent();
            String alias = reference.getAlias();
            // 回溯到选择语句
            while (!(parent instanceof MySqlSelectQueryBlock) && parent != null) {
                parent = parent.getParent();
            }
            // 插入行控制条件
            if (parent != null) {
                ((MySqlSelectQueryBlock) parent).addCondition(rowLevelControl.formCondition(tableName,alias));
            }
        }
        return false;
    }
}
