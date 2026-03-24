package com.hardware.sales.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductCategoryEntityTest {

    @Test
    void shouldOnlyContainColumnsThatExistInProductCategoryTable() {
        Set<String> fieldNames = Arrays.stream(ProductCategory.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertEquals(Object.class, ProductCategory.class.getSuperclass());
        assertFalse(fieldNames.contains("updateTime"));
    }
}
