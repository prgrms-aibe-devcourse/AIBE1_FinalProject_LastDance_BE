package store.lastdance.controller.calendar;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.lastdance.domain.calendar.Schedule;
import store.lastdance.dto.calender.request.CreateScheduleRequestDTO;
import store.lastdance.dto.calender.request.UpdateScheduleRequestDTO;
import store.lastdance.dto.calender.response.ScheduleResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.CurrentUser;
import store.lastdance.security.UserPrincipal;
import store.lastdance.service.calendar.CalendarService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 새 일정 생성
     * POST /api/v1/schedules
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponseDTO>> createSchedule(
            @Valid @RequestBody CreateScheduleRequestDTO request,
            @CurrentUser UserPrincipal userPrincipal) {

        log.info("일정 생성 요청 - 사용자: {}, 제목: {}",
                userPrincipal.getId(), request.getTitle());

        try {
            Schedule schedule = calendarService.createSchedule(request, userPrincipal.getId());
            ScheduleResponseDTO response = ScheduleResponseDTO.from(schedule);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "일정이 성공적으로 생성되었습니다."));

        } catch (Exception e) {
            log.error("일정 생성 실패 - 사용자: {}, 에러: {}",
                    userPrincipal.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("일정 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자의 모든 일정 조회 (필터링 가능)
     * GET /api/v1/schedules/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDTO>>> getMySchedules(
            @RequestParam(required = false, defaultValue = "MONTHLY") String viewType,
            @RequestParam(required = false) LocalDateTime dateTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID groupId,
            @CurrentUser UserPrincipal userPrincipal,
            Pageable pageable) {

        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }

        log.info("일정 목록 조회 - 사용자: {}, 달력 종류: {}, 기준 날짜: {}",
                userPrincipal.getId(), viewType, dateTime);

        try {
            List<Schedule> schedules = calendarService.getSchedulesByUser(
                    userPrincipal.getId(), viewType, dateTime, type, category, groupId, pageable);

            List<ScheduleResponseDTO> responses = schedules.stream()
                    .map(ScheduleResponseDTO::from)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(responses));

        } catch (Exception e) {
            log.error("일정 조회 실패 - 사용자: {}, 에러: {}",
                    userPrincipal.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("일정 조회에 실패했습니다."));
        }
    }

    /**
     * 특정 일정 상세 조회
     * GET /api/v1/schedules/{scheduleId}
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponseDTO>> getSchedule(
            @PathVariable Long scheduleId,
            @CurrentUser UserPrincipal userPrincipal) {

        log.info("일정 상세 조회 - 사용자: {}, 일정 ID: {}",
                userPrincipal.getId(), scheduleId);

        try {
            Schedule schedule = calendarService.getScheduleById(scheduleId, userPrincipal.getId());
            ScheduleResponseDTO response = ScheduleResponseDTO.from(schedule);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("일정 조회 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    userPrincipal.getId(), scheduleId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 일정에 접근할 권한이 없습니다."));

        } catch (Exception e) {
            log.error("일정 조회 실패 - 일정 ID: {}, 에러: {}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("일정을 찾을 수 없습니다."));
        }
    }

    /**
     * 일정 수정
     * PATCH /api/v1/schedules/{scheduleId}
     */
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponseDTO>> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequestDTO request,
            @CurrentUser UserPrincipal userPrincipal) {

        log.info("일정 수정 요청 - 사용자: {}, 일정 ID: {}",
                userPrincipal.getId(), scheduleId);

        try {
            Schedule schedule = calendarService.updateSchedule(scheduleId, request, userPrincipal.getId());
            ScheduleResponseDTO response = ScheduleResponseDTO.from(schedule);

            return ResponseEntity.ok(ApiResponse.success(response, "일정이 성공적으로 수정되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("일정 수정 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    userPrincipal.getId(), scheduleId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 일정을 수정할 권한이 없습니다."));

        } catch (Exception e) {
            log.error("일정 수정 실패 - 일정 ID: {}, 에러: {}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("일정 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 일정 삭제
     * DELETE /api/v1/schedules/{scheduleId}
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @PathVariable Long scheduleId,
            @RequestParam(required = false, defaultValue = "all") String deleteType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime instanceDate,
            @CurrentUser UserPrincipal userPrincipal) {

        log.info("일정 삭제 요청 - 사용자: {}, 일정 ID: {}, 삭제 타입: {}",
                userPrincipal.getId(), scheduleId, deleteType);

        try {
            calendarService.deleteSchedule(scheduleId, deleteType, instanceDate, userPrincipal.getId());

            return ResponseEntity.ok(ApiResponse.success(null, "일정이 성공적으로 삭제되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("일정 삭제 실패 - 권한 없음: 사용자 {}, 일정 ID: {}",
                    userPrincipal.getId(), scheduleId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 일정을 삭제할 권한이 없습니다."));

        } catch (Exception e) {
            log.error("일정 삭제 실패 - 일정 ID: {}, 에러: {}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("일정 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 그룹 일정 조회
     * GET /api/v1/schedules/groups/{groupId}
     */
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDTO>>> getGroupSchedules(
            @PathVariable UUID groupId,
            @RequestParam(required = false, defaultValue = "MONTHLY") String viewType,
            @RequestParam(required = false) LocalDateTime dateTime,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @CurrentUser UserPrincipal userPrincipal,
            Pageable pageable) {

        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }

        log.info("그룹 일정 조회 - 사용자: {}, 그룹 ID: {}, 달력 종류: {}, 기준 날짜: {}",
                userPrincipal.getId(), groupId, viewType, dateTime);

        try {
            // 그룹 멤버 권한 확인
            if (!calendarService.isGroupMember(groupId, userPrincipal.getId())) {
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