package service;

import bot.utils.TelegramBotUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.User;
import model.UserRole;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UserService implements TelegramBotUtils {

    ObjectMapper objectMapper = new ObjectMapper();

    // User lar ro'yxatini olish metodi
    public List<User> getUsersList() {
        ArrayList<User> users = new ArrayList<>();

        try {
            users = objectMapper.readValue(new File(URLUSER), new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }

    // User lar ro'yxatiga user qo'shish metodi
    public boolean add(User user) {
        List<User> users = getUsersList();

        if (users == null)
            users = new ArrayList<>();

        users.add(user);

        try {
            objectMapper.writeValue(new File(URLUSER), users);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    //User lar ro'yxatida user bor-yo'qligini chatId orqali tekshirish metodi
    public User isExist(String chatId) {
        List<User> users = getUsersList();

        if (users == null)
            users = new ArrayList<>();

        for (User user : users) {
            if (user.getChatId().equals(chatId))
                return user;
        }

        return null;

    }

    // User ni admin yoki admin emasligini chatId si orqali tekshirish metodi
    public boolean check(String chatId) {
        List<User> users = getUsersList();

        if (users == null)
            users = new ArrayList<>();

        User user = null;

        for (User user1 : users) {
            if (user1.getChatId().equals(chatId))
                user = user1;
        }

        return user != null && user.getUserRole().equals(UserRole.ADMIN);

    }

}
