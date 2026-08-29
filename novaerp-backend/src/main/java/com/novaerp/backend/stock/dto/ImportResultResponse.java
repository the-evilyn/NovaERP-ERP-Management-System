package com.novaerp.backend.stock.dto;

import java.util.List;

/**
 * created  - new rows inserted.
 * skipped  - row was valid but already existed (matched by reference/name), left untouched.
 * failed   - row could not be imported at all (see errors).
 * warnings - row was imported but a value had to be defaulted/clamped/looked up loosely.
 */
public record ImportResultResponse(
        int created,
        int skipped,
        int failed,
        List<ImportRowIssue> errors,
        List<ImportRowIssue> warnings
) {
}
