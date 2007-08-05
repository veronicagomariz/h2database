/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.bench;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import org.h2.test.TestBase;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;

public class TestPerformance {
    
    boolean collect;
    boolean log;
    
    public static void main(String[] args) throws Exception {
        new TestPerformance().test(args);
    }
    
    private Connection getResultConnection() throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:data/results");
    }
    
    private void openResults(boolean init) throws Exception {
        Connection conn = null;
        Statement stat = null;
        try {
            conn = getResultConnection();
            stat = conn.createStatement();
            if(init) {
                stat.execute("DROP TABLE IF EXISTS RESULTS");
            }
            stat.execute("CREATE TABLE IF NOT EXISTS RESULTS(TESTID INT, TEST VARCHAR, "
                    + "UNIT VARCHAR, DBID INT, DB VARCHAR, RESULT VARCHAR)");
        } finally {
            JdbcUtils.closeSilently(stat);
            JdbcUtils.closeSilently(conn);
        }
    }

    private void test(String[] args) throws Exception {
        boolean init = false;
        int dbId = -1;
        String out = "benchmark.html";
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-db")) {
                dbId = Integer.parseInt(args[++i]); 
            } else if(args[i].equals("-init")) {
                init = true;
            } else if(args[i].equals("-out")) {
                out = args[++i];
            } else if(args[i].equals("-log")) {
                log = Boolean.valueOf(args[++i]).booleanValue();
            }
        }
        openResults(init);
        
        Properties prop = new Properties();
        prop.load(getClass().getResourceAsStream("test.properties"));
        int size = Integer.parseInt(prop.getProperty("size"));
        ArrayList dbs = new ArrayList();
        for(int i=0; i<100; i++) {
            if(dbId != -1 && i != dbId) {
                continue;
            }
            String dbString = prop.getProperty("db" + i);
            if(dbString != null) {
                Database db = Database.parse(this, i, dbString);
                if(db != null) {
                    db.setTranslations(prop);
                    dbs.add(db);
                }
            }
        }
        ArrayList tests = new ArrayList();
        for(int i=0; i<100; i++) {
            String testString = prop.getProperty("test" + i);
            if(testString != null) {
                Bench bench = (Bench)Class.forName(testString).newInstance();
                tests.add(bench);
            }
        }
        testAll(dbs, tests, size);
        collect = false;
        if(dbs.size() == 0) {
            return;
        }
        ArrayList results = ((Database)dbs.get(0)).getResults();
        Connection conn = null;
        PreparedStatement prep = null;
        Statement stat = null;
        PrintWriter writer = null;
        try {
            conn = getResultConnection();
            stat = conn.createStatement();
            prep = conn.prepareStatement(
                    "INSERT INTO RESULTS(TESTID, TEST, UNIT, DBID, DB, RESULT) VALUES(?, ?, ?, ?, ?, ?)");
            for(int i=0; i<results.size(); i++) {
                Object[] res = (Object[])results.get(i);
                prep.setInt(1, i);
                prep.setString(2, res[0].toString());
                prep.setString(3, res[1].toString());
                for(int j=0; j<dbs.size(); j++) {
                    Database db  = (Database)dbs.get(j);
                    prep.setInt(4, db.getId());
                    prep.setString(5, db.getName());
                    ArrayList r = db.getResults();
                    Object[] v = (Object[])r.get(i);
                    prep.setString(6, v[2].toString());
                    prep.execute();
                }
            }
            
            writer = new PrintWriter(new FileWriter(out));
            ResultSet rs = stat.executeQuery(
                    "CALL '<table><tr><th>Test Case</th><th>Unit</th>' "
                    +"|| SELECT GROUP_CONCAT('<th>' || DB || '</th>' ORDER BY DBID SEPARATOR '') FROM "
                    +"(SELECT DISTINCT DBID, DB FROM RESULTS)"
                    +"|| '</tr>' || CHAR(10) "
                    +"|| SELECT GROUP_CONCAT('<tr><td>' || TEST || '</td><td>' || UNIT || '</td>' || ( "
                    +"SELECT GROUP_CONCAT('<td>' || RESULT || '</td>' ORDER BY DBID SEPARATOR '') FROM RESULTS R2 WHERE "
                    +"R2.TESTID = R1.TESTID) || '</tr>' ORDER BY TESTID SEPARATOR CHAR(10)) FROM "
                    +"(SELECT DISTINCT TESTID, TEST, UNIT FROM RESULTS) R1"
                    +"|| '</table>'"
            );
            rs.next();
            String result = rs.getString(1);
            writer.println(result);
        } finally {
            JdbcUtils.closeSilently(prep);
            JdbcUtils.closeSilently(stat);
            JdbcUtils.closeSilently(conn);
            IOUtils.closeSilently(writer);
        }
        
