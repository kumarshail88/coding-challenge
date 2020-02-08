package io.bankbridge.model;

import lombok.Data;

import java.util.List;

/**
 * Refactoring Changes:
 * Refactoring Changes:
 * Problem 1: Model fields are public. No getters and setters.
 *
 * Marked the fields as private. Using lombok.Data to generate getters and setters.
 */

@Data
public class BankModelList {

    private List<BankModel> banks;

}
