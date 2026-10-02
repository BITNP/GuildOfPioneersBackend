package net.bitnp.guildofpioneers.auth.dto;

import net.bitnp.guildofpioneers.user.AuthResponse;

/** Exposes authentication and admission separately without exposing provider tokens. */
public record AuthStatusResponse(String state, AuthResponse user, ProfileSuggestions suggestions,
                                 boolean bootstrapAdmin) {
    /** Initial local profile suggestions; they are never used for identity matching. */
    public record ProfileSuggestions(String userName, String email) { }
}
