package com.hama.white.row.level;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleDeleteStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectQueryBlock;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectTableReference;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleASTVisitorAdapter;
import com.alibaba.druid.util.JdbcConstants;

/**
 * @description: OracleRowLevelControlVisitor
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 7:27 PM
 */
public class OracleRowLevelControlVisitor extends OracleASTVisitorAdapter {

    /**
     * 数据库类型
     */
    private final String dbType = JdbcConstants.ORACLE;

    private RowLevelControl rowLevelControl;


    public OracleRowLevelControlVisitor(RowLevelControl rowLevelControl) {
        this.rowLevelControl = rowLevelControl;
    }


    @Override
    public boolean visit(OracleUpdateStatement updateStatement) {
       return rowLevelControl.visit(updateStatement,dbType);
    }

    @Override
    public boolean visit(OracleDeleteStatement deleteStatement) {
        return rowLevelControl.visit(deleteStatement,dbType);
    }

    /**
     * 出口状态
     *
     * @return boolean
     */
    @Override
    public boolean visit(OracleSelectTableReference reference) {
        String tableName = reference.getName().getSimpleName();
        // 符合目标表源名
        if (rowLevelControl.isTargetTable(tableName)) {
            SQLObject parent = reference.getParent();
            String alias = reference.getAlias();
            // 回溯到选择语句
            while (!(parent instanceof OracleSelectQueryBlock) && parent != null) {
                parent = parent.getParent();
            }
            // 插入行控制条件
            if (parent != null) {
                ((OracleSelectQueryBlock) parent).addCondition(rowLevelControl.formCondition(tableName, alias));
            }
        }
        return false;
    }
}
