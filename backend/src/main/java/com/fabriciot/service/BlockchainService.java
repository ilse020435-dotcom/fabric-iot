package com.fabriciot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.dto.blockchain.BlockchainListQuery;
import com.fabriciot.vo.blockchain.BlockchainDetailVO;
import com.fabriciot.vo.blockchain.BlockchainRecordVO;

public interface BlockchainService {

    IPage<BlockchainRecordVO> list(BlockchainListQuery query);

    BlockchainDetailVO detail(String txHash);
}

