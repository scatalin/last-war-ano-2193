package com.lastwar.ano2193.model;

import jakarta.persistence.*;

@Entity
@Table(name = "category_tag")
public class CategoryTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private UploadCategory category;

    @Column(nullable = false)
    private String name;

    private int displayOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UploadCategory getCategory() { return category; }
    public void setCategory(UploadCategory category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}