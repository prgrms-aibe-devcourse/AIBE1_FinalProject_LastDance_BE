package store.lastdance.service.expense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import store.lastdance.dto.expense.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.gemini.GeminiRequestDTO;
import store.lastdance.dto.gemini.GeminiResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.service.prompt.PromptService; // Import PromptService

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM을 사용하여 사용자의 지출 내역을 분석하는 클라이언트 클래스입니다.
 *
 * <p>이 클래스는 LLM 서비스의 API를 호출하여 지출 내역 데이터를 분석하고,
 * 반환된 응답을 파싱하여 구조화된 분석 결과(DTO)를 제공하는 역할을 합니다.
 * </p>
 *
 * @see ExpenseService ExpenseService에서 이 클래스를 주입받아 사용하게 됩니다.
 */
@Service
@Slf4j
public class ExpenseAnalyzerImpl implements ExpenseAnalyzer {

    // Regex Pattern for JSON block parsing (kept as static final for now)
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*([\\s\\S]+?)\\s*```|```\\s*([\\s\\S]+?)\\s*```", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final PromptService promptService; // Inject PromptService

    public ExpenseAnalyzerImpl(ObjectMapper objectMapper, @Value("${GOOGLE_GEMINI_KEY}") String apiKey, PromptService promptService
    ) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.promptService = promptService; // Assign injected service
    }

    @Override
    public AnalyzeExpenseResponseDTO.Suggestion analyzerExpenseData(String expenseJson) {
        return analyzerExpenseDataRecursive(expenseJson, 3);
    }

    private AnalyzeExpenseResponseDTO.Suggestion analyzerExpenseDataRecursive(String expenseJson, int retries) {
        if (retries <= 0) {
            throw new CustomException(ErrorCode.LLM_SERVICE_UNAVAILABLE);
        }

        String finalPrompt = createPrompt(expenseJson);
        GeminiRequestDTO requestDTO = createRequestJson(finalPrompt);

        try {
            GeminiResponseDTO responseDTO = webClient.post()
                    .bodyValue(requestDTO)
                    .retrieve()
                    .bodyToMono(GeminiResponseDTO.class)
                    .block();

            return parseSuggestionResponse(responseDTO);

        } catch (WebClientResponseException e) {
            if (e.getRawStatusCode() == 503) {
                log.warn("Gemini API 503 오류, {}번 재시도합니다...", retries);
                try {
                    Thread.sleep(2000); // 2초 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return analyzerExpenseDataRecursive(expenseJson, retries - 1);
            }
            log.error("Gemini API 호출 실패. Status: {}, Body: {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.LLM_SERVICE_UNAVAILABLE);
        }
    }

    private String createPrompt(String expenseJson) {
        String combinedPromptTemplate = promptService.getPromptContent("LLM_EXPENSE_ANALYSIS_PROMPT");
        return String.format(combinedPromptTemplate, expenseJson);
    }
    private GeminiRequestDTO createRequestJson(String prompt) {
        GeminiRequestDTO dto = new GeminiRequestDTO(List.of(new GeminiRequestDTO.Content(List.of(new GeminiRequestDTO.Part(prompt)))));

        return dto;
    }
    private AnalyzeExpenseResponseDTO.Suggestion parseSuggestionResponse(GeminiResponseDTO responseDTO) {
        try{
            log.info("LLM 응답 {}", responseDTO);
            if(responseDTO == null || responseDTO.candidates() == null || responseDTO.candidates().isEmpty() || responseDTO.candidates().get(0).content() == null || responseDTO.candidates().get(0).content().parts() == null || responseDTO.candidates().get(0).content().parts().isEmpty()) {
                log.error("LLM 응답이 비어있거나 구조가 올바르지 않습니다.");
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }
            String rawText = responseDTO.candidates().get(0).content().parts().get(0).text();
            log.info("LLM이 생성한 텍스트 : {}", rawText);

            String jsonText = null;
            Matcher m = JSON_BLOCK_PATTERN.matcher(rawText); // Use the static final Pattern
            if(m.find()) {
                jsonText = m.group(1) != null ? m.group(1) : m.group(2);
            } else if (rawText.startsWith("{") && rawText.endsWith("}")) {
                jsonText = rawText;
            } else {
                log.error("LLM 응답에 JSON 블록이 없습니다.");
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }
            jsonText = jsonText.trim();

            AnalyzeExpenseResponseDTO.Suggestion suggestion = objectMapper.readValue(jsonText,AnalyzeExpenseResponseDTO.Suggestion.class);

            return suggestion;

        } catch (JsonProcessingException e){
            log.error("LLM 응답 JSON 파싱 처리 실패");
            throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
        } catch (Exception e) {
            log.error("LLM 응답 처리중 예상치 못한 오류 발생");
            throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
        }

    }
}
