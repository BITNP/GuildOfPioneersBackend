package net.bitnp.guildofpioneers.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Explicit, operator-verified binding imported by the offline maintenance command. */
public record IdentityLinkRequest(@NotNull @Positive Long userId,
                                  @NotBlank @Size(max = 1024) String issuer,
                                  @NotBlank @Size(max = 255) String subject) { }
