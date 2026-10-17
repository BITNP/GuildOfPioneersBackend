package net.bitnp.guildofpioneers.auth;

import java.net.URI;

/** Restricts login return paths to the current site's root-relative pages. */
public final class LoginRedirects {
    public static final String SESSION_ATTRIBUTE = LoginRedirects.class.getName() + ".returnTo";
    private LoginRedirects() { }

    /** Falls back to home for malformed, external or authentication-loop destinations. */
    public static String safePath(String value) {
        if (value == null || !value.startsWith("/") || value.startsWith("//")
                || value.contains("\\") || value.chars().anyMatch(c -> c <= 32 || c == 127)
                || value.toLowerCase(java.util.Locale.ROOT).matches(".*%(2f|5c|0[ad]).*")) return "/";
        try {
            URI uri = URI.create(value);
            String path = uri.getPath();
            if (uri.isAbsolute() || uri.getRawAuthority() != null || path.startsWith("/api/")
                    || path.equals("/login")) return "/";
            return value;
        } catch (IllegalArgumentException ex) {
            return "/";
        }
    }
}
