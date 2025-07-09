package store.lastdance.exception;

import org.apache.http.protocol.HTTP;
import org.hibernate.Internal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import store.lastdance.dto.common.ErrorResponseDTO;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Global Exception Handler 테스트")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @Test
    @DisplayName("CustomException 처리")
    void handleCustomException() {
        // given
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);
        given(webRequest.getDescription(false)).willReturn("uri=/api/v1/test");

        // when
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo(exception.getMessage());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("다른 ErrorCode의 CustomException 처리")
    void handleCustomException_DifferentErrorCode() {
        // given
        CustomException exception = new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        given(webRequest.getDescription(false)).willReturn("uri=/api/v1/groups");

        // when
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().error()).isEqualTo("Forbidden");
        assertThat(response.getBody().message()).isEqualTo(exception.getMessage());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/groups");
    }

    @Test
    @DisplayName("서버 에러 ErrorCode 처리")
    void handleCustomException_InternalServerError() {
        // given
        CustomException exception = new CustomException(ErrorCode.USER_CREATE_FAILED);
        given(webRequest.getDescription(false)).willReturn("uri=/api/v1/admin");

        // when
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo(exception.getMessage());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/admin");
    }

    @Test
    @DisplayName("BAD_REQUEST ErrorCode 처리")
    void handleCustomException_BadRequest() {
        // given
        CustomException exception = new CustomException(ErrorCode.INVALID_CHECKLIST_REQUEST);
        given(webRequest.getDescription(false)).willReturn("uri=/api/v1/checklists");

        // when
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo(exception.getMessage());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/checklists");
    }
}

