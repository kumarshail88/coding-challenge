package io.bankbridge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refactoring Changes:
 * Problem 1: Model fields are public. No getters and setters.
 * <p>
 * Marked the fields as private. Using lombok.Data to generate getters and setters.
 */

@Data
@Builder
@NoArgsConstructor  //Used by Object Mapper
@AllArgsConstructor //Used by Lombok Builder.
public class BankModel {

    private String bic;
    private String name;
    private String countryCode;
    private String auth;

}
