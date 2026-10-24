package com.utility.docvalidation.service;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.validator.DocumentValidator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Registry that maps each {@link DocumentType} to the validator responsible for
 * it.
 *
 * <p>Spring injects <em>all</em> {@link DocumentValidator} beans on the
 * classpath, so adding a new document type requires only a new validator
 * component: it is picked up automatically, with no change to this registry,
 * the service or the controller (Open/Closed).
 *
 * <p>The map is built once at construction and never mutated, so the registry
 * is immutable and thread-safe.
 */
@Component
public class ValidatorRegistry {

    private final Map<DocumentType, DocumentValidator> validatorsByType;

    /**
     * @param validators every {@link DocumentValidator} bean discovered by Spring
     * @throws IllegalStateException if two validators declare the same type
     */
    public ValidatorRegistry(List<DocumentValidator> validators) {
        Map<DocumentType, DocumentValidator> map = new EnumMap<>(DocumentType.class);
        for (DocumentValidator validator : validators) {
            DocumentValidator existing = map.putIfAbsent(validator.type(), validator);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate validator registered for type " + validator.type());
            }
        }
        this.validatorsByType = Map.copyOf(map);
    }

    /**
     * Looks up the validator for a type.
     *
     * @param type the document type
     * @return the validator, or {@link Optional#empty()} if none is registered
     */
    public Optional<DocumentValidator> find(DocumentType type) {
        return Optional.ofNullable(validatorsByType.get(type));
    }

    /**
     * @return the set of document types that have a registered validator
     */
    public Set<DocumentType> supportedTypes() {
        return validatorsByType.keySet();
    }
}
