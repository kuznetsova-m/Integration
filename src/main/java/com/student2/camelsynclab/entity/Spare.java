package com.student2.camelsynclab.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "spares")
@Data
public class Spare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spare_code", unique = true, nullable = false)
    private String spareCode;

    @Column(name = "spare_name")
    private String spareName;

    @Column(name = "spare_description", columnDefinition = "TEXT")
    private String spareDescription;

    @Column(name = "spare_type")
    private String spareType;

    @Column(name = "spare_status")
    private String spareStatus;

    @Column(name = "price")
    private Integer price;

    private Integer quantity;

    @Column(name = "updated_at")
    private String updatedAt;
}