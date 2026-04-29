package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.entity.CartItem;
import com.drf.order.model.dto.CartItemsResult;
import com.drf.order.repository.CartItemRepository;
import com.drf.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

    @Transactional
    public void updateCartCoupon(Long memberId, Long memberCouponId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));
        cart.updateCouponId(memberCouponId);
    }

    @Transactional(readOnly = true)
    public List<Long> getUsedMemberCouponIds(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));
        return cartItemRepository.findByCartId(cart.getId()).stream()
                .map(CartItem::getCouponId)
                .filter(Objects::nonNull)
                .toList();
    }


    @Transactional(readOnly = true)
    public CartItemsResult getValidatedCartItems(long memberId, List<Long> cartItemIds) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findAllByIdInAndCartId(cartItemIds, cart.getId());

        if (items.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_OWNED);
        }

        return new CartItemsResult(cart, items);
    }

    @Transactional
    public void updateCartItemCoupon(Long memberId, Long productId, Long memberCouponId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));
        cartItemRepository.findByCartIdAndCouponId(cart.getId(), memberCouponId)
                .ifPresent(CartItem::clearCoupon);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        item.updateCouponId(memberCouponId);
    }
}
