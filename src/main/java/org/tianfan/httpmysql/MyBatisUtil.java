package org.tianfan.httpmysql;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class MyBatisUtil {
    
    private static SqlSessionFactory sqlSessionFactory;
    
    //获取SqlSessionFactory对象
    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //通过SqlSessionFactory获取SqlSession对象，其中包含了面向数据库执行执行SQL命令所需要的方法
    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession();
    }
}