package com.hama.white.row.level;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGDeleteStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGSelectQueryBlock;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGUpdateStatement;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGASTVisitorAdapter;
import com.alibaba.druid.util.JdbcConstants;

/**
 * @description: PGRowLevelControlVisitor
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 7:27 PM
 */
public class PGRowLevelControlVisitor extends PGASTVisitorAdapter {

    /**
     * 数据库类型
     */
    private final String dbType = JdbcConstants.POSTGRESQL;

    private RowLevelControl rowLevelControl;


    public PGRowLevelControlVisitor(RowLevelControl rowLevelControl) {
        this.rowLevelControl = rowLevelControl;
    }


    @Override
    public boolean visit(PGUpdateStatement updateStatement) {
        return rowLevelControl.visit(updateStatement,dbType);
    }

    @Override
    public boolean visit(PGDeleteStatement deleteStatement) {
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
            while (!(parent instanceof PGSelectQueryBlock) && parent != null) {
                parent = parent.getParent();
            }
            // 插入行控制条件
            if (parent != null) {
                ((PGSelectQueryBlock) parent).addCondition(rowLevelControl.formCondition(tableName,alias));
            }
        }
        return false;
    }
}
