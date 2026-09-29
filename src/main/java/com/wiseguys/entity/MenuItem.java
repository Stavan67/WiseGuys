package com.wiseguys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_item")
@Data
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_spicy", nullable = false)
    private boolean spicy = false;

    @Column(name = "jain_available", nullable = false)
    private boolean jainAvailable = false;

    @Column(name = "is_signature", nullable = false)
    private boolean signature = false;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "daily_available", nullable = false)
    private boolean dailyAvailable = true;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToMany
    @JoinTable(
            name = "menu_item_addon",
            joinColumns = @JoinColumn(name = "menu_item_id"),
            inverseJoinColumns = @JoinColumn(name = "addon_id")
    )
    private List<Addon> addons = new ArrayList<>();

    @Column(name = "requires_variant_selection", nullable = false)
    private boolean requiresVariantSelection = false;

    @Column(name = "variant_options", length = 200)
    private String variantOptions;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}