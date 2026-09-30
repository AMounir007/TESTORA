package com.testora.ai.gateway;

/** Provider-neutral request. Prompts are masked by the gateway before reaching any provider. */
public record AiRequest(String purpose, String prompt, int maxOutputTokens) { }
