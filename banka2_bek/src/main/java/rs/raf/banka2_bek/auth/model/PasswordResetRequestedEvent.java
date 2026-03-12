package rs.raf.banka2_bek.auth.model;

public class PasswordResetRequestedEvent {

    private final String email;
    private final String token;

    public PasswordResetRequestedEvent(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }
}