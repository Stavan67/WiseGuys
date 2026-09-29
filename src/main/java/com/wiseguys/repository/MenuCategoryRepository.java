package com.wiseguys.repository;

import com.wiseguys.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findByActiveTrueOrderByDisplayOrderAsc();
    List<MenuCategory> findAllByOrderByDisplayOrderAsc();
}