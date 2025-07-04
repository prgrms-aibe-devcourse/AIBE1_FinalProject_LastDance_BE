package store.lastdance.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RedirectUriResolver {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    private static final String DEV_URI = "http://localhost:5173";
    private static final String PROD_URI = "https://woori-zip.lastdance.store";

    private final List<String> allowedRedirectUris = Arrays.asList(
            DEV_URI, PROD_URI
    );

    public String resolveRedirectUri(HttpServletRequest request) {
        if ("dev".equals(activeProfile)) {
            return DEV_URI;
        }

        String requestRedirectUri = request.getParameter("redirect_uri");

        if (requestRedirectUri != null && allowedRedirectUris.contains(requestRedirectUri)) {
            return requestRedirectUri;
        }

        return PROD_URI;
    }
}
