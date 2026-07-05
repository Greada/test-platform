package com.testplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.testplatform.common.Result;
import com.testplatform.entity.CiBuild;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
public interface CiBuildService extends IService<CiBuild> {
    Result<Void> saveBuild(CiBuild build);

    Result<List<CiBuild>> listBuilds();

    Result<CiBuild> getLatestBuild();
}
