package org.example.demo_ssr_v1.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// 엔티티 화면보고 설계
@Table(name = "user_tb")
@Entity
@Data
@NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;

    @CreationTimestamp
    private Timestamp createdAt;
}
