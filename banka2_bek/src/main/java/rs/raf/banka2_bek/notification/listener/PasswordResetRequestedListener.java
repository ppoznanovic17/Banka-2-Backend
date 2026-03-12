package rs.raf.banka2_bek.notification.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.raf.banka2_bek.auth.model.PasswordResetRequestedEvent;
import rs.raf.banka2_bek.notification.service.MailNotificationService;

@Component
public class PasswordResetRequestedListener {

    private final MailNotificationService mailNotificationService;

    public PasswordResetRequestedListener(MailNotificationService mailNotificationService) {
        this.mailNotificationService = mailNotificationService;
    }

    @Async
    @EventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        mailNotificationService.sendPasswordResetMail(event.getEmail(), event.getToken());
    }
}

