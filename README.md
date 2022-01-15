 # 基于Alibaba Druid 做行级数据权限控制



### 一、 实现思路

SQL 抽象语法树遍历，在限制访问表的 SQL 对应层级添加 `WHERE` 权限条件，通过自定义Visitor来重写Druid ASTVisitorAdapter中的visit方式来实现。

### 二、功能介绍

#### 2.1 支持的数据类型

- Mysql
- Oracle
- PostgreSQL

#### 2.2 支持的SQL语法

- 查询语句 select
- 删除语句 delete ...
- 更新语句 update ...

### 三、快速开始

#### 3.1 QuickStart

运行代码中的com.hama.white.row.level.QuickStart

```java
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
```

#### 3.2 测试

##### 3.2.1单独测试

- MysqlRowLevelControlVisitorTest

- OracleRowLevelControlVisitorTest

- PGRowLevelControlVisitorTest

##### 3.2.2 整体测试

直接运行测试套件SuiteTest

### 四、核心代码

以MysqlRowLevelControlVisitor中的select语句为例，继承MySqlASTVisitorAdapter，只需要重写 boolean visit(SQLExprTableSource reference) 方法即可，只要大约20行左右代码便可实现select的行级权限控制。

```java
public class MysqlRowLevelControlVisitor extends MySqlASTVisitorAdapter {

    /**
     * 数据库类型
     */
    private final String dbType = JdbcConstants.MYSQL;

    private RowLevelControl rowLevelControl;

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

```

### 五、完整测试报告
#### 5.1 Mysql 测试报告

```
----------------- 测试点:单表查询 -----------------------
输入SQL: 
SELECT
   *
FROM
   bas_api

结果SQL: 
SELECT *
FROM bas_api
WHERE api_id = 20160620


----------------- 测试点:单表查询带Where -----------------------
输入SQL: 
SELECT
   *
FROM
   bas_api
WHERE
   api_id IS NOT NULL
OR invalid = 1

结果SQL: 
SELECT *
FROM bas_api
WHERE (api_id IS NOT NULL
		OR invalid = 1)
	AND api_id = 20160620


----------------- 测试点:单表查询带GROUP BY -----------------------
输入SQL: 
SELECT
   id ,
   count(*) AS cnt
FROM
   bas_api
WHERE
   user_name = 'hello'
GROUP BY
   id

结果SQL: 
SELECT id, count(*) AS cnt
FROM bas_api
WHERE user_name = 'hello'
	AND api_id = 20160620
GROUP BY id


----------------- 测试点: Join -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   bas_api AS ba
JOIN rel_api_config rac
ON ba.api_id = rac.api_id

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM bas_api ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_id = 20160620


----------------- 测试点: Join带Where -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   bas_api ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM bas_api ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE (ba.api_name = 'test'
		OR ba.invalid = 1)
	AND ba.api_id = 20160620


----------------- 测试点: Join带子查询 -----------------------
输入SQL: 
SELECT    ba.api_id ,   rac.config_value
FROM
   (
       SELECT
           *
       FROM
           bas_api
       WHERE
           invalid = 1
   ) ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM (
	SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_name = 'test'
	OR ba.invalid = 1


----------------- 测试点: Join 限制两张表的行级权限 -----------------------
输入SQL: 
SELECT
   api.api_id ,
   task.task_code
FROM
   bas_api api
JOIN bas_task task ON api.api_id = task.task_id
WHERE
   api.api_name = 'test'

结果SQL: 
SELECT api.api_id, task.task_code
FROM bas_api api
	JOIN bas_task task ON api.api_id = task.task_id
WHERE api.api_name = 'test'
	AND api.api_id = 20160620
	AND task.taskName = 'tmp_hive'


----------------- 测试点: Union混合Join带子查询 -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   (
       SELECT
           *
       FROM
           bas_api
       WHERE
           invalid = 1
   ) ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1
UNION
   SELECT
       ba.api_id ,
       rac.config_value
   FROM
       (
           SELECT
               *
           FROM
               bas_api
           WHERE
               invalid = 1
       ) ba
   JOIN rel_api_config rac ON ba.api_id = rac.api_id
   WHERE
       ba.api_name = 'test'
   OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM (
	SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_name = 'test'
	OR ba.invalid = 1
UNION
SELECT ba.api_id, rac.config_value
FROM (
	SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_name = 'test'
	OR ba.invalid = 1


----------------- 测试点:单表删除 -----------------------
输入SQL: 
DELETE
FROM
   bas_api
WHERE
   user_name = 'hello'

结果SQL: 
DELETE FROM bas_api
WHERE user_name = 'hello'
	AND api_id = 20160620


----------------- 测试点:单表更新 -----------------------
输入SQL: 
UPDATE bas_api
SET user_name = 'hello'
WHERE
   city = 'hangzhou'

结果SQL: 
UPDATE bas_api
SET user_name = 'hello'
WHERE city = 'hangzhou'
	AND api_id = 20160620
```

