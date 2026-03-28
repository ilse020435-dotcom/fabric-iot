package com.fabriciot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.common.result.Result;
import com.fabriciot.dto.blockchain.BlockchainListQuery;
import com.fabriciot.service.BlockchainService;
import com.fabriciot.vo.blockchain.BlockchainDetailVO;
import com.fabriciot.vo.blockchain.BlockchainRecordVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;

    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('blockchain:view')")
    public Result<List<BlockchainRecordVO>> list(@Valid BlockchainListQuery query) {
        IPage<BlockchainRecordVO> page = blockchainService.list(query);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @GetMapping("/{txHash}")
    @PreAuthorize("hasAuthority('blockchain:view')")
    public Result<BlockchainDetailVO> detail(@PathVariable("txHash") String txHash) {
        return Result.success(blockchainService.detail(txHash));
    }
}

