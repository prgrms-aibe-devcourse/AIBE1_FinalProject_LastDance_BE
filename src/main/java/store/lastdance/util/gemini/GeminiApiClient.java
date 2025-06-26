package store.lastdance.util.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeminiApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiApiClient(@Value("${GOOGLE_GEMINI_KEY}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getJudgmentRatio(String situation) {
        String prompt = "너는 공정한 판단을 내려주는 심판이야. 다음 갈등 상황에서 누가 몇 대 몇으로 잘못했는지 숫자로 간결히 판결해줘.\n"
                + "판결 결과: (예시: 7:3로 질문자님의 잘못이 큽니다.)\n"
                + "그 다음에는 각 당사자의 잘못에 대한 이유를 각각 2~3가지씩 나눠서 간결하게 나열해줘.\n"
                + "형식은 다음과 같아:\n"
                + "판결 결과: 6:4로 질문자님의 잘못이 큽니다.\n"
                + "질문자님의 잘못:\n"
                + "1. 이유 1\n"
                + "2. 이유 2\n"
                + "상대방의 잘못:\n"
                + "1. 이유 1\n"
                + "2. 이유 2\n"
                + "상황: " + situation;

        String requestJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";

        return webClient.post()
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseGeminiResponse)
                .onErrorReturn("AI 판단 실패")
                .block();
    }

    private String parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text").asText("AI 판단 실패");
        } catch (Exception e) {
            return "AI 판단 실패";
        }
    }
}