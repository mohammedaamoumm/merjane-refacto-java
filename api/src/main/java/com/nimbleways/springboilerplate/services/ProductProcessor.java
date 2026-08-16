package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;

/**
 * Interface Strategy pour le traitement des produits.
 * Chaque type de produit (NORMAL, SEASONAL, EXPIRABLE) a sa propre implémentation.
 */
public interface ProductProcessor {

    /**
     * Traite un produit commandé : décrémente le stock si possible,
     * sinon gère la notification appropriée selon les règles métier.
     *
     * @param product le produit à traiter
     */
    void process(Product product);

    /**
     * Retourne le type de produit que cette stratégie gère.
     * Utilisé par le registre pour router vers la bonne stratégie.
     *
     * @return le ProductType supporté
     */
    ProductType getSupportedType();
}
