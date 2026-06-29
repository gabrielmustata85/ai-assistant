package com.ai.assistant.model;

import lombok.Data;

import java.util.List;

@Data
public class Vector {

    private final String id;
    private final List<Float> values;
}
