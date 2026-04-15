package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.ProductResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.model.request.CartAddRequest;
import com.drf.order.model.request.CartUpdateRequest;
import com.drf.order.model.response.CartItemResponse;
import com.drf.order.repository.CartRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Set<String> ADDABLE_STATUSES = Set.of("ON_SALE", "SOLD_OUT");

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    @Transactional
    public void addItem(Long memberId, CartAddRequest request) {
        ProductResponse product = getProduct(request.productId());
        if (!ADDABLE_STATUSES.contains(product.status())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        cartRepository.findByMemberIdAndProductId(memberId, request.productId())
                .ifPresentOrElse(
                        cart -> cart.addQuantity(request.quantity()),
                        () -> cartRepository.save(Cart.of(memberId, product.id(), request.quantity()))
                );
    }

    @Transactional
    public void updateQuantity(Long memberId, Long productId, CartUpdateRequest request) {
        Cart cart = getCartItem(memberId, productId);
        cart.updateQuantity(request.quantity());
    }

    @Transactional
    public void removeItem(Long memberId, Long productId) {
        Cart cart = getCartItem(memberId, productId);
        cartRepository.delete(cart);
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(Long memberId) {
        return cartRepository.findByMemberId(memberId).stream()
                .map(cart -> CartItemResponse.of(cart, getProduct(cart.getProductId())))
                .toList();
    }

    private ProductResponse getProduct(Long productId) {
        try {
            return productClient.getProduct(productId).getData();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private Cart getCartItem(Long memberId, Long productId) {
        return cartRepository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }
}
