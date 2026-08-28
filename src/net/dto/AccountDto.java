package net.dto;

import model.user_data.User;

public class AccountDto {

    public String username;
    public String nickname;
    public String email;
    public String gender;
    public String profilePicture;
    public String securityQuestion;

    public static AccountDto of(User user) {
        AccountDto dto = new AccountDto();
        dto.username = user.username;
        dto.nickname = user.nickname;
        dto.email = user.email;
        dto.gender = user.gender;
        dto.profilePicture = user.profilePicture;
        dto.securityQuestion = user.securityQuestion;
        return dto;
    }

    public void applyTo(User user) {
        user.username = username;
        user.nickname = nickname;
        user.email = email;
        user.gender = gender;
        if (profilePicture != null && !profilePicture.isEmpty()) user.profilePicture = profilePicture;
        user.securityQuestion = securityQuestion;
    }
}
