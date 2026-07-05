package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.CiBuild;
import com.testplatform.service.CiBuildService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/ci")
public class CiBuildController {
    private final CiBuildService ciBuildService;

    public CiBuildController(CiBuildService ciBuildService) {
        this.ciBuildService = ciBuildService;
    }

    @PostMapping("/builds")
    public Result<Void> saveBuild(@RequestBody CiBuild build) {
        return ciBuildService.saveBuild(build);
    }

    @GetMapping("/builds")
    public Result<List<CiBuild>> listBuilds() {
        return ciBuildService.listBuilds();
    }

    @GetMapping("/builds/latest")
    public Result<CiBuild> getLatestBuild() {
        return ciBuildService.getLatestBuild();
    }
}
