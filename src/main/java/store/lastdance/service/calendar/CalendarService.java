package store.lastdance.service.calendar;

import org.springframework.data.domain.Pageable;
import store.lastdance.domain.calendar.Schedule;
import store.lastdance.dto.calender.request.CreateScheduleRequestDTO;
import store.lastdance.dto.calender.request.UpdateScheduleRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarService {

    Schedule createSchedule(CreateScheduleRequestDTO request, UUID userId);

    List<Schedule> getSchedulesByUser(UUID userId,
                                     String viewType,
                                     LocalDateTime dateTime,
                                     String type,
                                     String category, 
                                     UUID groupId, 
                                     Pageable pageable);

    Schedule getScheduleById(Long scheduleId, UUID userId);

    Schedule updateSchedule(Long scheduleId, UpdateScheduleRequestDTO request, UUID userId);

    void deleteSchedule(Long scheduleId, String deleteType, LocalDateTime instanceDate, UUID userId);

    List<Schedule> getSchedulesByGroup(UUID groupId, String viewType, LocalDateTime dateTime, 
                                      String type, String category, Pageable pageable);

    List<Schedule> getSchedulesByGroup(UUID groupId, LocalDateTime startDate, LocalDateTime endDate);

    List<Schedule> getSchedulesByGroupWithViewType(UUID groupId, String viewType, LocalDateTime dateTime,
                                                  String type, String category, Pageable pageable);

    List<Schedule> getRecurringInstances(Long scheduleId,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        UUID userId);

    boolean isGroupMember(UUID groupId, UUID userId);
}
