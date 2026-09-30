package com.testora.ai.providers;

import com.testora.ai.gateway.AiRequest;
import com.testora.ai.gateway.AiResponse;

/**
 * SPI for AI providers (cloud, enterprise, local LLM). Implementations are discovered with
 * java.util.ServiceLoader (META-INF/services/com.testora.ai.providers.AiProvider) and ordered by {@link #priority()}
 * (lower = tried first, so cheap/local models go before expensive ones).
 * The core framework never references a concrete provider.
 */
public interface AiProvider {
    String name();

    boolean isAvailable();

    default int priority() { return 100; }

    AiResponse complete(AiRequest request) throws Exception;
}
