package com.ai.assistant.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Clasificarea complexității unei cereri, pentru a alege modelul (ieftin vs. puternic). */
public record QuestionComplexity(
        @JsonPropertyDescription("true dacă cererea necesită calcule fiscale, estimări, raționament pe legislație sau corelarea mai multor date; false dacă e simplă/generală")
        boolean complex,
        @JsonPropertyDescription("motivul scurt al clasificării") String reason) {
}
