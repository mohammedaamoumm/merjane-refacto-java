package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.ProductProcessor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Traitement des produits SEASONAL (Règle 2 du cahier des charges).
 */
@Service
public class SeasonalProductProcessor implements ProductProcessor {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public SeasonalProductProcessor(ProductRepository productRepository,
                                    NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {
        LocalDate now = LocalDate.now();

        boolean isInSeason = now.isAfter(product.getSeasonStartDate())
                && now.isBefore(product.getSeasonEndDate());

        if (isInSeason && product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);

        } else if (now.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);

        } else if (product.getSeasonStartDate().isAfter(now)) {
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);

        } else {
            product.setLeadTime(product.getLeadTime());
            productRepository.save(product);
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        }
    }

    @Override
    public ProductType getSupportedType() {
        return ProductType.SEASONAL;
    }
}
