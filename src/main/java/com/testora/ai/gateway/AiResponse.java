package com.testora.ai.gateway;

public record AiResponse(String text, String provider, String model, int tokens, long latencyMs) { }
