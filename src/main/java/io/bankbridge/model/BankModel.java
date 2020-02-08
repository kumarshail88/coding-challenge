package io.bankbridge.model;

import lombok.Data;

/**
 * Refactoring Changes:
 * Problem 1: Model fields are public. No getters and setters.
 *
 * Marked the fields as private. Using lombok.Data to generate getters and setters.
 */

@Data
public class BankModel {

    private String bic;
    private String name;
    private String countryCode;
    private String auth;

}
