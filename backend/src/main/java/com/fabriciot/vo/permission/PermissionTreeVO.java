package com.fabriciot.vo.permission;

import lombok.Data;

import java.util.List;

@Data
public class PermissionTreeVO {

    private String key;

    private String label;

    private String type;

    private List<PermissionTreeVO> children;
}

