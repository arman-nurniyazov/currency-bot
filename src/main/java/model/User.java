package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    private String chatId;
    private String firstname;
    private String lastname;
    private String phoneNumber;
    private UserRole userRole;
    private String registeredDate;
    private String smsCode;
}