#### 5.2 Oracle 测试报告

```
----------------- 测试点:单表查询 -----------------------
输入SQL: 
SELECT
   *
FROM
   bas_api

结果SQL: 
SELECT *
FROM bas_api
WHERE api_id = 20160620


----------------- 测试点:单表查询带Where -----------------------
输入SQL: 
SELECT
   *
FROM
   bas_api
WHERE
   api_id IS NOT NULL
OR invalid = 1

结果SQL: 
SELECT *
FROM bas_api
WHERE (api_id IS NOT NULL
		OR invalid = 1)
	AND api_id = 20160620


----------------- 测试点:单表查询带GROUP BY -----------------------
输入SQL: 
SELECT
   id ,
   count(*) AS cnt
FROM
   bas_api
WHERE
   user_name = 'hello'
GROUP BY
   id

结果SQL: 
SELECT id, count(*) AS cnt
FROM bas_api
WHERE user_name = 'hello'
	AND api_id = 20160620
GROUP BY id


----------------- 测试点: Join -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   bas_api AS ba
JOIN rel_api_config rac
ON ba.api_id = rac.api_id

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM bas_api ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id 
WHERE ba.api_id = 20160620


----------------- 测试点: Join带Where -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   bas_api ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM bas_api ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id 
WHERE (ba.api_name = 'test'
		OR ba.invalid = 1)
	AND ba.api_id = 20160620


----------------- 测试点: Join带子查询 -----------------------
输入SQL: 
SELECT    ba.api_id ,   rac.config_value
FROM
   (
       SELECT
           *
       FROM
           bas_api
       WHERE
           invalid = 1
   ) ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM (
	SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id 
WHERE ba.api_name = 'test'
	OR ba.invalid = 1


----------------- 测试点: Join 限制两张表的行级权限 -----------------------
输入SQL: 
SELECT
   api.api_id ,
   task.task_code
FROM
   bas_api api
JOIN bas_task task ON api.api_id = task.task_id
WHERE
   api.api_name = 'test'

结果SQL: 
SELECT api.api_id, task.task_code
FROM bas_api api
	JOIN bas_task task ON api.api_id = task.task_id 
WHERE api.api_name = 'test'
	AND api.api_id = 20160620
	AND task.taskName = 'tmp_hive'


----------------- 测试点: Union混合Join带子查询 -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   (
       SELECT
           *
       FROM
           bas_api
       WHERE
           invalid = 1
   ) ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1
UNION
   SELECT
       ba.api_id ,
       rac.config_value
   FROM
       (
           SELECT
               *
           FROM
               bas_api
           WHERE
               invalid = 1
       ) ba
   JOIN rel_api_config rac ON ba.api_id = rac.api_id
   WHERE
       ba.api_name = 'test'
   OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM (
	SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id 
WHERE ba.api_name = 'test'
	OR ba.invalid = 1
UNION
SELECT ba.api_id, rac.config_value
FROM (
	SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id 
WHERE ba.api_name = 'test'
	OR ba.invalid = 1


----------------- 测试点:单表删除 -----------------------
输入SQL: 
DELETE
FROM
   bas_api
WHERE
   user_name = 'hello'

结果SQL: 
DELETE FROM bas_api
WHERE user_name = 'hello'
	AND api_id = 20160620


----------------- 测试点:单表更新 -----------------------
输入SQL: 
UPDATE bas_api
SET user_name = 'hello'
WHERE
   city = 'hangzhou'

结果SQL: 
UPDATE bas_api
SET user_name = 'hello'
WHERE city = 'hangzhou'
	AND api_id = 20160620
```

