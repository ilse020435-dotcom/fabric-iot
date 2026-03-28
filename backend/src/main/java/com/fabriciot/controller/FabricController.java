package com.fabriciot.controller;

import com.fabriciot.common.result.Result;
import com.fabriciot.fabric.FabricDiagnosticService;
import com.fabriciot.vo.fabric.FabricTestVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fabric")
public class FabricController {

    private final FabricDiagnosticService fabricDiagnosticService;

    public FabricController(FabricDiagnosticService fabricDiagnosticService) {
        this.fabricDiagnosticService = fabricDiagnosticService;
    }

    @GetMapping("/test")
    @PreAuthorize("hasAuthority('fabric:diagnose')")
    public Result<FabricTestVO> test(@RequestParam(value = "function", required = false) String function,
                                     @RequestParam(value = "args", required = false) List<String> args) {
        return Result.success(fabricDiagnosticService.testConnection(function, args));
    }
}
