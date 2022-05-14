package bot;

import bot.service.BotAdminService;
import bot.service.BotUserService;
import bot.utils.RequestsAndResponses;
import bot.utils.TelegramBotUtils;
import lombok.SneakyThrows;

import model.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import service.UserService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBot extends TelegramLongPollingBot implements TelegramBotUtils, RequestsAndResponses {

    BotUserService botUserService = new BotUserService();
    BotAdminService botAdminService = new BotAdminService();
    UserService userService = new UserService();

    @Override
    public String getBotToken() {
        return TelegramBotUtils.BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return TelegramBotUtils.BOT_USERNAME;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if(botAdminService.isAdmin(update)) {
                Message message = update.getMessage();
                if(message.hasText()) {
                    String text = message.getText();
                    switch (text) {
                        case START -> execute(botAdminService.menu(update));
                        case LISTOFUSERS -> execute(botAdminService.usersInfo(update));
                        case LISTOFCONVERSIONS -> execute(botAdminService.conversionsInfo(update));
                        case LISTOFCURRENCIES -> execute(botAdminService.currenciesInfo(update));
                        case SENDNEWSORADS -> execute(botAdminService.writeText(update));
                        default -> sendText(update);
                    }
                }
            }
            else {
                Message message = update.getMessage();
                if (message.hasText()) {
                    String text = message.getText();
                    if (text.equals(START)) {
                        execute(botUserService.startMenu(update));
                    } else {
                        if (BotUserService.state.containsKey(message.getChatId().toString()) && (BotUserService.state.get(message.getChatId().toString()).peek().getText().equals(ENTERAMOUNT) ||
                                BotUserService.state.get(message.getChatId().toString()).peek().getText().endsWith(SUM))) {
                            execute(botUserService.sendConversion(update));
                        } else if (BotUserService.state.containsKey(message.getChatId().toString())) {
                            execute(botUserService.currencyMenu(update));
                        } else {
                            execute(botUserService.checkCodeAndSendResponse(update));
                        }
                    }
                } else if (message.hasContact()) {
                    execute(botUserService.registrationMenu(update));
                }
            }
        }
        else if(update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            switch (data) {
                case GETINFO -> execute(botUserService.sendInfo(update));
                case CHANGETOUZS -> execute(botUserService.askAmount(update));
                case BACK -> execute(botUserService.back(update));
                case BACKTOSTARTMENU -> execute(botUserService.backToStartMenu(update));
            }
        }
    }

    // Yangilik/Reklama ni foydalanuvchilarga yuborish metodi
    @SneakyThrows
    public void sendText(Update update) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());


        if(BotAdminService.state.get(update.getMessage().getChatId().toString()).equals(WRITETEXT)) {
            List<User> users = userService.getUsersList();
            ExecutorService executorService = Executors.newCachedThreadPool();

            executorService.execute(() -> {
                for (User user : users) {
                    sendMessage.setChatId(user.getChatId());
                    sendMessage.setText(update.getMessage().getText());
                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else {
            sendMessage.setText(WRONGOPTION);
            execute(sendMessage);
        }

    }

}
