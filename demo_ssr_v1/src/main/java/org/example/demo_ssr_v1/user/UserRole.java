package org.example.demo_ssr_v1.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "user_role_tb",
        // 한 사람이 같은 권한을 두 번 가질수 없게 제약 설정함
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role"})
        }
)
@NoArgsConstructor
@Getter
@Entity
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder
    public UserRole(Role role) {
        this.role = role;
    }
}
