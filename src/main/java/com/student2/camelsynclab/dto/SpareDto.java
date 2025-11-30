package com.student2.camelsynclab.dto;

import lombok.Data;

@Data
public class SpareDto {
    private String spareCode;
    private String spareName;
    private String spareDescription;
    private String spareType;
    private String spareStatus;
    private String price;
    private Integer quantity;
    private String updatedAt;
}