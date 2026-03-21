package store.lastdance.controller.calendar;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.calendar.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calendar.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calendar.response.CalendarResponseDTO;
import store.lastdance.dto.common.ErrorResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.calendar.CalendarV2Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Calender", description = "캘린더 관리 API")
@RestController
@RequestMapping("/api/v2/calendars")
@RequiredArgsConstructor
@Slf4j
public class CalendarV2Controller {

    private final CalendarV2Service calendarService;

    @Operation(
            summary = "캘린더 생성",
            description = "새로운 일정을 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "일정 생성 성공",
                    content = @Content(schema = @Schema(implementation = CalendarResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "일정 생성 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CalendarResponseDTO>> createCalendar(
            @Valid @RequestBody CreateCalendarRequestDTO request,
            @RequestParam(required = false) UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        CalendarResponseDTO calendar = calendarService.createCalendar(request, user.getUserId(), groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(calendar));
    }

    @Operation(
            summary = "내 일정 목록 조회",
            description = "사용자의 모든 일정을 조회합니다. (필터링 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 조회 성공",
                    content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "일정 조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CalendarResponseDTO>>> getMyCalendars(
            @RequestParam(required = false, defaultValue = "MONTHLY") String viewType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user,
            Pageable pageable) {

        List<CalendarResponseDTO> responses = calendarService.getCalendarsByUser(
                user.getUserId(), viewType, dateTime, type, category, groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "특정 일정 상세 조회",
            description = "특정 일정의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CalendarResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "일정 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "일정 조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarResponseDTO>> getCalendar(
            @PathVariable Long calendarId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        CalendarResponseDTO calendar = calendarService.getCalendarById(calendarId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(calendar));
    }

    @Operation(
            summary = "일정 수정",
            description = "기존 일정의 정보를 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CalendarResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: 시작일이 종료일보다 늦음)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "일정 수정 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "일정 수정 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarResponseDTO>> updateCalendar(
            @PathVariable Long calendarId,
            @Valid @RequestBody UpdateCalendarRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        CalendarResponseDTO calendar = calendarService.updateCalendar(calendarId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(calendar));
    }

    @Operation(
            summary = "일정 삭제",
            description = "기존 일정을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "일정 삭제 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "일정 삭제 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<Void>> deleteCalendar(
            @PathVariable Long calendarId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        calendarService.deleteCalendar(calendarId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "그룹 일정 목록 조회",
            description = "특정 그룹의 모든 일정을 조회합니다. (필터링 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 일정 조회 성공",
                    content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "그룹 일정 조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<List<CalendarResponseDTO>>> getGroupCalendars(
            @PathVariable UUID groupId,
            @RequestParam(required = false, defaultValue = "MONTHLY") String viewType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal CustomOAuth2User user,
            Pageable pageable) {

        if (!calendarService.isGroupMember(groupId, user.getUserId())) {
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
        }

        List<CalendarResponseDTO> responses = calendarService.getCalendarsByUser(
                user.getUserId(), viewType, dateTime, type, category, groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
