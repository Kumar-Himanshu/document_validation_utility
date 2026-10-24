package com.utility.docvalidation.service;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.dto.ValidationRequest;
import com.utility.docvalidation.dto.ValidationResponse;
import com.utility.docvalidation.exception.UnsupportedDocumentTypeException;
import com.utility.docvalidation.util.SensitiveDataMasker;
import com.utility.docvalidation.validator.DocumentValidator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default orchestration of document validation.
 *
 * <p>Parses the (case-insensitive, alias-aware) document type, routes to the
 * matching validator via the {@link ValidatorRegistry}, records per-type
 * metrics and returns the uniform response. The input value is echoed back
 * exactly as received and is only ever logged in masked form.
 */
@Service
public class DefaultDocumentValidationService implements DocumentValidationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultDocumentValidationService.class);

    private static final String METRIC_TIMER = "document.validation";
    private static final String TAG_TYPE = "type";
    private static final String TAG_RESULT = "result";

    private final ValidatorRegistry registry;
    private final MeterRegistry meterRegistry;

    /**
     * @param registry      the validator registry (constructor injected)
     * @param meterRegistry Micrometer registry for per-type request/latency metrics
     */
    public DefaultDocumentValidationService(ValidatorRegistry registry, MeterRegistry meterRegistry) {
        this.registry = registry;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ValidationResponse validate(ValidationRequest request) {
        DocumentType type = DocumentType.parse(request.documentType())
                .orElseThrow(() -> new UnsupportedDocumentTypeException(
                        "Unsupported documentType. Supported types: " + DocumentType.supportedTokens()));

        DocumentValidator validator = registry.find(type)
                .orElseThrow(() -> new UnsupportedDocumentTypeException(
                        "Unsupported documentType. Supported types: " + DocumentType.supportedTokens()));

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean valid = validator.validate(request.documentNumber());
        sample.stop(meterRegistry.timer(METRIC_TIMER,
                TAG_TYPE, type.name(),
                TAG_RESULT, Boolean.toString(valid)));

        log.info("Validated type={} value={} result={}",
                type, SensitiveDataMasker.mask(request.documentNumber()), valid);

        // Echo the document number back exactly as received.
        return new ValidationResponse(request.documentNumber(), valid);
    }

    @Override
    public Set<DocumentType> supportedTypes() {
        return registry.supportedTypes();
    }
}
