package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.ProductProcessor;
import org.springframework.stereotype.Service;

/**
 * Traitement des produits NORMAL (Règle 1 du cahier des charges).
 */
@Service
public class NormalProductProcessor implements ProductProcessor {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public NormalProductProcessor(ProductRepository productRepository,
                                  NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else if (product.getLeadTime() > 0) {
            product.setLeadTime(product.getLeadTime());
            productRepository.save(product);
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        }
    }

    @Override
    public ProductType getSupportedType() {
        return ProductType.NORMAL;
    }
}
