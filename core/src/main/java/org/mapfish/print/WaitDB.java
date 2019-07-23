package org.mapfish.print;

import java.sql.DriverManager;


/**
 * If the DB isconfigured (for multi-instance), this small executable will wait for the DB to be available.
 * Otherwise, just exit with 1.
 */
public abstract class WaitDB {
    /**
     * A comment.
     *
     * @param args the parameters
     */
    public static void main(final String[] args) {
        if (System.getProperty("db.name") == null) {
            System.out.println("Not running in multi-instance mode: no DB to connect to");
            System.exit(1);
        }
        while (true) {
            try {
                Class.forName("org.postgresql.Driver");
                DriverManager.getConnection("jdbc:postgresql://" + System.getProperty("db.host") +
                                            ":" + System.getProperty("db.port", "5432") + "/" +
                                            System.getProperty("db.name"),
                                            System.getProperty("db.username"),
                                            System.getProperty("db.password"));
                System.out.println("Opened database successfully. Running in multi-instance mode");
                System.exit(0);
                return;
            } catch (Exception e) {
                System.out.println("Failed to connect to the DB: " + e.toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    //ignored
                }
            }
        }
    }
}
