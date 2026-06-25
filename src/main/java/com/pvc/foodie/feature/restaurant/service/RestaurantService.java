package com.pvc.foodie.feature.restaurant.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.notification.event.RestaurantCreatedEvent;
import com.pvc.foodie.feature.rating.dto.RatingSummary;
import com.pvc.foodie.feature.rating.service.RatingService;
import com.pvc.foodie.feature.restaurant.dto.CategoryRequest;
import com.pvc.foodie.feature.restaurant.dto.CategoryResponse;
import com.pvc.foodie.feature.restaurant.dto.MenuItemRequest;
import com.pvc.foodie.feature.restaurant.dto.MenuItemResponse;
import com.pvc.foodie.feature.restaurant.dto.RestaurantRequest;
import com.pvc.foodie.feature.restaurant.dto.RestaurantResponse;
import com.pvc.foodie.feature.restaurant.entity.Category;
import com.pvc.foodie.feature.restaurant.entity.MenuItem;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;
import com.pvc.foodie.feature.restaurant.repository.CategoryRepository;
import com.pvc.foodie.feature.restaurant.repository.MenuItemRepository;
import com.pvc.foodie.feature.restaurant.repository.RestaurantRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CurrentUserService currentUserService;
    private final RatingService ratingService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurants(String search) {
        List<Restaurant> restaurants = search == null || search.isBlank()
                ? restaurantRepository.findAll()
                : restaurantRepository.findByNameContainingIgnoreCase(search);
        List<RestaurantResponse> response = restaurants.stream().map(this::toRestaurantResponse).toList();
        log.info("Restaurants fetched: searchPresent={}, count={}", search != null && !search.isBlank(),
                response.size());
        return response;
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found"));
        log.info("Restaurant fetched: restaurantId={}, open={}", id, restaurant.isOpen());
        return toRestaurantResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID restaurantId) {
        ensureRestaurantExists(restaurantId);
        List<CategoryResponse> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId).stream()
                .map(this::toCategoryResponse)
                .toList();
        log.info("Restaurant categories fetched: restaurantId={}, count={}", restaurantId, categories.size());
        return categories;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenu(UUID restaurantId, UUID categoryId) {
        ensureRestaurantExists(restaurantId);
        List<MenuItem> menuItems = categoryId == null
                ? menuItemRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId)
                : menuItemRepository.findByRestaurantIdAndCategoryIdOrderByDisplayOrderAsc(restaurantId, categoryId);
        List<MenuItemResponse> response = menuItems.stream().map(this::toMenuItemResponse).toList();
        log.info("Restaurant menu fetched: restaurantId={}, categoryId={}, itemCount={}", restaurantId, categoryId,
                response.size());
        return response;
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getMyRestaurants() {
        User owner = requireRestaurantUser();
        List<RestaurantResponse> restaurants = restaurantRepository.findByCreatedByIdOrderByNameAsc(owner.getId()).stream()
                .map(this::toRestaurantResponse)
                .toList();
        log.info("Owned restaurants fetched: ownerId={}, count={}", owner.getId(), restaurants.size());
        return restaurants;
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        User owner = requireRestaurantUser();
        Restaurant restaurant = new Restaurant();
        restaurant.setCreatedBy(owner);
        applyRestaurantRequest(restaurant, request);
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created: ownerId={}, restaurantId={}, name={}, open={}",
                owner.getId(), saved.getId(), saved.getName(), saved.isOpen());
        eventPublisher.publishEvent(new RestaurantCreatedEvent(saved));
        return toRestaurantResponse(saved);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(UUID id, RestaurantRequest request) {
        Restaurant restaurant = getOwnedRestaurant(id);
        applyRestaurantRequest(restaurant, request);
        log.info("Restaurant updated: ownerId={}, restaurantId={}, name={}, open={}",
                restaurant.getCreatedBy().getId(), id, restaurant.getName(), restaurant.isOpen());
        return toRestaurantResponse(restaurant);
    }

    @Transactional
    public void deleteRestaurant(UUID id) {
        Restaurant restaurant = getOwnedRestaurant(id);
        restaurantRepository.delete(restaurant);
        log.info("Restaurant deleted: ownerId={}, restaurantId={}", restaurant.getCreatedBy().getId(), id);
    }

    @Transactional
    public CategoryResponse createCategory(UUID restaurantId, CategoryRequest request) {
        Restaurant restaurant = getOwnedRestaurant(restaurantId);
        Category category = new Category();
        category.setRestaurant(restaurant);
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        Category saved = categoryRepository.save(category);
        log.info("Category created: ownerId={}, restaurantId={}, categoryId={}, name={}",
                restaurant.getCreatedBy().getId(), restaurantId, saved.getId(), saved.getName());
        return toCategoryResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID restaurantId, UUID categoryId, CategoryRequest request) {
        getOwnedRestaurant(restaurantId);
        Category category = categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        log.info("Category updated: restaurantId={}, categoryId={}, name={}", restaurantId, categoryId,
                category.getName());
        return toCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID restaurantId, UUID categoryId) {
        getOwnedRestaurant(restaurantId);
        Category category = categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));
        categoryRepository.delete(category);
        log.info("Category deleted: restaurantId={}, categoryId={}", restaurantId, categoryId);
    }

    @Transactional
    public MenuItemResponse createMenuItem(UUID restaurantId, MenuItemRequest request) {
        Restaurant restaurant = getOwnedRestaurant(restaurantId);
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        applyMenuItemRequest(item, restaurantId, request);
        MenuItem saved = menuItemRepository.save(item);
        log.info("Menu item created: ownerId={}, restaurantId={}, menuItemId={}, name={}, price={}, available={}",
                restaurant.getCreatedBy().getId(), restaurantId, saved.getId(), saved.getName(), saved.getPrice(),
                saved.isAvailable());
        return toMenuItemResponse(saved);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(UUID restaurantId, UUID menuItemId, MenuItemRequest request) {
        getOwnedRestaurant(restaurantId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Menu item not found"));
        applyMenuItemRequest(item, restaurantId, request);
        log.info("Menu item updated: restaurantId={}, menuItemId={}, name={}, price={}, available={}",
                restaurantId, menuItemId, item.getName(), item.getPrice(), item.isAvailable());
        return toMenuItemResponse(item);
    }

    @Transactional
    public void deleteMenuItem(UUID restaurantId, UUID menuItemId) {
        getOwnedRestaurant(restaurantId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Menu item not found"));
        menuItemRepository.delete(item);
        log.info("Menu item deleted: restaurantId={}, menuItemId={}", restaurantId, menuItemId);
    }

    private void ensureRestaurantExists(UUID restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            log.warn("Restaurant lookup failed: restaurantId={}", restaurantId);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found");
        }
    }

    private Restaurant getOwnedRestaurant(UUID restaurantId) {
        User owner = requireRestaurantUser();
        return restaurantRepository.findByIdAndCreatedById(restaurantId, owner.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found"));
    }

    private User requireRestaurantUser() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != Role.RESTAURANT && user.getRole() != Role.ADMIN) {
            log.warn("Restaurant access denied: userId={}, role={}", user.getId(), user.getRole());
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Restaurant access required");
        }
        return user;
    }

    private void applyRestaurantRequest(Restaurant restaurant, RestaurantRequest request) {
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setImageUrl(request.imageUrl());
        restaurant.setPhone(request.phone());
        restaurant.setAddress(request.address());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setOpen(request.open());
        restaurant.setRazorpayLinkedAccountId(request.razorpayLinkedAccountId());
    }

    private void applyMenuItemRequest(MenuItem item, UUID restaurantId, MenuItemRequest request) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndRestaurantId(request.categoryId(), restaurantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));
        }
        item.setCategory(category);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setImageUrl(request.imageUrl());
        item.setVeg(request.veg());
        item.setAvailable(request.available());
        item.setDisplayOrder(request.displayOrder());
    }

    private RestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        RatingSummary summary = ratingService.getRestaurantRatingSummary(restaurant.getId());
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getImageUrl(),
                restaurant.getPhone(),
                restaurant.getAddress(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.isOpen(),
                summary.averageRating(),
                summary.ratingCount());
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDisplayOrder());
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
