package store.lastdance.service.expense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import store.lastdance.dto.expense.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.gemini.GeminiRequestDTO;
import store.lastdance.dto.gemini.GeminiResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.service.prompt.PromptService; // Import PromptService

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
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
        String finalPrompt = createPrompt(expenseJson);
        GeminiRequestDTO requestDTO = createRequestJson(finalPrompt);

        GeminiResponseDTO responseDTO = webClient.post()
                .bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(GeminiResponseDTO.class)
                .block();

        return parseSuggestionResponse(responseDTO);
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
            if(responseDTO == null || responseDTO.candidates() == null || responseDTO.candidates().isEmpty() || responseDTO.candidates().get(0).content() == null || responseDTO.candidates().get(0).content().parts() == null || responseDTO.candidates().get(0).content().parts().isEmpty()) {
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }
            String rawText = responseDTO.candidates().get(0).content().parts().get(0).text();

            String jsonText = null;
            Matcher m = JSON_BLOCK_PATTERN.matcher(rawText); // Use the static final Pattern
            if(m.find()) {
                jsonText = m.group(1) != null ? m.group(1) : m.group(2);
            } else if (rawText.startsWith("{") && rawText.endsWith("}")) {
                jsonText = rawText;
            } else {
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }
            jsonText = jsonText.trim();

            AnalyzeExpenseResponseDTO.Suggestion suggestion = objectMapper.readValue(jsonText,AnalyzeExpenseResponseDTO.Suggestion.class);

            return suggestion;

        } catch (JsonProcessingException e){
            throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
        }

    }
}
