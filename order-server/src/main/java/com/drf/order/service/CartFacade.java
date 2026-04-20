package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.response.ProductResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.model.request.CartAddRequest;
import com.drf.order.model.request.CartUpdateRequest;
import com.drf.order.model.response.CartItemResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CartFacade {

    private static final Set<String> ADDABLE_STATUSES = Set.of("ON_SALE", "SOLD_OUT");

    private final CartService cartService;
    private final ProductClient productClient;

    public void addItem(Long memberId, CartAddRequest request) {
        ProductResponse product = getProduct(request.productId());
        if (!ADDABLE_STATUSES.contains(product.status())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        Cart cart = cartService.findOrCreateCart(memberId);
        cartService.addOrMergeItem(cart.getId(), request.productId(), request.quantity());
    }

    public void updateQuantity(Long memberId, Long cartItemId, CartUpdateRequest request) {
        cartService.validateOwnership(memberId, cartItemId);
        cartService.updateQuantity(cartItemId, request.quantity());
    }

    public void removeItem(Long memberId, Long cartItemId) {
        cartService.validateOwnership(memberId, cartItemId);
        cartService.deleteItem(cartItemId);
    }

    public List<CartItemResponse> getCart(Long memberId) {
        return cartService.findItemsByMemberId(memberId).stream()
                .map(item -> CartItemResponse.of(item, getProduct(item.getProductId())))
                .toList();
    }

    private ProductResponse getProduct(Long productId) {
        try {
            return productClient.getProduct(productId).getData();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
