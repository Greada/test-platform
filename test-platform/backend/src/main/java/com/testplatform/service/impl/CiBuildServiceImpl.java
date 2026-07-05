package com.testplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.testplatform.common.Result;
import com.testplatform.entity.CiBuild;
import com.testplatform.mapper.CiBuildMapper;
import com.testplatform.service.CiBuildService;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class CiBuildServiceImpl extends ServiceImpl<CiBuildMapper, CiBuild>
        implements CiBuildService {
    @Override
    public Result<Void> saveBuild(CiBuild build) {
        save(build);
        return Result.success(null);
    }

    @Override
    public Result<List<CiBuild>> listBuilds() {
        List<CiBuild> list = lambdaQuery().orderByDesc(CiBuild::getBuildNumber).list();
        return Result.success(list);
    }

    @Override
    public Result<CiBuild> getLatestBuild() {
        CiBuild build = lambdaQuery().orderByDesc(CiBuild::getBuildNumber).last("LIMIT 1").one();
        return Result.success(build);
    }
}
