
package com.pvc.foodie.feature.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(name = "razorpay_linked_account_id")
    private String razorpayLinkedAccountId;

    @Column(nullable = false)
    private boolean blocked;

    @Enumerated(EnumType.STRING)
    private Role role;
}
