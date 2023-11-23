package org.tianfan.mysqlTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class Test002 {

    static String name = "root";
    static String password = "123456";
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/itheima?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Connection> coroutine1 = CompletableFuture.supplyAsync(() -> {
            // 协程1的执行逻辑
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
            return conn;
        });

        CompletableFuture<String> coroutine2 = CompletableFuture.supplyAsync(() -> {


            // 协程2的执行逻辑
            return "进行数据库操作";
        });

        /**
         * 其他耗时操作
         */

        Connection connection = coroutine1.get();
        System.out.println(coroutine2.get()); // 输出：Coroutine 2 finished.
    }
}