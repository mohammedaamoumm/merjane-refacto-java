package com.nimbleways.springboilerplate.exceptions;

/**
 * Exception lancée quand une commande n'est pas trouvée en BDD.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }
}
