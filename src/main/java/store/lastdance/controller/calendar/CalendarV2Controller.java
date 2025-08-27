package store.lastdance.controller.calendar;

import io.swagger.v3.oas.annotations.Operation;
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
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calender.response.CalendarResponseDTO;
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
    @PostMapping
    public ResponseEntity<ApiResponse<CalendarResponseDTO>> createCalendar(
            @Valid @RequestBody CreateCalendarRequestDTO request,
            @RequestParam(required = false) UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        try {
            CalendarResponseDTO calendar = calendarService.createCalendar(request, user.getUserId(), groupId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(calendar));

        } catch (Exception e) {
            log.error("일정 생성 실패 - 사용자: {}, 에러: {}", user.getUserId(), e.getMessage(), e);
            throw new CustomException(ErrorCode.CALENDAR_CREATE_FAILED);
        }
    }

    @Operation(
            summary = "내 일정 목록 조회",
            description = "사용자의 모든 일정을 조회합니다. (필터링 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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

        try {
            List<CalendarResponseDTO> responses = calendarService.getCalendarsByUser(
                    user.getUserId(), viewType, dateTime, type, category, groupId, pageable);

            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("일정 조회 실패 - 사용자: {}, 에러: {}", user.getUserId(), e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_FOUND_FAILED);
        }
    }

    @Operation(
            summary = "특정 일정 상세 조회",
            description = "특정 일정의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarResponseDTO>> getCalendar(
            @PathVariable Long calendarId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        try {
            CalendarResponseDTO calendar = calendarService.getCalendarById(calendarId, user.getUserId());

            return ResponseEntity.ok(ApiResponse.success(calendar));

        } catch (CustomException e) {
            log.warn("일정 조회 실패 - 권한 없음: 사용자 {}, 일정 ID: {}", user.getUserId(), calendarId);
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
        } catch (Exception e) {
            log.error("일정 조회 실패 - 일정 ID: {}, 에러: {}", calendarId, e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_FOUND_FAILED);
        }
    }

    @Operation(
            summary = "일정 수정",
            description = "기존 일정의 정보를 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarResponseDTO>> updateCalendar(
            @PathVariable Long calendarId,
            @Valid @RequestBody UpdateCalendarRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        try {
            CalendarResponseDTO calendar = calendarService.updateCalendar(calendarId, request, user.getUserId());

            return ResponseEntity.ok(ApiResponse.success(calendar));

        } catch (CustomException e) {
            log.warn("일정 수정 실패 - 권한 없음: 사용자 {}, 일정 ID: {}", user.getUserId(), calendarId);
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);

        } catch (Exception e) {
            log.error("일정 수정 실패 - 일정 ID: {}, 에러: {}", calendarId, e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_UPDATE_FAILED);
        }
    }

    @Operation(
            summary = "일정 삭제",
            description = "기존 일정을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<Void>> deleteCalendar(
            @PathVariable Long calendarId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        try {
            calendarService.deleteCalendar(calendarId, user.getUserId());

            return ResponseEntity.ok(ApiResponse.success());

        } catch (CustomException e) {
            log.warn("일정 삭제 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    user.getUserId(), calendarId);
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);

        } catch (Exception e) {
            log.error("일정 삭제 실패 - 일정 ID: {}, 에러: {}", calendarId, e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_DELETE_FAILED);
        }
    }

    @Operation(
            summary = "그룹 일정 목록 조회",
            description = "특정 그룹의 모든 일정을 조회합니다. (필터링 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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

        try {
            if (!calendarService.isGroupMember(groupId, user.getUserId())) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }

            List<CalendarResponseDTO> responses = calendarService.getCalendarsByUser(
                    user.getUserId(), viewType, dateTime, type, category, groupId, pageable);

            return ResponseEntity.ok(ApiResponse.success(responses));

        } catch (Exception e) {
            log.error("그룹 일정 조회 실패 - 그룹 ID: {}, 에러: {}", groupId, e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_FOUND_FAILED);
        }
    }
}