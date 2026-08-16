package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.controllers.OrderController;
import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.exceptions.GlobalExceptionHandler;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.OrderService;
import com.nimbleways.springboilerplate.services.ProductProcessor;
import com.nimbleways.springboilerplate.services.ProductProcessorRegistry;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks 
    private ProductService productService;

    @Test
    public void test() {
        // GIVEN
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    @Test
    public void productService_handleSeasonalProduct_shouldNotifyOutOfStock_whenLeadTimeExceedsSeasonEnd() {
        Product product = new Product(1L, 60, 0, ProductType.SEASONAL, "Watermelon", null,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30));
        when(productRepository.save(any())).thenReturn(product);

        productService.handleSeasonalProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendOutOfStockNotification("Watermelon");
    }

    @Test
    public void productService_handleExpiredProduct_shouldNotifyExpiration_whenExpired() {
        Product product = new Product(1L, 15, 10, ProductType.EXPIRABLE, "Milk",
                LocalDate.now().minusDays(1), null, null);
        when(productRepository.save(any())).thenReturn(product);

        productService.handleExpiredProduct(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Milk", product.getExpiryDate());
    }

    // --- NORMAL PRODUCT PROCESSOR TESTS ---

    @Test
    public void normalProductProcessor_shouldDecrementStock_whenAvailable() {
        NormalProductProcessor processor = new NormalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 10, ProductType.NORMAL, "USB Cable", null, null, null);
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
    }

    @Test
    public void normalProductProcessor_shouldNotifyDelay_whenOutOfStock() {
        NormalProductProcessor processor = new NormalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 0, ProductType.NORMAL, "USB Cable", null, null, null);
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendDelayNotification(15, "USB Cable");
    }

    @Test
    public void normalProductProcessor_shouldDoNothing_whenOutOfStockAndZeroLeadTime() {
        NormalProductProcessor processor = new NormalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 0, 0, ProductType.NORMAL, "USB Cable", null, null, null);

        processor.process(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository, never()).save(any());
        verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
    }

    // --- SEASONAL PRODUCT PROCESSOR TESTS ---

    @Test
    public void seasonalProductProcessor_shouldDecrementStock_whenInSeasonAndAvailable() {
        SeasonalProductProcessor processor = new SeasonalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 10, ProductType.SEASONAL, "Watermelon", null,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30));
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
    }

    @Test
    public void seasonalProductProcessor_shouldNotifyOutOfStock_whenLeadTimeExceedsSeasonEnd() {
        SeasonalProductProcessor processor = new SeasonalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 60, 0, ProductType.SEASONAL, "Watermelon", null,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30));
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendOutOfStockNotification("Watermelon");
        verify(productRepository).save(product);
    }

    @Test
    public void seasonalProductProcessor_shouldNotifyOutOfStock_whenBeforeSeasonStart() {
        SeasonalProductProcessor processor = new SeasonalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 10, ProductType.SEASONAL, "Grapes", null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(60));
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        verify(notificationService).sendOutOfStockNotification("Grapes");
        verify(productRepository).save(product);
    }

    @Test
    public void seasonalProductProcessor_shouldNotifyDelay_whenInSeasonOutOfStockButLeadTimeOk() {
        SeasonalProductProcessor processor = new SeasonalProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 10, 0, ProductType.SEASONAL, "Watermelon", null,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30));
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        verify(notificationService).sendDelayNotification(10, "Watermelon");
    }

    // --- EXPIRABLE PRODUCT PROCESSOR TESTS ---

    @Test
    public void expirableProductProcessor_shouldDecrementStock_whenNotExpiredAndAvailable() {
        ExpirableProductProcessor processor = new ExpirableProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 10, ProductType.EXPIRABLE, "Butter",
                LocalDate.now().plusDays(10), null, null);
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
    }

    @Test
    public void expirableProductProcessor_shouldNotifyExpiration_whenExpired() {
        ExpirableProductProcessor processor = new ExpirableProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 10, ProductType.EXPIRABLE, "Milk",
                LocalDate.now().minusDays(1), null, null);
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Milk", product.getExpiryDate());
        verify(productRepository).save(product);
    }

    @Test
    public void expirableProductProcessor_shouldNotifyExpiration_whenOutOfStock() {
        ExpirableProductProcessor processor = new ExpirableProductProcessor(productRepository, notificationService);
        Product product = new Product(1L, 15, 0, ProductType.EXPIRABLE, "Milk",
                LocalDate.now().plusDays(5), null, null);
        when(productRepository.save(any())).thenReturn(product);

        processor.process(product);

        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Milk", product.getExpiryDate());
        verify(productRepository).save(product);
    }

    // --- PRODUCT PROCESSOR REGISTRY TESTS ---

    @Test
    public void productProcessorRegistry_shouldReturnCorrectProcessor() {
        NormalProductProcessor normalProcessor = new NormalProductProcessor(productRepository, notificationService);
        SeasonalProductProcessor seasonalProcessor = new SeasonalProductProcessor(productRepository, notificationService);
        ExpirableProductProcessor expirableProcessor = new ExpirableProductProcessor(productRepository, notificationService);

        ProductProcessorRegistry registry = new ProductProcessorRegistry(
                List.of(normalProcessor, seasonalProcessor, expirableProcessor)
        );

        assertEquals(normalProcessor, registry.getProcessor(ProductType.NORMAL));
        assertEquals(seasonalProcessor, registry.getProcessor(ProductType.SEASONAL));
        assertEquals(expirableProcessor, registry.getProcessor(ProductType.EXPIRABLE));
    }

    @Test
    public void productProcessorRegistry_shouldThrowException_whenNullType() {
        ProductProcessorRegistry registry = new ProductProcessorRegistry(List.of());
        assertThrows(IllegalArgumentException.class, () -> registry.getProcessor(null));
    }

    // --- ORDER SERVICE TESTS ---

    @Test
    public void orderService_processOrder_shouldProcessAllOrderProducts() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        NormalProductProcessor normalProcessor = new NormalProductProcessor(productRepository, notificationService);
        ProductProcessorRegistry registry = new ProductProcessorRegistry(List.of(normalProcessor));

        OrderService orderService = new OrderService(orderRepository, registry);

        Product product = new Product(1L, 15, 10, ProductType.NORMAL, "USB Cable", null, null, null);
        Order order = new Order(42L, Set.of(product));

        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(productRepository.save(any())).thenReturn(product);

        ProcessOrderResponse response = orderService.processOrder(42L);

        assertEquals(42L, response.id());
        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
    }

    @Test
    public void orderService_processOrder_shouldThrowOrderNotFoundException_whenInvalidId() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductProcessorRegistry registry = new ProductProcessorRegistry(List.of());
        OrderService orderService = new OrderService(orderRepository, registry);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.processOrder(99L));
    }

    // --- CONTROLLER & EXCEPTION HANDLER TESTS ---

    @Test
    public void orderController_shouldDelegateToOrderService() {
        OrderService orderService = mock(OrderService.class);
        OrderController controller = new OrderController(orderService);

        when(orderService.processOrder(100L)).thenReturn(new ProcessOrderResponse(100L));

        ProcessOrderResponse response = controller.processOrder(100L);

        assertEquals(100L, response.id());
        verify(orderService).processOrder(100L);
    }

    @Test
    public void globalExceptionHandler_shouldReturnNotFoundResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        OrderNotFoundException ex = new OrderNotFoundException(77L);

        Map<String, String> response = handler.handleOrderNotFound(ex);

        assertEquals("Order not found with id: 77", response.get("error"));
    }
}