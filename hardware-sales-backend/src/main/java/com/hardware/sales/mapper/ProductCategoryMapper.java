package com.hardware.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hardware.sales.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
