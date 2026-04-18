package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.entity.CartItem;
import com.drf.order.repository.CartItemRepository;
import com.drf.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public Cart findOrCreateCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseGet(() -> cartRepository.save(Cart.of(memberId)));
    }

    @Transactional
    public void addOrMergeItem(Long cartId, Long productId, int quantity) {
        cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .ifPresentOrElse(
                        item -> item.addQuantity(quantity),
                        () -> cartItemRepository.save(CartItem.of(cartId, productId, quantity))
                );
    }

    @Transactional(readOnly = true)
    public void validateOwnership(Long memberId, Long cartItemId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));
        cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Transactional
    public void updateQuantity(Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        item.updateQuantity(quantity);
    }

    @Transactional
    public void deleteItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional(readOnly = true)
    public List<CartItem> findItemsByMemberId(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));
        return cartItemRepository.findByCartId(cart.getId());
    }
}
