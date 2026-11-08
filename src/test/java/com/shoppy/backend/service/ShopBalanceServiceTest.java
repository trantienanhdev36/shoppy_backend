package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import com.shoppy.backend.common.enums.ShipperTransactionType;
import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.entity.Order;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.ShopBalanceTransaction;
import com.shoppy.backend.entity.ShipperCodTransaction;
import com.shoppy.backend.repository.OrderRepository;
import com.shoppy.backend.repository.ShopBalanceTransactionRepository;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.ShipperCodTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopBalanceServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ShopRepository shopRepository;
    @Mock
    private ShopBalanceTransactionRepository shopBalanceTransactionRepository;
    @Mock
    private ShipperCodTransactionRepository shipperCodTransactionRepository;

    @InjectMocks
    private ShopBalanceService service;

    @Test
    void creditsWalletOrderOnceExcludingShippingFee() {
        Order order = order(1L, 10L, PaymentMethod.WALLET, "120.00");
        Shop shop = shop(10L, "50.00");
        when(shopRepository.findByIdWithLock(10L)).thenReturn(Optional.of(shop));
        when(shopBalanceTransactionRepository.existsByOrderId(1L)).thenReturn(false, true);

        service.creditWalletOrder(order);
        service.creditWalletOrder(order);

        assertEquals(new BigDecimal("170.00"), shop.getBalance());
        verify(shopBalanceTransactionRepository).save(any());
    }

    @Test
    void creditsOnlyCodOrdersFullyCoveredByRemittance() {
        Order first = order(1L, 10L, PaymentMethod.COD, "100.00");
        Order second = order(2L, 10L, PaymentMethod.COD, "200.00");
        ShipperCodTransaction firstCollection = collection(1L, "110.00");
        ShipperCodTransaction secondCollection = collection(2L, "210.00");
        ShipperCodTransaction partialCollection = collection(3L, "160.00");
        Shop shop = shop(10L, "0.00");

        when(shipperCodTransactionRepository.findCompletedCollectionsByShipperId(7L))
                .thenReturn(List.of(firstCollection, secondCollection, partialCollection));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(first));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(second));
        when(shopRepository.findByIdWithLock(10L)).thenReturn(Optional.of(shop));
        when(shopBalanceTransactionRepository.existsByOrderId(anyLong())).thenReturn(false);

        service.creditRemittedCodOrders(7L, new BigDecimal("320.00"));

        assertEquals(new BigDecimal("300.00"), shop.getBalance());
        verify(orderRepository, never()).findById(3L);
        ArgumentCaptor<ShopBalanceTransaction> captor = ArgumentCaptor.forClass(ShopBalanceTransaction.class);
        verify(shopBalanceTransactionRepository, times(2)).save(captor.capture());
        assertEquals(List.of(1L, 2L), captor.getAllValues().stream()
                .map(ShopBalanceTransaction::getOrderId)
                .toList());
    }

    private static Order order(Long id, Long shopId, PaymentMethod method, String total) {
        return Order.builder()
                .id(id)
                .shopId(shopId)
                .paymentMethod(method)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal(total))
                .build();
    }

    private static Shop shop(Long id, String balance) {
        return Shop.builder().id(id).balance(new BigDecimal(balance)).build();
    }

    private static ShipperCodTransaction collection(Long orderId, String amount) {
        return ShipperCodTransaction.builder()
                .orderId(orderId)
                .type(ShipperTransactionType.COLLECTION)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal(amount))
                .build();
    }
}
