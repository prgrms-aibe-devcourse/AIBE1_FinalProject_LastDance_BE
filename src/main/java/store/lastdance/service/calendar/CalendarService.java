package store.lastdance.service.calendar;

import org.springframework.data.domain.Pageable;
import store.lastdance.domain.calendar.Schedule;
import store.lastdance.dto.calender.request.CreateScheduleRequestDTO;
import store.lastdance.dto.calender.request.UpdateScheduleRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarService {

    /**
     * 새 일정 생성
     */
    Schedule createSchedule(CreateScheduleRequestDTO request, UUID userId);

    /**
     * 사용자의 일정 목록 조회
     */
    List<Schedule> getSchedulesByUser(UUID userId,
                                     String viewType,
                                     LocalDateTime dateTime,
                                     String type,
                                     String category, 
                                     UUID groupId, 
                                     Pageable pageable);

    /**
     * 특정 일정 조회 (권한 확인)
     */
    Schedule getScheduleById(Long scheduleId, UUID userId);

    /**
     * 일정 수정
     */
    Schedule updateSchedule(Long scheduleId, UpdateScheduleRequestDTO request, UUID userId);

    /**
     * 일정 삭제 (반복 일정 처리 포함)
     */
    void deleteSchedule(Long scheduleId, String deleteType, LocalDateTime instanceDate, UUID userId);

    /**
     * 그룹 일정 조회 (뷰타입으로)
     */
    List<Schedule> getSchedulesByGroup(UUID groupId, String viewType, LocalDateTime dateTime, 
                                      String type, String category, Pageable pageable);

    /**
     * 그룹 일정 조회 (날짜 범위로)
     */
    List<Schedule> getSchedulesByGroup(UUID groupId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 그룹 일정 조회 (뷰타입으로) - 별명 메서드
     */
    List<Schedule> getSchedulesByGroupWithViewType(UUID groupId, String viewType, LocalDateTime dateTime, 
                                                  String type, String category, Pageable pageable);

    /**
     * 반복 일정 인스턴스 조회
     */
    List<Schedule> getRecurringInstances(Long scheduleId, 
                                        LocalDateTime startDate, 
                                        LocalDateTime endDate, 
                                        UUID userId);

    /**
     * 그룹 멤버 권한 확인
     */
    boolean isGroupMember(UUID groupId, UUID userId);
}
