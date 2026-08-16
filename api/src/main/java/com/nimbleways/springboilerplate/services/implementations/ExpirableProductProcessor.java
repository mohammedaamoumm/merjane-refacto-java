package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.ProductProcessor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Traitement des produits EXPIRABLE (Règle 3 du cahier des charges).
 */
@Service
public class ExpirableProductProcessor implements ProductProcessor {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ExpirableProductProcessor(ProductRepository productRepository,
                                     NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }

    @Override
    public ProductType getSupportedType() {
        return ProductType.EXPIRABLE;
    }
}
