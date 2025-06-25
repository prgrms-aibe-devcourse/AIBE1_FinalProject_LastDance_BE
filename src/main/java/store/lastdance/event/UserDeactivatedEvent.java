package store.lastdance.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class UserDeactivatedEvent extends ApplicationEvent {
    private final UUID userId;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public UserDeactivatedEvent(Object source, UUID userId, HttpServletRequest request, HttpServletResponse response) {
        super(source);
        this.userId = userId;
        this.request = request;
        this.response = response;
    }
}
