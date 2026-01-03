package org.example.demo_ssr_v1._api;

import lombok.Data;

// 응답 ToDo 에 대한 DTO 설계
@Data
public class ToDo {
//    { "userId": 1, "id": 1, "title": "delectus aut autem", "completed": false }
    // JSON 키값은 무조건 ""내부여야하고 마지막에 , 로 끝나면 안됨
    // JsonFormatter
    private Integer userId;
    private Integer id;
    private String title;
    private boolean completed;
}
