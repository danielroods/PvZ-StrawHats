package net.dto;

import model.user_data.User;

public class AccountDto {

    public String accountId;
    public String username;
    public String nickname;
    public String email;
    public String gender;
    public String profilePicture;
    public String securityQuestion;
    public String passwordHash;
    public String securityAnswerHash;
    public String syncedPasswordHash;

    public static AccountDto of(User user) {
        AccountDto dto = new AccountDto();
        dto.accountId = user.accountId();
        dto.username = user.username;
        dto.nickname = user.nickname;
        dto.email = user.email;
        dto.gender = user.gender;
        dto.profilePicture = user.profilePicture;
        dto.securityQuestion = user.securityQuestion;
        dto.passwordHash = user.passwordHash;
        dto.securityAnswerHash = user.securityAnswerHash;
        dto.syncedPasswordHash = user.syncedPasswordHash;
        return dto;
    }

    public void applyTo(User user) {
        if (accountId != null && !accountId.isBlank()) user.accountId = accountId;
        if (username != null && !username.isBlank()) user.username = username;
        if (nickname != null) user.nickname = nickname;
        if (email != null) user.email = email;
        if (gender != null) user.gender = gender;
        if (profilePicture != null && !profilePicture.isEmpty()) {
            user.profilePicture = profilePicture;
        }
        if (securityQuestion != null) user.securityQuestion = securityQuestion;
        if (passwordHash != null && !passwordHash.isBlank()) user.passwordHash = passwordHash;
        if (securityAnswerHash != null) user.securityAnswerHash = securityAnswerHash;
    }

    public void applyProfileTo(User user) {
        String requested = username;
        username = null;
        applyTo(user);
        username = requested;
    }
}