//        ResultSet rsDbs = conn.createStatement().executeQuery("SELECT DB RESULTS GROUP BY DBID, DB ORDER BY DBID");
//        while(rsDbs.next()) {
//            writer.println("<th>" + rsDbs.getString(1) + "</th>");
//        }
//        ResultSet rs = conn.createStatement().executeQuery("SELECT TEST, UNIT FROM RESULTS GROUP BY TESTID, TEST, UNIT ORDER BY TESTID");
//        while(rs.next()) {
//            writer.println("<tr><td>" + rs.getString(1) + "</td>");
//            writer.println("<td>" + rs.getString(2) + "</td>");
//            ResultSet rsRes = conn.createStatement().executeQuery("SELECT RESULT FROM RESULTS WHERE TESTID=? ORDER BY DBID");
//            
//            
//        }
        
//        PrintWriter writer = new PrintWriter(new FileWriter("benchmark.html"));
//        writer.println("<table><tr><th>Test Case</th><th>Unit</th>");
//        for(int j=0; j<dbs.size(); j++) {
//            Database db = (Database)dbs.get(j);
//            writer.println("<th>" + db.getName() + "</th>");
//        }
//        writer.println("</tr>");
//        for(int i=0; i<results.size(); i++) {
//            Object[] res = (Object[])results.get(i);
//            writer.println("<tr><td>" + res[0] + "</td>");
//            writer.println("<td>" + res[1] + "</td>");
//            for(int j=0; j<dbs.size(); j++) {
//                Database db  = (Database)dbs.get(j);
//                ArrayList r = db.getResults();
//                Object[] v = (Object[])r.get(i);
//                writer.println("<td  style=\"text-align: right\">" + v[2] + "</td>");
//            }
//            writer.println("</tr>");
//        }
//        writer.println("</table>");

        System.out.println("Test finished");
        System.exit(0);
    }
    
    private void testAll(ArrayList dbs, ArrayList tests, int size) throws Exception {
        for(int i=0; i<dbs.size(); i++) {
            if(i>0) {
                Thread.sleep(1000);
            }
            // calls garbage collection
            TestBase.getMemoryUsed();
            Database db = (Database)dbs.get(i);
            System.out.println("testing " + db.getName());
            db.startServer();
            Connection conn = db.getConnection();
            runDatabase(db, tests, 1);
            runDatabase(db, tests, 1);
            collect = true;
            runDatabase(db, tests, size);
            conn.close();
            db.log("Executed Statements", "#", db.getExecutedStatements());
            db.log("Total Time", "ms", db.getTotalTime());
            db.log("Statement per Second", "#",  db.getExecutedStatements()*1000/db.getTotalTime());
            collect = false;
            db.stopServer();
        }
    }
    
    private void runDatabase(Database db, ArrayList tests, int size) throws Exception {
        for(int j=0; j<tests.size(); j++) {
            Bench bench = (Bench)tests.get(j);
            runTest(db, bench, size);
        }
    }
    
    private void runTest(Database db, Bench bench, int size) throws Exception {
        bench.init(db, size);
        bench.runTest();
    }

    public void log(String s) {
        if(log) {
            System.out.println(s);
        }
    }

}
