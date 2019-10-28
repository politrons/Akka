package io.cloudstate.shopping;

import io.cloudstate.javasupport.CloudState;
import io.cloudstate.shopping.domain.Domain;

/**
 * Main class to register the entity created using [CloudState] instance and the [registerEventSourcedEntity]
 * in this case since the entity is an EventSourcing entity.
 */
public class ShoppingCartMain {

    public static void main(String... args) {
        new CloudState()
                .registerEventSourcedEntity(
                        ShoppingCartEntity.class,
                        Protocol.getDescriptor().findServiceByName("ShoppingCartService"),
                        Domain.getDescriptor())
                .start();
    }
}