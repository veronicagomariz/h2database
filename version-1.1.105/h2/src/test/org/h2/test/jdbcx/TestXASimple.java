/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.jdbcx;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.test.TestBase;

/**
 * A simple XA test.
 */
public class TestXASimple extends TestBase {

    public void test() throws SQLException {

        deleteDb("xaSimple1");
        deleteDb("xaSimple2");
        org.h2.Driver.load();

        // InitialContext context = new InitialContext();
        // context.rebind(USER_TRANSACTION_JNDI_NAME, j.getUserTransaction());

        JdbcDataSource ds1 = new JdbcDataSource();
        ds1.setPassword("");
        ds1.setUser("sa");
        ds1.setURL("jdbc:h2:" + baseDir + "/xaSimple1");

        JdbcDataSource ds2 = new JdbcDataSource();
        ds2.setPassword("");
        ds2.setUser("sa");
        ds2.setURL("jdbc:h2:" + baseDir + "/xaSimple2");

        // UserTransaction ut = (UserTransaction)
        // context.lookup("UserTransaction");
        // ut.begin();

        XAConnection xa1 = ds1.getXAConnection();
        Connection c1 = xa1.getConnection();
        c1.setAutoCommit(false);
        XAConnection xa2 = ds2.getXAConnection();
        Connection c2 = xa2.getConnection();
        c2.setAutoCommit(false);

        c1.createStatement().executeUpdate("create table test(id int, test varchar(255))");
        c2.createStatement().executeUpdate("create table test(id int, test varchar(255))");

        // ut.rollback();
        c1.close();
        c2.close();

        xa1.close();
        xa2.close();

        // j.stop();
        // System.exit(0);
        deleteDb("xaSimple1");
        deleteDb("xaSimple2");

    }
}
