package store.lastdance.dto.calendar;

import java.time.LocalDateTime;

public record DateRangeDTO(LocalDateTime start, LocalDateTime end) {

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }
}
