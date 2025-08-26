package store.lastdance.service.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.domain.analysis.FeedbackType;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.user.User;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;
import store.lastdance.dto.gemini.GeminiRequestDTO;
import store.lastdance.dto.gemini.GeminiResponseDTO;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.analysis.ExpenseAnalysisHistoryRepository;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.user.UserRepository;
import jakarta.annotation.PostConstruct;
import store.lastdance.service.prompt.PromptService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    private WebClient webClient;

    @Value("${GOOGLE_GEMINI_KEY}")
    private String apiKey;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*([\\s\\S]+?)\\s*```|```\\s*([\\s\\S]+?)\\s*```", Pattern.CASE_INSENSITIVE);

    

    @Override
    @Transactional
    public AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Expense> expenses = expenseRepository.findPersonalAndShareExpensesByDateRange(user, requestDTO.startDate(), requestDTO.endDate());
        BigDecimal totalBudget = getUserBudget(userId);

        if (expenses.isEmpty()) {
            return createEmptyAnalysis(totalBudget);
        }

        BigDecimal totalSpending = calculateTotalSpending(expenses);

        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = calculateBudgetUsage(totalSpending, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = calculateDailySpending(totalSpending,requestDTO.startDate(),requestDTO.endDate());

        List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails = calculateCategoryDetails(expenses,totalSpending);

        AnalyzeExpenseResponseDTO.Suggestion suggestion = getLlmAnalysisResult(expenses);

        String mainFinding = createMainFinding(categoryDetails);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = new AnalyzeExpenseResponseDTO.AnalysisResult(mainFinding, suggestion);

        ExpenseAnalysisHistory savedHistory = saveAnalysisHistory(user,requestDTO,budgetUsage,dailySpending,analysisResult);

        return new AnalyzeExpenseResponseDTO(savedHistory.getId(), budgetUsage,dailySpending,analysisResult,categoryDetails);
    }

    @Override
    public PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId, Pageable pageable) {
        // User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<ExpenseAnalysisHistory> historyPage = expenseAnalysisHistoryRepository.findByUser_UserId(userId, pageable);

        Page<ExpenseAnalysisHistoryDTO> historyDTOPage = historyPage.map(ExpenseAnalysisHistoryDTO::from);

        return PageWithSummaryResponse.of(historyDTOPage, null);
    }

    private ExpenseAnalysisHistory saveAnalysisHistory(User user, AnalyzeExpenseRequestDTO requestDTO, AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage, AnalyzeExpenseResponseDTO.DailySpending dailySpending, AnalyzeExpenseResponseDTO.AnalysisResult analysisResult) {

        ExpenseAnalysisHistory history = ExpenseAnalysisHistory.builder()
                .user(user)
                .startDate(requestDTO.startDate())
                .endDate(requestDTO.endDate())
                .budgetUsagePercentage(budgetUsage.percentage())
                .budgetUsageCurrentSpending(budgetUsage.currentSpending())
                .budgetUsageTotalBudget(budgetUsage.totalBudget())
                .dailySpendingAverageSoFar(dailySpending.averageSoFar())
                .dailySpendingEstimatedEom(dailySpending.estimatedEom())
                .mainFinding(analysisResult.mainFinding())
                .suggestionTitle(analysisResult.suggestion().title())
                .suggestionDescription(analysisResult.suggestion().description())
                .suggestionEffect(analysisResult.suggestion().effect())
                .suggestionDifficulty(analysisResult.suggestion().difficulty())
                .build();

        return expenseAnalysisHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public String toggleFeedback(Long historyId, UUID userid, FeedbackType type) {
        ExpenseAnalysisHistory history = expenseAnalysisHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HISTORY_NOT_FOUND));

        if (!history.getUser().getUserId().equals(userid)) {
            throw new CustomException(ErrorCode.EXPENSE_ACCESS_DENIED);
        }

        boolean isUp = (type == FeedbackType.UP);
        boolean isDown = (type == FeedbackType.DOWN);

        // 현재 상태와 같은 버튼을 다시 누르면 피드백 취소
        if ((isUp && Boolean.TRUE.equals(history.getUp())) || (isDown && Boolean.TRUE.equals(history.getDown()))) {
            history.feedback(null, null);
            return "CANCELED";
        } else {
            // 새로운 피드백 설정
            history.feedback(isUp, isDown);
            return "APPLIED";
        }
    }

    private BigDecimal getUserBudget(UUID userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return BigDecimal.valueOf(user.getUserBudget());
    }

    private BigDecimal calculateTotalSpending(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AnalyzeExpenseResponseDTO.BudgetUsage calculateBudgetUsage(BigDecimal totalSpending, BigDecimal totalBudget){
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, totalSpending, totalBudget);
        }
        BigDecimal percentage = totalSpending.multiply(BigDecimal.valueOf(100)).divide(totalBudget,2,RoundingMode.HALF_UP);
        return new AnalyzeExpenseResponseDTO.BudgetUsage(percentage.doubleValue(), totalSpending, totalBudget);
    }

    private AnalyzeExpenseResponseDTO.DailySpending calculateDailySpending(BigDecimal totalSpending, LocalDate startDate, LocalDate endDate){
        LocalDate effectiveEnd = endDate.isBefore(LocalDate.now()) ? endDate : LocalDate.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1;
        if (days <= 0) {
            days = 1;
        }
        BigDecimal averageSoFar = totalSpending.divide(BigDecimal.valueOf(days),0,RoundingMode.HALF_UP);
        int daysInMonth = endDate.lengthOfMonth();
        BigDecimal estimatedEom = averageSoFar.multiply(BigDecimal.valueOf(daysInMonth)); // 월말 예상 지출

        return new AnalyzeExpenseResponseDTO.DailySpending(averageSoFar,estimatedEom);
    }

    private List<AnalyzeExpenseResponseDTO.CategoryDetail> calculateCategoryDetails(List<Expense> expenses, BigDecimal totalSpending){
        if(totalSpending.compareTo(BigDecimal.ZERO) == 0){
            return Collections.emptyList();
        }

        Map<ExpenseCategory,List<Expense>> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        return expensesByCategory.entrySet().stream()
                .map(entry -> {
                    ExpenseCategory category = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();
                    BigDecimal categoryTotal = calculateTotalSpending(categoryExpenses);
                    double percentage = categoryTotal.divide(totalSpending,4,RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    int count = categoryExpenses.size();
                    return new AnalyzeExpenseResponseDTO.CategoryDetail(category.getDescription(),percentage,categoryTotal,count);
                }).sorted(Comparator.comparing(AnalyzeExpenseResponseDTO.CategoryDetail::percentage).reversed()).toList();
    }

    private AnalyzeExpenseResponseDTO.Suggestion getLlmAnalysisResult(List<Expense> expenses){
        // LLM에 전달할 데이터만 동적으로 가공
        List<Map<String, Object>> llmExpenseData = expenses.stream()
                .map(expense -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", expense.getTitle());
                    data.put("amount", expense.getAmount());
                    data.put("category", expense.getCategory().getDescription()); // 한글 설명
                    data.put("expenseType", expense.getExpenseType().getDescription());
                    if (expense.getSplitType() != null) {
                        data.put("splitType", expense.getSplitType().getDescription());
                    }
                    data.put("date", expense.getExpenseDate());
                    data.put("memo", expense.getMemo());
                    return data;
                })
                .toList();
        try{
            String expenseJson = objectMapper.writeValueAsString(llmExpenseData);
            return analyzerExpenseData(expenseJson);
        } catch (JsonProcessingException e){
            log.error("LLM 전송용 DTO to JSON 변환 실패");
            return new AnalyzeExpenseResponseDTO.Suggestion("데이터 처리중 오류 발생", "잠시 후 다시 시도해주세요.", "오류", "오류");
        }
    }

    private AnalyzeExpenseResponseDTO createEmptyAnalysis(BigDecimal totalBudget) {
        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, BigDecimal.ZERO, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = new AnalyzeExpenseResponseDTO.DailySpending(BigDecimal.ZERO, BigDecimal.ZERO);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = new AnalyzeExpenseResponseDTO.AnalysisResult("분석할 지출 내역이 없습니다.",
                new AnalyzeExpenseResponseDTO.Suggestion("지출 내역을 추가하고 다시 시도해주세요.","없음","없음", "없음"));

        return new AnalyzeExpenseResponseDTO(null,budgetUsage,dailySpending,analysisResult,Collections.emptyList());
    }

    private String createMainFinding(List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails) {
        if (categoryDetails == null || categoryDetails.isEmpty()){
            return "주요 지출 항목이 없습니다.";
        }
        AnalyzeExpenseResponseDTO.CategoryDetail maxCategoryDetail = categoryDetails.get(0);

        return String.format("%s 지출 집중", maxCategoryDetail.category());
    }

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
            log.debug("LLM 응답 수신 - candidates: {}", 
                      responseDTO != null && responseDTO.candidates() != null 
                          ? responseDTO.candidates().size() 
                          : 0);

            if(responseDTO == null
               || responseDTO.candidates() == null
               || responseDTO.candidates().isEmpty()
               || responseDTO.candidates().get(0).content() == null
               || responseDTO.candidates().get(0).content().parts() == null
               || responseDTO.candidates().get(0).content().parts().isEmpty()) {
                log.error("LLM 응답이 비어있거나 구조가 올바르지 않습니다.");
                throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
            }

            String rawText = responseDTO.candidates()
                                        .get(0)
                                        .content()
                                        .parts()
                                        .get(0)
                                        .text();
            if (log.isDebugEnabled()) {
                String preview = rawText == null
                                 ? ""
                                 : rawText.substring(0, Math.min(rawText.length(), 300));
                log.debug("LLM 응답 텍스트(preview 300자): {}", preview);
            }

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