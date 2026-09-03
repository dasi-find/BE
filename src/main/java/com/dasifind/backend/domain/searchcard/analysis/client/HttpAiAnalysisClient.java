package com.dasifind.backend.domain.searchcard.analysis.client;

import com.dasifind.backend.domain.searchcard.analysis.config.AiAnalysisProperties;
import com.dasifind.backend.global.error.BusinessException;
import com.dasifind.backend.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpAiAnalysisClient implements AiAnalysisClient {

    private final RestClient restClient;
    private final AiAnalysisProperties properties;

    public HttpAiAnalysisClient(RestClient aiAnalysisRestClient, AiAnalysisProperties properties) {
        this.restClient = aiAnalysisRestClient;
        this.properties = properties;
    }

    @Override
    public AiAnalysisClientResponse analyze(AiAnalysisClientRequest request) {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        try {
            AiAnalysisClientResponse response = restClient.post()
                    .uri(properties.endpoint())
                    .body(request)
                    .retrieve()
                    .body(AiAnalysisClientResponse.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
            }
            return response;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 503) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
