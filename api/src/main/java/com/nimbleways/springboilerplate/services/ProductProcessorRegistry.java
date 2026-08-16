package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.entities.ProductType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registre qui résout la bonne stratégie de traitement
 * en fonction du type de produit.
 */
@Service
public class ProductProcessorRegistry {

    private final Map<ProductType, ProductProcessor> processors;

    public ProductProcessorRegistry(List<ProductProcessor> processorList) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(
                        ProductProcessor::getSupportedType,
                        Function.identity()
                ));
    }

    /**
     * Retourne le processeur approprié pour un type de produit donné.
     *
     * @param type le type de produit
     * @return le processeur correspondant
     * @throws IllegalArgumentException si aucun processeur n'existe pour ce type
     */
    public ProductProcessor getProcessor(ProductType type) {
        ProductProcessor processor = processors.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("No processor found for product type: " + type);
        }
        return processor;
    }
}
