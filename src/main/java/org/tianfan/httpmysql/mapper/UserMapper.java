package org.tianfan.httpmysql.mapper;

import org.tianfan.httpmysql.pojo.User;

import java.util.List;

public interface UserMapper {
    
    List<User> getUsers();

}