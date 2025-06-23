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
import store.lastdance.domain.calendar.Schedule;
import store.lastdance.dto.calender.request.CreateScheduleRequestDTO;
import store.lastdance.dto.calender.request.UpdateScheduleRequestDTO;
import store.lastdance.dto.calender.response.ScheduleResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.calendar.CalendarService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Calender", description = "캘린더 관리 API")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(
            summary = "캘린더 생성",
            description = "새로운 일정을 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "일정 생성 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponseDTO>> createSchedule(
            @Valid @RequestBody CreateScheduleRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("일정 생성 요청 - 사용자: {}, 제목: {}",
                user.getUserId(), request.getTitle());

        try {
            Schedule schedule = calendarService.createSchedule(request, user.getUserId());
            ScheduleResponseDTO response = ScheduleResponseDTO.from(schedule);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "일정이 성공적으로 생성되었습니다."));

        } catch (Exception e) {
            log.error("일정 생성 실패 - 사용자: {}, 에러: {}",
                    user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("일정 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "내 일정 목록 조회",
            description = "사용자의 모든 일정을 조회합니다. (필터링 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "일정 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDTO>>> getMySchedules(
            @RequestParam(required = false, defaultValue = "MONTHLY") String viewType,
            @RequestParam(required = false) LocalDateTime dateTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user,
            Pageable pageable) {

        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }

        log.info("일정 목록 조회 - 사용자: {}, 달력 종류: {}, 기준 날짜: {}",
                user.getUserId(), viewType, dateTime);

        try {
            List<Schedule> schedules = calendarService.getSchedulesByUser(
                    user.getUserId(), viewType, dateTime, type, category, groupId, pageable);

            List<ScheduleResponseDTO> responses = schedules.stream()
                    .map(ScheduleResponseDTO::from)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(responses));

        } catch (Exception e) {
            log.error("일정 조회 실패 - 사용자: {}, 에러: {}",
                    user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("일정 조회에 실패했습니다."));
        }
    }

    @Operation(
            summary = "특정 일정 상세 조회",
            description = "특정 일정의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "일정 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponseDTO>> getSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("일정 상세 조회 - 사용자: {}, 일정 ID: {}",
                user.getUserId(), scheduleId);

        try {
            Schedule schedule = calendarService.getScheduleById(scheduleId, user.getUserId());
            ScheduleResponseDTO response = ScheduleResponseDTO.from(schedule);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("일정 조회 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    user.getUserId(), scheduleId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 일정에 접근할 권한이 없습니다."));

        } catch (Exception e) {
            log.error("일정 조회 실패 - 일정 ID: {}, 에러: {}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("일정을 찾을 수 없습니다."));
        }
    }

    @Operation(
            summary = "일정 수정",
            description = "기존 일정의 정보를 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "일정 수정 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "수정 권한 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponseDTO>> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("일정 수정 요청 - 사용자: {}, 일정 ID: {}",
                user.getUserId(), scheduleId);

        try {
            Schedule schedule = calendarService.updateSchedule(scheduleId, request, user.getUserId());
            ScheduleResponseDTO response = ScheduleResponseDTO.from(schedule);

            return ResponseEntity.ok(ApiResponse.success(response, "일정이 성공적으로 수정되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("일정 수정 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    user.getUserId(), scheduleId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 일정을 수정할 권한이 없습니다."));

        } catch (Exception e) {
            log.error("일정 수정 실패 - 일정 ID: {}, 에러: {}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("일정 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "일정 삭제",
            description = "기존 일정을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "일정 삭제 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @PathVariable Long scheduleId,
            @RequestParam(required = false, defaultValue = "all") String deleteType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime instanceDate,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("일정 삭제 요청 - 사용자: {}, 일정 ID: {}, 삭제 타입: {}",
                user.getUserId(), scheduleId, deleteType);

        try {
            calendarService.deleteSchedule(scheduleId, deleteType, instanceDate, user.getUserId());

            return ResponseEntity.ok(ApiResponse.success(null, "일정이 성공적으로 삭제되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("일정 삭제 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    user.getUserId(), scheduleId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 일정을 삭제할 권한이 없습니다."));

        } catch (Exception e) {
            log.error("일정 삭제 실패 - 일정 ID: {}, 에러: {}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("일정 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "그룹 일정 목록 조회",
            description = "특정 그룹의 모든 일정을 조회합니다. (필터링 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 일정 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "그룹 접근 권한 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDTO>>> getGroupSchedules(
            @PathVariable UUID groupId,
            @RequestParam(required = false, defaultValue = "MONTHLY") String viewType,
            @RequestParam(required = false) LocalDateTime dateTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal CustomOAuth2User user,
            Pageable pageable) {

        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }

        log.info("그룹 일정 조회 - 사용자: {}, 그룹 ID: {}, 달력 종류: {}, 기준 날짜: {}",
                user.getUserId(), groupId, viewType, dateTime);

        try {
            // 그룹 멤버 권한 확인
            if (!calendarService.isGroupMember(groupId, user.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("해당 그룹에 접근할 권한이 없습니다."));
            }

            List<Schedule> schedules = calendarService.getSchedulesByGroup(
                    groupId, viewType, dateTime, type, category, pageable);

            List<ScheduleResponseDTO> responses = schedules.stream()
                    .map(ScheduleResponseDTO::from)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(responses));

        } catch (Exception e) {
            log.error("그룹 일정 조회 실패 - 그룹 ID: {}, 에러: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("그룹 일정 조회에 실패했습니다."));
        }
    }
}