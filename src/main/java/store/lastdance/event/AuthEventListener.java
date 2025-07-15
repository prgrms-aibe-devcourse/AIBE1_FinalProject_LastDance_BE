package store.lastdance.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import store.lastdance.service.auth.AuthService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {

    private final AuthService authService;

    @EventListener
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        authService.logout(event.getRequest(), event.getResponse());
    }
}