#### 5.3 PostgreSQL 测试报告

```
----------------- 测试点:单表查询 -----------------------
输入SQL: 
SELECT
   *
FROM
   bas_api

结果SQL: 
SELECT *
FROM bas_api
WHERE api_id = 20160620


----------------- 测试点:单表查询带Where -----------------------
输入SQL: 
SELECT
   *
FROM
   bas_api
WHERE
   api_id IS NOT NULL
OR invalid = 1

结果SQL: 
SELECT *
FROM bas_api
WHERE (api_id IS NOT NULL
		OR invalid = 1)
	AND api_id = 20160620


----------------- 测试点:单表查询带GROUP BY -----------------------
输入SQL: 
SELECT
   id ,
   count(*) AS cnt
FROM
   bas_api
WHERE
   user_name = 'hello'
GROUP BY
   id

结果SQL: 
SELECT id, count(*) AS cnt
FROM bas_api
WHERE user_name = 'hello'
	AND api_id = 20160620
GROUP BY id


----------------- 测试点: Join -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   bas_api AS ba
JOIN rel_api_config rac
ON ba.api_id = rac.api_id

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM bas_api ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_id = 20160620


----------------- 测试点: Join带Where -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   bas_api ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM bas_api ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE (ba.api_name = 'test'
		OR ba.invalid = 1)
	AND ba.api_id = 20160620


----------------- 测试点: Join带子查询 -----------------------
输入SQL: 
SELECT    ba.api_id ,   rac.config_value
FROM
   (
       SELECT
           *
       FROM
           bas_api
       WHERE
           invalid = 1
   ) ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM (
	(SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620)
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_name = 'test'
	OR ba.invalid = 1


----------------- 测试点: Join 限制两张表的行级权限 -----------------------
输入SQL: 
SELECT
   api.api_id ,
   task.task_code
FROM
   bas_api api
JOIN bas_task task ON api.api_id = task.task_id
WHERE
   api.api_name = 'test'

结果SQL: 
SELECT api.api_id, task.task_code
FROM bas_api api
	JOIN bas_task task ON api.api_id = task.task_id
WHERE api.api_name = 'test'
	AND api.api_id = 20160620
	AND task.taskName = 'tmp_hive'


----------------- 测试点: Union混合Join带子查询 -----------------------
输入SQL: 
SELECT
   ba.api_id ,
   rac.config_value
FROM
   (
       SELECT
           *
       FROM
           bas_api
       WHERE
           invalid = 1
   ) ba
JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE
   ba.api_name = 'test'
OR ba.invalid = 1
UNION
   SELECT
       ba.api_id ,
       rac.config_value
   FROM
       (
           SELECT
               *
           FROM
               bas_api
           WHERE
               invalid = 1
       ) ba
   JOIN rel_api_config rac ON ba.api_id = rac.api_id
   WHERE
       ba.api_name = 'test'
   OR ba.invalid = 1

结果SQL: 
SELECT ba.api_id, rac.config_value
FROM (
	(SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620)
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_name = 'test'
	OR ba.invalid = 1
UNION
SELECT ba.api_id, rac.config_value
FROM (
	(SELECT *
	FROM bas_api
	WHERE invalid = 1
		AND api_id = 20160620)
) ba
	JOIN rel_api_config rac ON ba.api_id = rac.api_id
WHERE ba.api_name = 'test'
	OR ba.invalid = 1


----------------- 测试点:单表删除 -----------------------
输入SQL: 
DELETE
FROM
   bas_api
WHERE
   user_name = 'hello'

结果SQL: 
DELETE FROM bas_api
WHERE user_name = 'hello'
	AND api_id = 20160620


----------------- 测试点:单表更新 -----------------------
输入SQL: 
UPDATE bas_api
SET user_name = 'hello'
WHERE
   city = 'hangzhou'

结果SQL: 
UPDATE bas_api
SET user_name = 'hello'
WHERE city = 'hangzhou'
	AND api_id = 20160620
```





 

