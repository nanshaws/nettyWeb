package org.tianfan.httpmysql.pojo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class User {
    private Long id;
    private String name;
    private Integer age;
}
