package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductProcessorRegistry processorRegistry;

    public OrderService(OrderRepository orderRepository,
                        ProductProcessorRegistry processorRegistry) {
        this.orderRepository = orderRepository;
        this.processorRegistry = processorRegistry;
    }

    /**
     * Traite une commande : pour chaque produit de la commande,
     * applique les règles métier correspondantes à son type.
     *
     * @param orderId l'identifiant de la commande
     * @return la réponse contenant l'ID de la commande traitée
     */
    public ProcessOrderResponse processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        for (Product product : order.getItems()) {
            ProductProcessor processor = processorRegistry.getProcessor(product.getType());
            processor.process(product);
        }

        return new ProcessOrderResponse(order.getId());
    }
}
