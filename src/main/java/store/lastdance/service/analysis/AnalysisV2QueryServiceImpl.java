package store.lastdance.service.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import store.lastdance.converter.analysis.AnalysisV2Converter;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.expense.SplitType;
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
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisV2QueryServiceImpl implements AnalysisV2QueryService {

    private static final AnalyzeExpenseResponseDTO.Suggestion FALLBACK_SUGGESTION = new AnalyzeExpenseResponseDTO.Suggestion(
            "지출 분석 중 일시적인 오류가 발생했습니다",
            "잠시 후 다시 시도해주세요",
            "분석 불가",
            "해당 없음"
    );

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private final AnalysisV2Converter analysisV2Converter;
    private final TransactionTemplate transactionTemplate;

    private WebClient webClient;

    @Value("${GOOGLE_GEMINI_KEY}")
    private String apiKey;

    public AnalysisV2QueryServiceImpl(
            UserRepository userRepository,
            ExpenseRepository expenseRepository,
            ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository,
            ObjectMapper objectMapper,
            PromptService promptService,
            AnalysisV2Converter analysisV2Converter,
            PlatformTransactionManager transactionManager,
            @Value("${GOOGLE_GEMINI_KEY}") String apiKey
    ) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.expenseAnalysisHistoryRepository = expenseAnalysisHistoryRepository;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
        this.analysisV2Converter = analysisV2Converter;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new CustomException(ErrorCode.LLM_API_KEY_MISSING);
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.webClient = WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", apiKey) // API 키를 헤더로 추가
                .build();
    }

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*([\\s\\S]+?)\\s*```|```\\s*([\\s\\S]+?)\\s*```", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional
    public AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Expense> expenses = expenseRepository.findPersonalAndShareExpensesByDateRange(user, requestDTO.startDate(), requestDTO.endDate());
        BigDecimal totalBudget = getUserBudget(user);

        if (expenses.isEmpty()) {
            return createEmptyAnalysis(totalBudget);
        }

        BigDecimal totalSpending = calculateTotalSpending(expenses);

        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = calculateBudgetUsage(totalSpending, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = calculateDailySpending(totalSpending,requestDTO.startDate(),requestDTO.endDate());

        List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails = calculateCategoryDetails(expenses,totalSpending);

        AnalyzeExpenseResponseDTO.Suggestion suggestion = getLlmAnalysisResult(expenses);

        String mainFinding = createMainFinding(categoryDetails);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = analysisV2Converter.toAnalysisResult(mainFinding, suggestion);

        // Save history within a new transaction
        ExpenseAnalysisHistory savedHistory = transactionTemplate.execute(status -> {
            return saveAnalysisHistory(user, requestDTO, budgetUsage, dailySpending, analysisResult);
        });

        return analysisV2Converter.toAnalyzeExpenseResponseDTO(savedHistory.getId(), budgetUsage,dailySpending,analysisResult,categoryDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId, Pageable pageable) {
        // User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<ExpenseAnalysisHistory> historyPage = expenseAnalysisHistoryRepository.findByUser_UserId(userId, pageable);

        Page<ExpenseAnalysisHistoryDTO> historyDTOPage = historyPage.map(analysisV2Converter::toExpenseAnalysisHistoryDTO);

        return PageWithSummaryResponse.of(historyDTOPage, null);
    }

    public ExpenseAnalysisHistory saveAnalysisHistory(User user, AnalyzeExpenseRequestDTO requestDTO, AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage, AnalyzeExpenseResponseDTO.DailySpending dailySpending, AnalyzeExpenseResponseDTO.AnalysisResult analysisResult) {

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

    private BigDecimal getUserBudget(User user){
        return BigDecimal.valueOf(user.getUserBudget());
    }

    private BigDecimal calculateTotalSpending(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AnalyzeExpenseResponseDTO.BudgetUsage calculateBudgetUsage(BigDecimal totalSpending, BigDecimal totalBudget){
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return analysisV2Converter.toBudgetUsage(0.0, totalSpending, totalBudget);
        }
        BigDecimal percentage = totalSpending.multiply(BigDecimal.valueOf(100)).divide(totalBudget,2,RoundingMode.HALF_UP);
        return analysisV2Converter.toBudgetUsage(percentage.doubleValue(), totalSpending, totalBudget);
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

        return analysisV2Converter.toDailySpending(averageSoFar,estimatedEom);
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
                    return analysisV2Converter.toCategoryDetail(category.getDescription(),percentage,categoryTotal,count);
                }).sorted(Comparator.comparing(AnalyzeExpenseResponseDTO.CategoryDetail::percentage).reversed()).toList();
    }

    private AnalyzeExpenseResponseDTO.Suggestion getLlmAnalysisResult(List<Expense> expenses) {
        List<Map<String, Object>> llmExpenseData = expenses.stream()
                .map(expense -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", Objects.toString(expense.getTitle(), ""));
                    data.put("amount", expense.getAmount());
                    // 'OTHER' 멤버가 존재하므로 Enum을 기본값으로 사용
                    data.put("category", Optional.ofNullable(expense.getCategory()).orElse(ExpenseCategory.OTHER).getDescription());
                    // 기본 멤버가 없으므로, 의미에 맞는 문자열을 기본값으로 사용
                    data.put("expenseType", Optional.ofNullable(expense.getExpenseType()).map(ExpenseType::getDescription).orElse("기타"));
                    data.put("splitType", Optional.ofNullable(expense.getSplitType()).map(SplitType::getDescription).orElse("해당 없음"));
                    data.put("date", Objects.toString(expense.getExpenseDate(), ""));
                    data.put("memo", Objects.toString(expense.getMemo(), ""));
                    return data;
                })
                .toList();
        try {
            String expenseJson = objectMapper.writeValueAsString(llmExpenseData);
            return analyzerExpenseData(expenseJson);
        } catch (JsonProcessingException e) {
            log.warn("LLM 전송용 DTO to JSON 변환 실패, 기본 제안을 반환합니다.", e);
            return FALLBACK_SUGGESTION;
        }
    }

    private AnalyzeExpenseResponseDTO createEmptyAnalysis(BigDecimal totalBudget) {
        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = analysisV2Converter.toBudgetUsage(0.0, BigDecimal.ZERO, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = analysisV2Converter.toDailySpending(BigDecimal.ZERO, BigDecimal.ZERO);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = analysisV2Converter.toAnalysisResult("분석할 지출 내역이 없습니다.",
                new AnalyzeExpenseResponseDTO.Suggestion("지출 내역을 추가하고 다시 시도해주세요.","없음","없음", "없음"));

        return analysisV2Converter.toAnalyzeExpenseResponseDTO(null,budgetUsage,dailySpending,analysisResult,Collections.emptyList());
    }

    private String createMainFinding(List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails) {
        if (categoryDetails == null || categoryDetails.isEmpty()){
            return "주요 지출 항목이 없습니다.";
        }
        AnalyzeExpenseResponseDTO.CategoryDetail maxCategoryDetail = categoryDetails.get(0);

        return String.format("%s 지출 집중", maxCategoryDetail.category());
    }

    public AnalyzeExpenseResponseDTO.Suggestion analyzerExpenseData(String expenseJson) {
        int maxRetries = 3;
        long initialDelayMs = 1000;
        long maxDelayMs = 10000;
        Random jitter = new Random();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String finalPrompt = createPrompt(expenseJson);
                GeminiRequestDTO requestDTO = createRequestJson(finalPrompt);

                GeminiResponseDTO responseDTO = webClient.post()
                        .bodyValue(requestDTO)
                        .retrieve()
                        .bodyToMono(GeminiResponseDTO.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                return parseSuggestionResponse(responseDTO);

            } catch (Exception e) {
                if (isRetryable(e)) {
                    log.warn("Gemini API 호출 실패. 재시도합니다... (시도 {}/{})", attempt, maxRetries, e);

                    if (attempt == maxRetries) {
                        break;
                    }

                    try {
                        long delay = (long) (initialDelayMs * Math.pow(2, attempt - 1));
                        delay += jitter.nextInt(500);
                        long finalDelay = Math.min(delay, maxDelayMs);

                        Thread.sleep(finalDelay);

                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CustomException(ErrorCode.LLM_SERVICE_UNAVAILABLE);
                    }
                } else {
                    log.error("Gemini API 호출 중 재시도 불가능한 오류 발생", e);
                    throw new CustomException(ErrorCode.LLM_PARSING_FAILED);
                }
            }
        }
        throw new CustomException(ErrorCode.LLM_SERVICE_UNAVAILABLE);
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof WebClientResponseException webClientResponseException) {
            return webClientResponseException.getStatusCode().is5xxServerError();
        }
        if (e instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
            return true;
        }
        return e instanceof RuntimeException && e.getCause() instanceof java.util.concurrent.TimeoutException;
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
                log.warn("LLM 응답이 비어있거나 구조가 올바르지 않아 기본 제안을 반환합니다.");
                return FALLBACK_SUGGESTION;
            }

            String rawText = responseDTO.candidates()
                                        .get(0)
                                        .content()
                                        .parts()
                                        .get(0)
                                        .text();
            if (log.isDebugEnabled()) {
                String preview = rawText == null ? "" : rawText.substring(0, Math.min(rawText.length(), 300));
                log.debug("LLM 응답 텍스트(preview 300자): {}", preview);
            }

            String jsonText = null;
            Matcher m = JSON_BLOCK_PATTERN.matcher(rawText);
            if(m.find()) {
                jsonText = m.group(1) != null ? m.group(1) : m.group(2);
            } else if (rawText.startsWith("{") && rawText.endsWith("}")) {
                jsonText = rawText;
            } else {
                log.warn("LLM 응답에 JSON 블록이 없어 기본 제안을 반환합니다.");
                return FALLBACK_SUGGESTION;
            }
            jsonText = jsonText.trim();

            return objectMapper.readValue(jsonText, AnalyzeExpenseResponseDTO.Suggestion.class);

        } catch (JsonProcessingException e){
            log.warn("LLM 응답 JSON 파싱 처리 실패, 기본 제안을 반환합니다.", e);
            return FALLBACK_SUGGESTION;
        } catch (Exception e) {
            log.warn("LLM 응답 처리중 예상치 못한 오류 발생, 기본 제안을 반환합니다.", e);
            return FALLBACK_SUGGESTION;
        }
    }
}