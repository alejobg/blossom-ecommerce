package com.blossom.ecommerce.shared.domain;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s was not found".formatted(resource, id));
    }
}
