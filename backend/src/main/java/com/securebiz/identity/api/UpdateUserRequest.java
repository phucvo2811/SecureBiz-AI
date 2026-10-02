package com.securebiz.identity.api;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record UpdateUserRequest(@NotBlank @Email @Size(max=254) String email,
                                @Pattern(regexp="OFFICIAL|TRAINEE|IT|MANAGER|OTHER") String accountType) {}
