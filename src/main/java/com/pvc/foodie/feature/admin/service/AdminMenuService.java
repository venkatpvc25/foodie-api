package com.pvc.foodie.feature.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.admin.dto.AdminMenuItemUpdateRequest;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.service.AuditLogService;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.rating.dto.RatingSummary;
import com.pvc.foodie.feature.rating.service.RatingService;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuItemRequest;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuUploadRequest;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuUploadResponse;
import com.pvc.foodie.feature.restaurant.dto.MenuItemResponse;
import com.pvc.foodie.feature.restaurant.entity.Category;
import com.pvc.foodie.feature.restaurant.entity.MenuItem;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;
import com.pvc.foodie.feature.restaurant.repository.CategoryRepository;
import com.pvc.foodie.feature.restaurant.repository.MenuItemRepository;
import com.pvc.foodie.feature.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMenuService {

    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RatingService ratingService;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenu(UUID restaurantId, UUID categoryId, Boolean available) {
        adminAccessService.requireAdmin();
        requireRestaurant(restaurantId);
        List<MenuItem> items = categoryId == null
                ? menuItemRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId)
                : menuItemRepository.findByRestaurantIdAndCategoryIdOrderByDisplayOrderAsc(restaurantId, categoryId);
        return items.stream()
                .filter(item -> available == null || item.isAvailable() == available)
                .map(this::toMenuItemResponse)
                .toList();
    }

    @Transactional
    public BulkMenuUploadResponse bulkCreateMenuItems(UUID restaurantId, BulkMenuUploadRequest request) {
        User admin = adminAccessService.requireAdmin();
        Restaurant restaurant = requireRestaurant(restaurantId);
        List<MenuItem> items = request.items().stream()
                .map(itemRequest -> toMenuItem(restaurant, itemRequest))
                .toList();
        List<MenuItemResponse> response = menuItemRepository.saveAll(items).stream()
                .map(this::toMenuItemResponse)
                .toList();
        auditLogService.record(
                admin,
                AuditAction.MENU_ITEMS_BULK_UPLOADED,
                AuditEntityType.RESTAURANT,
                restaurant.getId(),
                restaurant.getName(),
                "Admin bulk uploaded menu items count=" + response.size());
        log.info("Admin menu items bulk uploaded: adminId={}, restaurantId={}, count={}",
                admin.getId(), restaurantId, response.size());
        return new BulkMenuUploadResponse(response.size(), response);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(UUID restaurantId, UUID menuItemId, AdminMenuItemUpdateRequest request) {
        User admin = adminAccessService.requireAdmin();
        MenuItem item = requireMenuItem(restaurantId, menuItemId);
        applyUpdate(item, restaurantId, request);
        auditLogService.record(
                admin,
                AuditAction.MENU_ITEM_UPDATED,
                AuditEntityType.MENU_ITEM,
                item.getId(),
                item.getName(),
                "Updated menu item restaurantId=" + restaurantId + ", available=" + item.isAvailable()
                        + ", price=" + item.getPrice());
        log.info("Admin menu item updated: restaurantId={}, menuItemId={}, available={}, price={}",
                restaurantId, menuItemId, item.isAvailable(), item.getPrice());
        return toMenuItemResponse(item);
    }

    @Transactional
    public MenuItemResponse setMenuItemAvailable(UUID restaurantId, UUID menuItemId, boolean available) {
        User admin = adminAccessService.requireAdmin();
        MenuItem item = requireMenuItem(restaurantId, menuItemId);
        item.setAvailable(available);
        auditLogService.record(
                admin,
                available ? AuditAction.MENU_ITEM_ENABLED : AuditAction.MENU_ITEM_DISABLED,
                AuditEntityType.MENU_ITEM,
                item.getId(),
                item.getName(),
                "Set available=" + available + ", restaurantId=" + restaurantId);
        log.info("Admin menu item availability changed: restaurantId={}, menuItemId={}, available={}",
                restaurantId, menuItemId, available);
        return toMenuItemResponse(item);
    }

    private void applyUpdate(MenuItem item, UUID restaurantId, AdminMenuItemUpdateRequest request) {
        if (request.categoryId() != null) {
            item.setCategory(requireCategory(restaurantId, request.categoryId()));
        }
        if (request.name() != null) {
            item.setName(request.name());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.price() != null) {
            item.setPrice(request.price());
        }
        if (request.imageUrl() != null) {
            item.setImageUrl(request.imageUrl());
        }
        if (request.veg() != null) {
            item.setVeg(request.veg());
        }
        if (request.available() != null) {
            item.setAvailable(request.available());
        }
        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }
    }

    private Restaurant requireRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found"));
    }

    private MenuItem requireMenuItem(UUID restaurantId, UUID menuItemId) {
        return menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Menu item not found"));
    }

    private Category requireCategory(UUID restaurantId, UUID categoryId) {
        return categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));
    }

    private MenuItem toMenuItem(Restaurant restaurant, BulkMenuItemRequest request) {
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setCategory(resolveBulkCategory(restaurant, request));
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setImageUrl(request.imageUrl());
        item.setVeg(request.veg());
        item.setAvailable(request.available() == null || request.available());
        item.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        return item;
    }

    private Category resolveBulkCategory(Restaurant restaurant, BulkMenuItemRequest request) {
        if (request.categoryId() != null) {
            return requireCategory(restaurant.getId(), request.categoryId());
        }
        if (request.categoryName() == null || request.categoryName().isBlank()) {
            return null;
        }
        return categoryRepository.findByRestaurantIdAndNameIgnoreCase(restaurant.getId(), request.categoryName().trim())
                .orElseGet(() -> createBulkCategory(restaurant, request.categoryName().trim()));
    }

    private Category createBulkCategory(Restaurant restaurant, String name) {
        Category category = new Category();
        category.setRestaurant(restaurant);
        category.setName(name);
        category.setDisplayOrder(0);
        return categoryRepository.save(category);
    }

    private MenuItemResponse toMenuItemResponse(MenuItem item) {
        UUID categoryId = item.getCategory() == null ? null : item.getCategory().getId();
        RatingSummary summary = ratingService.getMenuItemRatingSummary(item.getId());
        return new MenuItemResponse(
                item.getId(),
                categoryId,
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.isVeg(),
                item.isAvailable(),
                item.getDisplayOrder(),
                summary.averageRating(),
                summary.ratingCount());
    }
}
