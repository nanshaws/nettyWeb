package org.tianfan.mysqlTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.ThreadFactory;

public class Test001 {
    static String name = "root";
    static String password = "123456";
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/itheima?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static void main(String[] args) throws InterruptedException {

        Runnable runnable = () -> {
            Connection conn = null;
            Statement stmt = null;
            try {
                // 注册 JDBC 驱动
                Class.forName(JDBC_DRIVER);

                // 打开链接
                System.out.println("连接数据库...");
                conn = DriverManager.getConnection(DB_URL, name, password);
                System.out.println("连接成功...");

            }catch (Exception e){
                e.printStackTrace();
            }
        };
//        Runnable runn = () -> {
//            System.out.println("Hello, world!");
//        };
//
       ThreadFactory virtualThreadFactory = Thread.ofPlatform().name("prefix", 0).factory();
//
//        Thread factoryThread = virtualThreadFactory.newThread(runn);
//        factoryThread.start();

// 使用静态构建器方法
        Thread virtualThread = Thread.startVirtualThread(runnable);

        Thread.sleep(1000);
    }

}
