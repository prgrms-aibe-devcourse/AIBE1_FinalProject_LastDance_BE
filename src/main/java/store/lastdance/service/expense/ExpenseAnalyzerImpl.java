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

import java.util.List;

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

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public ExpenseAnalyzerImpl(ObjectMapper objectMapper, @Value("${GOOGLE_GEMINI_KEY}") String apiKey
    ) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
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
        String systemInstruction = """
                당신은 재정관리 전문가입니다.
                사용자의 지출 데이터를 기반으로, 불필요한 지출을 줄일 수 있는 가장 효과적인 절약 팁 하나를 제안해주세요.
                답변은 오직 다음 마크다운 형식의 개선 제안 하나만 포함해야 합니다.
                """;
        String userPrompt = "나의 지출 내역은 다음과 같다: ```json\n" + expenseJson + "\n```\n\n" +
                            "이 데이터를 바탕으로 가장 효과적인 개선 제안 하나를 다음 형식에 맞춰 제공해주세요.";
        String formatInstruction = """
                ***Format***
                반드시 첫 줄부터 아래 포맷만 출력하고, 안내 문구나 예시 등은 출력하지 마세요.
                # 제목 : [개선 제안의 제목]
                - 제안 내용 : [구체적인 설명, 마크다운 형식으로 작성]
                - 예상 효과 : [예상되는 효과]
                - 난이도 : [쉬움/보통/어려움 중 하나]

                ***Format Example***
                # 제목 : 자동 저축 설정
                - 제안 내용 : 매월 고정 금액을 자동으로 저축하는 습관을 만들어보세요. **예상 효과를 강조하거나, 추가적인 팁을 포함할 수 있습니다.**
                - 예상 효과 : 연간 목표 달성률 40% 향상
                - 난이도 : 쉬움
                ************
                위 형식을 준수하여 작성하세요.
                """;

        String finalPrompt = systemInstruction + "\n\n" +
                             userPrompt + "\n\n" +
                             formatInstruction;

        return finalPrompt;
    }
    private GeminiRequestDTO createRequestJson(String prompt) {
        GeminiRequestDTO dto = new GeminiRequestDTO(List.of(new GeminiRequestDTO.Content(List.of(new GeminiRequestDTO.Part(prompt)))));

        return dto;
    }
    private AnalyzeExpenseResponseDTO.Suggestion parseSuggestionResponse(GeminiResponseDTO responseDTO) {

        try {
//            JsonNode root = objectMapper.readTree(responseJson);
//            String textResponse = root.path("candidates")
//                    .get(0)
//                    .path("content")
//                    .path("parts")
//                    .get(0)
//                    .path("text").asText();

            String textResponse = responseDTO.candidates().get(0).content().parts().get(0).text();

            log.info("LLM 응답 원본: {}", textResponse); // <== 원본 로그 남기기

            if (textResponse.isEmpty()) {
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }

            // LLM 응답 (마크다운 텍스트) 파싱 수정 필요
            String title = "";
            String description = "";
            String effect = "";
            String difficulty = "";

            String[] lines = textResponse.split("\n");
            for (String line : lines) {
                if (line.startsWith("#")) {
                    title = line.replace("# 제목 :", "").trim();
                } else if (line.startsWith("- 제안 내용 :")) {
                    description = line.replace("- 제안 내용 :", "").trim();
                } else if (line.startsWith("- 예상 효과 :")) {
                    effect = line.replace("- 예상 효과 :", "").trim();
                } else if (line.startsWith("- 난이도 :")) {
                    difficulty = line.replace("- 난이도 :", "").trim();
                }
            }

            if (title.isEmpty() || description.isEmpty() || effect.isEmpty() || difficulty.isEmpty()) {
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }

            return new AnalyzeExpenseResponseDTO.Suggestion(title, effect, difficulty, description);

        } catch (Exception e) { // 일반 예외 처리 추가
            log.error("LLM 응답 파싱 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
        }
    }
}
