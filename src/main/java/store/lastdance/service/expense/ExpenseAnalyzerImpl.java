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
                답변은 오직 *JSON 형식*에 맞춰 개선 제안 하나만 포함해야 합니다.
                """;
        String userPrompt = "나의 지출 내역은 다음과 같다: ```json\n" + expenseJson + "\n```\n\n" +
                            "이 데이터를 바탕으로 가장 효과적인 개선 제안 하나를 다음 형식에 맞춰 제공해주세요.";
        String formatInstruction = """
                ***Format***
                반드시 첫 줄부터 아래 포맷만 출력하고, 안내 문구나 예시 등은 출력하지 마세요.
                - "title" : [개선 제안의 제목]
                - "description" : [구체적인 설명, Markdown 형식으로 작성]
                - "effect" : [예상되는 효과]
                - "difficulty" : [쉬움/보통/어려움 중 하나]

                ***Format Example***
                {
                "title" : "자동 저축 설정"
                "description" : **Markdown 형식으로 작성**
                 "
                 지출 내역을 분석한 결과, '그룹' 유형의 지출이 상당히 많습니다. 특히 '식비', '유흥', '쇼핑' 카테고리
                 에서 그룹 지출이 빈번하게 발생하고 있습니다.
                 ### 제안:

                - 지출 항목 분석: 그룹 내에서 어떤 항목에 가장 많은 지출이 발생하는지 파악합니다. (예: 식비, 엔터테인먼트, 쇼핑 등)
                - 예산 설정: 각 항목별로 합리적인 월별 예산을 설정합니다. 예산을 초과하지 않도록 그룹 구성원들과 함께 노력합니다.
                - 대안 모색: 더 저렴한 대안을 찾아봅니다. (예: 외식 대신 직접 요리, 저렴한 엔터테인먼트 활동 찾기)
                - 정기적인 검토: 매달 예산 대비 실제 지출을 검토하고, 필요에 따라 예산을 조정합니다.
                "
                "effect" : "연간 목표 달성률 40% 향상"
                "difficulty" : "쉬움"
                }
                ************
                위 형식을 반드시 준수하여 JSON 객체만 응답하세요.
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
        try{
            log.info("LLM 응답 {}", responseDTO);
            if(responseDTO == null || responseDTO.candidates() == null || responseDTO.candidates().isEmpty() || responseDTO.candidates().get(0).content() == null || responseDTO.candidates().get(0).content().parts() == null || responseDTO.candidates().get(0).content().parts().isEmpty()) {
                log.error("LLM 응답이 비어있거나 구조가 올바르지 않습니다.");
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }
            String rawText = responseDTO.candidates().get(0).content().parts().get(0).text();
            log.info("LLM이 생성한 텍스트 : {}", rawText);

            String jsonText = null;
            Pattern p = Pattern.compile("```json\\s*([\\s\\S]+?)\\s*```|```\\s*([\\s\\S]+?)\\s*```", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(rawText);
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
