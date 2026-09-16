package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.logic.ReceiptValidator;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

/**
 * Validates and updates an existing Receipt aggregate through the repository.
 */
public final class UpdateReceiptUseCase {

    private final IReceiptRepository repository;
    private final ReceiptValidator validator;

    public UpdateReceiptUseCase(IReceiptRepository repository) {
        this(repository, new ReceiptValidator());
    }

    public UpdateReceiptUseCase(IReceiptRepository repository,
                                ReceiptValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public void execute(Receipt receipt) {
        ReceiptValidator.ValidationResult result = validator.validate(receipt);
        if (!result.isValid()) {
            throw new ReceiptValidationException(result);
        }
        repository.updateReceipt(receipt);
    }

    public static final class ReceiptValidationException
            extends IllegalArgumentException {
        private final ReceiptValidator.ValidationResult validationResult;

        public ReceiptValidationException(
                ReceiptValidator.ValidationResult validationResult) {
            super(validationResult.getErrorCode().name());
            this.validationResult = validationResult;
        }

        public ReceiptValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
