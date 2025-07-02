package store.lastdance.util.youthpolicy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class YouthPolicyClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openapi.youth.api-key}")
    private String apiKey;

    public YouthPolicyClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://www.youthcenter.go.kr")
                .build();
    }

    public JsonNode getYouthPolicies(int pageNum, int pageSize, String keyword) {
        String uri = UriComponentsBuilder.fromPath("/go/ythip/getPlcy")
                .queryParam("apiKeyNm", apiKey)
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .queryParam("rtnType", "json")
                .queryParam("plcyKywdNm", keyword)
                .toUriString();

        String response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("청년정책 응답 파싱 실패", e);
        }
    }
}
