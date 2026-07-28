package com.fnpis.common.error;

/**
 * One invalid field, appended to the error skeleton under {@code errors}
 * (API contract 6.1).
 *
 * @param field         name as the client sent it
 * @param message       what is wrong with it
 * @param rejectedValue the offending value rendered as a string, null when the
 *                      value cannot be shown back safely
 */
public record FieldError(String field, String message, String rejectedValue) {
}
