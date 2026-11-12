package com.lauriel.store.dto.excel;

import java.io.Serializable;

public record ImportRowError(
    String sheet,
    int rowNumber,
    String column,
    String value,
    String message
) implements Serializable {}

