package store.lastdance.domain.user;

public enum OAuthProvider {
    KAKAO,
    GOOGLE,
    NAVER;
    
    public boolean isNaverMail() {
        return this == NAVER;
    }
}
