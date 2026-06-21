package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.dto.CategoryNode;
import com.testplatform.entity.TestCategory;
import com.testplatform.service.CategoryService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.validation.Valid;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public Result<List<CategoryNode>> listTree() {
        return categoryService.listTree();
    }

    @GetMapping
    public Result<List<TestCategory>> listAll() {
        return categoryService.listAll();
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody TestCategory testCategory) {
        return categoryService.save(testCategory);
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody TestCategory testCategory) {
        return categoryService.update(testCategory);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return categoryService.deleteById(id);
    }
}
