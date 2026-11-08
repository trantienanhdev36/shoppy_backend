package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import com.shoppy.backend.common.enums.ShipperTransactionType;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.entity.Order;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.ShopBalanceTransaction;
import com.shoppy.backend.entity.ShipperCodTransaction;
import com.shoppy.backend.repository.OrderRepository;
import com.shoppy.backend.repository.ShopBalanceTransactionRepository;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.ShipperCodTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopBalanceService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ShopBalanceTransactionRepository shopBalanceTransactionRepository;
    private final ShipperCodTransactionRepository shipperCodTransactionRepository;

    @Transactional
    public void creditWalletOrder(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            creditOrder(order, PaymentMethod.WALLET);
        }
    }

    @Transactional
    public void creditRemittedCodOrders(Long shipperId, BigDecimal totalRemitted) {
        List<ShipperCodTransaction> collections =
                shipperCodTransactionRepository.findCompletedCollectionsByShipperId(shipperId);
        BigDecimal collectedThrough = BigDecimal.ZERO;

        for (ShipperCodTransaction collection : collections) {
            collectedThrough = collectedThrough.add(collection.getAmount());
            if (collectedThrough.compareTo(totalRemitted) > 0) {
                break;
            }

            Order order = orderRepository.findById(collection.getOrderId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            if (order.getPaymentMethod() != PaymentMethod.COD || order.getStatus() != OrderStatus.DELIVERED) {
                throw new IllegalStateException(
                        "COD collection does not belong to a delivered COD order: " + collection.getOrderId());
            }
            creditOrder(order, PaymentMethod.COD);
        }
    }

    private void creditOrder(Order order, PaymentMethod paymentMethod) {
        Shop shop = shopRepository.findByIdWithLock(order.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (shopBalanceTransactionRepository.existsByOrderId(order.getId())) {
            return;
        }

        shop.setBalance(shop.getBalance().add(order.getTotalAmount()));
        shopBalanceTransactionRepository.save(ShopBalanceTransaction.builder()
                .shopId(shop.getId())
                .orderId(order.getId())
                .paymentMethod(paymentMethod)
                .amount(order.getTotalAmount())
                .build());
    }
}
