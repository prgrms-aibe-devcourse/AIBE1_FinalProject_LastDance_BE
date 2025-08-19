package store.lastdance.controller.analysis;

/*
 NOTE: Testing stack used in this file:
 - JUnit 5 (Jupiter) for unit testing
 - Mockito (JUnit 5 MockitoExtension) for mocking dependencies
 We keep these as pure unit tests (no Spring context) to validate controller behavior,
 service interactions, and critical annotations via reflection.
*/

import io.swagger.v3.oas.annotations.Parameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import store.lastdance.aspect.RateLimit;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.analysis.AnalysisService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private CustomOAuth2User oAuth2User;

    @InjectMocks
    private AnalysisController controller;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("analyzeExpenses: delegates to service using authenticated user's ID and returns 200 OK")
    void analyzeExpenses_delegatesAndReturnsOk() {
        AnalyzeExpenseRequestDTO requestDTO = mock(AnalyzeExpenseRequestDTO.class);
        AnalyzeExpenseResponseDTO responseDTO = mock(AnalyzeExpenseResponseDTO.class);

        when(oAuth2User.getUserId()).thenReturn(USER_ID);
        when(analysisService.analyzeExpenses(USER_ID, requestDTO)).thenReturn(responseDTO);

        ResponseEntity<ApiResponse<AnalyzeExpenseResponseDTO>> response = controller.analyzeExpenses(oAuth2User, requestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return HTTP 200 OK");
        assertNotNull(response.getBody(), "ApiResponse body should not be null");

        // Verify the controller called the service with the correct arguments
        verify(analysisService, times(1)).analyzeExpenses(USER_ID, requestDTO);

        // Attempt to verify the ApiResponse contains the same data using reflection to avoid tight coupling
        try {
            Object body = response.getBody();
            assertNotNull(body, "Body should not be null");
            Method getData = body.getClass().getMethod("getData");
            Object data = getData.invoke(body);
            assertSame(responseDTO, data, "ApiResponse data should be the service returned DTO");
        } catch (NoSuchMethodException e) {
            // If ApiResponse does not expose getData(), we skip deep assertion but keep interaction checks
        } catch (Exception e) {
            fail("Failed to introspect ApiResponse via reflection: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("analyzeExpenses: method has @RateLimit and POST mapping '/expenses'")
    void analyzeExpenses_hasRateLimitAndPostMapping() throws Exception {
        Method m = AnalysisController.class.getDeclaredMethod(
                "analyzeExpenses",
                CustomOAuth2User.class,
                AnalyzeExpenseRequestDTO.class
        );

        assertTrue(m.isAnnotationPresent(RateLimit.class), "analyzeExpenses should be annotated with @RateLimit");

        PostMapping post = m.getAnnotation(PostMapping.class);
        assertNotNull(post, "analyzeExpenses should be annotated with @PostMapping");

        String[] values = post.value().length > 0 ? post.value() : post.path();
        assertTrue(arrayContains(values, "/expenses"),
                "POST mapping should contain '/expenses' but was: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("feedbackAnalyzeExpense: delegates to service and returns 200 OK for 'like'")
    void feedbackAnalyzeExpense_delegatesAndReturnsOk_like() {
        long historyId = 123L;
        String type = "like";
        String serviceResult = "toggled";

        when(oAuth2User.getUserId()).thenReturn(USER_ID);
        when(analysisService.toggleFeedback(historyId, USER_ID, type)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<String>> response = controller.feedbackAnalyzeExpense(oAuth2User, historyId, type);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return HTTP 200 OK");
        assertNotNull(response.getBody(), "ApiResponse body should not be null");
        verify(analysisService, times(1)).toggleFeedback(historyId, USER_ID, type);

        // Reflection check for ApiResponse data
        try {
            Object body = response.getBody();
            assertNotNull(body, "Body should not be null");
            Method getData = body.getClass().getMethod("getData");
            Object data = getData.invoke(body);
            assertEquals(serviceResult, data, "ApiResponse data should be the service returned result");
        } catch (NoSuchMethodException e) {
            // Skip if ApiResponse doesn't have getData()
        } catch (Exception e) {
            fail("Failed to introspect ApiResponse via reflection: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("feedbackAnalyzeExpense: propagates service exception for invalid input")
    void feedbackAnalyzeExpense_propagatesException() {
        long historyId = 987L;
        String type = "invalid";

        when(oAuth2User.getUserId()).thenReturn(USER_ID);
        when(analysisService.toggleFeedback(historyId, USER_ID, type))
                .thenThrow(new IllegalArgumentException("invalid type"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.feedbackAnalyzeExpense(oAuth2User, historyId, type),
                "Should propagate service exception"
        );
        assertTrue(ex.getMessage() == null || ex.getMessage().toLowerCase().contains("invalid"),
                "Exception message should indicate invalid input");
        verify(analysisService, times(1)).toggleFeedback(historyId, USER_ID, type);
    }

    @Test
    @DisplayName("feedbackAnalyzeExpense: handles null 'type' by delegating null to service")
    void feedbackAnalyzeExpense_nullTypeDelegation() {
        long historyId = 45L;
        when(oAuth2User.getUserId()).thenReturn(USER_ID);
        when(analysisService.toggleFeedback(eq(historyId), eq(USER_ID), isNull())).thenReturn("OK");

        ResponseEntity<ApiResponse<String>> response = controller.feedbackAnalyzeExpense(oAuth2User, historyId, null);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return HTTP 200 OK");
        assertNotNull(response.getBody(), "ApiResponse body should not be null");
        verify(analysisService, times(1)).toggleFeedback(eq(historyId), eq(USER_ID), isNull());
    }

    @Test
    @DisplayName("getAnalysisHistoryList: delegates to service and returns 200 OK")
    void getAnalysisHistoryList_delegatesAndReturnsOk() {
        Pageable pageable = mock(Pageable.class);
        PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> page = mock(PageWithSummaryResponse.class);

        when(oAuth2User.getUserId()).thenReturn(USER_ID);
        when(analysisService.getExpenseAnalysisHistory(USER_ID, pageable)).thenReturn(page);

        ResponseEntity<ApiResponse<PageWithSummaryResponse<ExpenseAnalysisHistoryDTO>>> response =
                controller.getAnalysisHistoryList(oAuth2User, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return HTTP 200 OK");
        assertNotNull(response.getBody(), "ApiResponse body should not be null");
        verify(analysisService, times(1)).getExpenseAnalysisHistory(USER_ID, pageable);

        // Reflection check for ApiResponse data
        try {
            Object body = response.getBody();
            assertNotNull(body, "Body should not be null");
            Method getData = body.getClass().getMethod("getData");
            Object data = getData.invoke(body);
            assertSame(page, data, "ApiResponse data should be the page returned by service");
        } catch (NoSuchMethodException e) {
            // Skip if ApiResponse doesn't have getData()
        } catch (Exception e) {
            fail("Failed to introspect ApiResponse via reflection: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("getAnalysisHistoryList: PageableDefault annotations enforce sort by createdAt DESC")
    void getAnalysisHistoryList_pageableDefaultAnnotation() throws Exception {
        Method m = AnalysisController.class.getDeclaredMethod(
                "getAnalysisHistoryList",
                CustomOAuth2User.class,
                Pageable.class
        );
        java.lang.reflect.Parameter[] params = m.getParameters();
        assertEquals(2, params.length, "Expected two parameters");
        java.lang.reflect.Parameter pageableParam = params[1];

        PageableDefault pageableDefault = pageableParam.getAnnotation(PageableDefault.class);
        assertNotNull(pageableDefault, "Pageable parameter should be annotated with @PageableDefault");

        String[] sortFields = pageableDefault.sort();
        assertNotNull(sortFields);
        assertTrue(Arrays.asList(sortFields).contains("createdAt"),
                "PageableDefault.sort should include 'createdAt'");

        Sort.Direction dir = pageableDefault.direction();
        assertEquals(Sort.Direction.DESC, dir, "PageableDefault.direction should be DESC");
    }

    @Test
    @DisplayName("@AuthenticationPrincipal parameter is hidden in OpenAPI (@Parameter(hidden = true)) for all endpoints")
    void authenticationPrincipalParameter_isHiddenInOpenApi() throws Exception {
        // analyzeExpenses(CustomOAuth2User, AnalyzeExpenseRequestDTO)
        {
            Method m = AnalysisController.class.getDeclaredMethod(
                    "analyzeExpenses",
                    CustomOAuth2User.class,
                    AnalyzeExpenseRequestDTO.class
            );
            java.lang.reflect.Parameter principalParam = m.getParameters()[0];
            Parameter p = principalParam.getAnnotation(Parameter.class);
            assertNotNull(p, "First parameter should have @Parameter annotation");
            assertTrue(p.hidden(), "Authentication principal should be hidden in OpenAPI");
        }

        // feedbackAnalyzeExpense(CustomOAuth2User, Long, String)
        {
            Method m = AnalysisController.class.getDeclaredMethod(
                    "feedbackAnalyzeExpense",
                    CustomOAuth2User.class,
                    Long.class,
                    String.class
            );
            java.lang.reflect.Parameter principalParam = m.getParameters()[0];
            Parameter p = principalParam.getAnnotation(Parameter.class);
            assertNotNull(p, "First parameter should have @Parameter annotation");
            assertTrue(p.hidden(), "Authentication principal should be hidden in OpenAPI");
        }

        // getAnalysisHistoryList(CustomOAuth2User, Pageable)
        {
            Method m = AnalysisController.class.getDeclaredMethod(
                    "getAnalysisHistoryList",
                    CustomOAuth2User.class,
                    Pageable.class
            );
            java.lang.reflect.Parameter principalParam = m.getParameters()[0];
            Parameter p = principalParam.getAnnotation(Parameter.class);
            assertNotNull(p, "First parameter should have @Parameter annotation");
            assertTrue(p.hidden(), "Authentication principal should be hidden in OpenAPI");
        }
    }

    @Test
    @DisplayName("Controller class has base RequestMapping '/api/v1/analysis'")
    void controllerClass_hasBaseRequestMapping() {
        RequestMapping mapping = AnalysisController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping, "Controller should be annotated with @RequestMapping");
        String[] values = mapping.value().length > 0 ? mapping.value() : mapping.path();
        assertTrue(arrayContains(values, "/api/v1/analysis"),
                "Class-level mapping should include '/api/v1/analysis' but was: " + Arrays.toString(values));
    }

    // Helpers

    private static boolean arrayContains(String[] arr, String sought) {
        if (arr == null) return false;
        for (String s : arr) {
            if (sought.equals(s)) return true;
        }
        return false;
    }
}