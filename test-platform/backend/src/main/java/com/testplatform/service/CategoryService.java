package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.dto.CategoryNode;
import com.testplatform.entity.TestCategory;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
public interface CategoryService {
    Result<List<CategoryNode>> listTree();

    Result<List<TestCategory>> listAll();

    Result<Void> save(TestCategory category);

    Result<Void> update(TestCategory category);

    Result<Void> deleteById(Long id);
}
