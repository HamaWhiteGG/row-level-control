package com.hama.white.row.level;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @description: SuiteTest
 * @author: baisong
 * @version: 1.0.0
 * @date: 2022/1/15 11:50 PM
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({MysqlRowLevelControlVisitorTest.class
        , OracleRowLevelControlVisitorTest.class
        , PGRowLevelControlVisitorTest.class})
public class SuiteTest {

    /**
     * The entry class of the test suite is just to organize the test classes together for testing,
     * without any test methods.
     */
}
