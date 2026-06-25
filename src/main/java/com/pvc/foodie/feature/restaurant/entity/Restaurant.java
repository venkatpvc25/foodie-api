package com.pvc.foodie.feature.restaurant.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.pvc.foodie.feature.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
public class Restaurant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(name = "is_open", nullable = false)
    private boolean open = true;

    @Column(name = "razorpay_linked_account_id")
    private String razorpayLinkedAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
