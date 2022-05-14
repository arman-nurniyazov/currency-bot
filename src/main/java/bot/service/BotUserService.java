package bot.service;

import bot.utils.RequestsAndResponses;
import bot.utils.TelegramBotUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import model.ConversionHistory;
import model.Currency;
import model.User;
import model.UserRole;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import service.ConversionHistoryService;
import service.CurrencyService;
import service.UserService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

public class BotUserService implements RequestsAndResponses, TelegramBotUtils {

    public static Map<String, Stack<SendMessage>> state = new HashMap<>();
    public static Map<String, Integer> userCurrency = new HashMap<>();
    UserService userService = new UserService();
    CurrencyService currencyService = new CurrencyService();
    ConversionHistoryService conversionHistoryService = new ConversionHistoryService();
    ObjectMapper objectMapper = new ObjectMapper();

    //start bosgandan keyingi menu ni chiqarish metodi
    public SendMessage startMenu(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        if (userService.isExist(update.getMessage().getChatId().toString()) != null) {
            StringBuilder response = new StringBuilder(RequestsAndResponses.WELCOME + "\n\n");

            for (int i = 0; i < currencyService.getCurrenciesList().size(); i++) {
                response.append(i + 1).append(". ").append(currencyService.getCurrenciesList().get(i).getName()).append("\n");
            }

            response.append("\n" + RequestsAndResponses.CHOOSECURRENCY);

            sendMessage.setText(response.toString());
            sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));

            Stack<SendMessage> stateStack = new Stack<>();
            stateStack.push(sendMessage);
            state.put(update.getMessage().getChatId().toString(), stateStack);
        } else {
            sendMessage.setText(RequestsAndResponses.SENDCONTACT);
            sendMessage.setReplyMarkup(getContactButton());
        }

        return sendMessage;
    }

    //User ga SMS kod yuborish va ro'yxatdan o'tkazuvchi metod
    public SendMessage registrationMenu(Update update) {
        if(userService.isExist(update.getMessage().getChatId().toString()) != null)
            return startMenu(update);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(RequestsAndResponses.SENDSMSCODE);

        String smsCode = sendSMSCode();

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message.creator(new com.twilio.type.PhoneNumber("+" + update.getMessage().getContact().getPhoneNumber()),
                        new com.twilio.type.PhoneNumber(TWILIO_PHONENUMBER),
                        "YOUR SMS CODE: " + smsCode).create();

        User user = new User();
        user.setChatId(update.getMessage().getChatId().toString());
        user.setFirstname(update.getMessage().getFrom().getFirstName());
        user.setLastname(update.getMessage().getFrom().getLastName());
        user.setPhoneNumber(update.getMessage().getContact().getPhoneNumber());
        user.setUserRole(UserRole.USER);
        user.setRegisteredDate(LocalDateTime.now().toString());
        user.setSmsCode(smsCode);

        userService.add(user);

        return sendMessage;
    }

    //User kiritgan Sms code ni tekshirish va javob qaytarish metodi
    public SendMessage checkCodeAndSendResponse(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        User user = userService.isExist(update.getMessage().getChatId().toString());

        if(user.getSmsCode().equals(update.getMessage().getText())) {
            return startMenu(update);
        } else {
            sendMessage.setText(RequestsAndResponses.WRONGSMSCODE);
        }

        return sendMessage;

    }

    // Valyuta menyusini chiqarish metodi
    public SendMessage currencyMenu(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        Currency currency = currencyService.getCurrency(update.getMessage().getText());

        if(currency != null) {
            userCurrency.put(update.getMessage().getChatId().toString(), currency.getId());
            sendMessage.setText(RequestsAndResponses.YOURCHOICE + currency.getName() + "\n" + RequestsAndResponses.CHOOSECOMMAND);
            sendMessage.setReplyMarkup(getCurrencyButtons());

            Stack<SendMessage> stateStack = state.get(update.getMessage().getChatId().toString());
            if(stateStack.peek().getText().endsWith(RequestsAndResponses.CHOOSECOMMAND)) {
                stateStack.pop();
            }
            stateStack.push(sendMessage);
            state.put(update.getMessage().getChatId().toString(), stateStack);
        } else {
            sendMessage.setText(RequestsAndResponses.WRONGNUMBER);
        }

        return sendMessage;
    }

    // Valyuta haqida ma'lumot yuborish metodi
    public SendMessage sendInfo(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());

        Currency currency = currencyService.infoCurrency(userCurrency.get(update.getCallbackQuery().getMessage().getChatId().toString()));

        String response = RequestsAndResponses.CURRENCYNAME +
                currency.getName() +
                "\n" +
                RequestsAndResponses.CURRENCYRATE +
                currency.getRate() +
                RequestsAndResponses.SUM +
                "\n" +
                RequestsAndResponses.DATE +
                currency.getDate();

        sendMessage.setText(response);
        sendMessage.setReplyMarkup(getBackButtons());

        Stack<SendMessage> stateStack = state.get(update.getCallbackQuery().getMessage().getChatId().toString());
        stateStack.push(sendMessage);
        state.put(update.getCallbackQuery().getMessage().getChatId().toString(), stateStack);

        return sendMessage;
    }

    // Konvertatsiya qilish tanlanganda qiymat kiritishni so'rovchi metod
    public SendMessage askAmount(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
        sendMessage.setText(RequestsAndResponses.ENTERAMOUNT);
        sendMessage.setReplyMarkup(getBackButtons());

        Stack<SendMessage> stateStack = state.get(update.getCallbackQuery().getMessage().getChatId().toString());
        stateStack.push(sendMessage);
        state.put(update.getCallbackQuery().getMessage().getChatId().toString(), stateStack);

        return sendMessage;

    }

    // Konvertatsiya qilib natijani qaytaruvchi metod
    public SendMessage sendConversion(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        String amount = update.getMessage().getText();

        try {
            int d = Integer.parseInt(amount);
            sendMessage.setText(d + " " +
                            currencyService.infoCurrency(userCurrency.get(update.getMessage().getChatId().toString())).getName() +
                            " = " + (d * Double.parseDouble(currencyService.infoCurrency(userCurrency.get(update.getMessage().getChatId().toString())).getRate())) +
                            RequestsAndResponses.SUM);
            conversionHistoryService.add(new ConversionHistory(
                    update.getMessage().getChatId().toString(),
                    LocalDateTime.now().toString(),
                    currencyService.infoCurrency(userCurrency.get(update.getMessage().getChatId().toString())).getName(),
                    d, RequestsAndResponses.SUM,
                    d * Double.parseDouble(currencyService.infoCurrency(userCurrency.get(update.getMessage().getChatId().toString())).getRate())
            ));
            sendMessage.setReplyMarkup(getBackButtons());

            Stack<SendMessage> stateStack = state.get(update.getMessage().getChatId().toString());
            if(!stateStack.peek().getText().endsWith(RequestsAndResponses.SUM)) {
                stateStack.push(sendMessage);
                state.put(update.getMessage().getChatId().toString(), stateStack);
            }
        } catch (Exception e) {
            sendMessage.setText(RequestsAndResponses.WRONGINPUTNUMBER);
        }

        return sendMessage;

    }

    // Orqaga qaytish uchun metod
    public SendMessage back(Update update) {
        state.get(update.getCallbackQuery().getMessage().getChatId().toString()).pop();
        return state.get(update.getCallbackQuery().getMessage().getChatId().toString()).peek();
    }

    // Eng boshiga qaytish uchun metod
    public SendMessage backToStartMenu(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());

        StringBuilder response = new StringBuilder(RequestsAndResponses.WELCOME + "\n\n");

        for (int i = 0; i < currencyService.getCurrenciesList().size(); i++) {
            response.append(i + 1).append(". ").append(currencyService.getCurrenciesList().get(i).getName()).append("\n");
        }

        response.append("\n" + RequestsAndResponses.CHOOSECURRENCY);

        sendMessage.setText(response.toString());

        Stack<SendMessage> stateStack = new Stack<>();
        stateStack.push(sendMessage);
        state.put(update.getCallbackQuery().getMessage().getChatId().toString(), stateStack);

        return sendMessage;
    }

    // Contact tugmasini chiqaruvchi replyKeyboardMarkup
    private ReplyKeyboardMarkup getContactButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);

        KeyboardRow keyboardRow = new KeyboardRow();

        KeyboardButton contactButton = new KeyboardButton();
        contactButton.setText(RequestsAndResponses.SENDINGCONTACT);
        contactButton.setRequestContact(true);

        keyboardRow.add(contactButton);

        keyboardRows.add(keyboardRow);

        return replyKeyboardMarkup;
    }

    // 4 xonali random smsCode yaratuvchi metod
    private String sendSMSCode() {
        Random random = new Random();
        return String.valueOf(random.nextInt(1000, 9999));
    }

    // currencyMenu dagi button larni chiqaruvchi metod
    private InlineKeyboardMarkup getCurrencyButtons() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> list = new ArrayList<>();
        inlineKeyboardMarkup.setKeyboard(list);

        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText(RequestsAndResponses.GETINFO);
        inlineKeyboardButton.setCallbackData(RequestsAndResponses.GETINFO);
        inlineKeyboardButtons.add(inlineKeyboardButton);
        list.add(inlineKeyboardButtons);

        inlineKeyboardButtons = new ArrayList<>();
        inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText(RequestsAndResponses.CHANGETOUZS);
        inlineKeyboardButton.setCallbackData(RequestsAndResponses.CHANGETOUZS);
        inlineKeyboardButtons.add(inlineKeyboardButton);
        list.add(inlineKeyboardButtons);

        inlineKeyboardButtons = new ArrayList<>();
        inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText(RequestsAndResponses.BACK);
        inlineKeyboardButton.setCallbackData(RequestsAndResponses.BACK);
        inlineKeyboardButtons.add(inlineKeyboardButton);
        list.add(inlineKeyboardButtons);

        return inlineKeyboardMarkup;
    }

    // Orqaga qaytish button larini chiqaruvchi metod
    private InlineKeyboardMarkup getBackButtons() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> list = new ArrayList<>();
        inlineKeyboardMarkup.setKeyboard(list);

        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText(RequestsAndResponses.BACK);
        inlineKeyboardButton.setCallbackData(RequestsAndResponses.BACK);
        inlineKeyboardButtons.add(inlineKeyboardButton);
        list.add(inlineKeyboardButtons);

        inlineKeyboardButtons = new ArrayList<>();
        inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText(RequestsAndResponses.BACKTOSTARTMENU);
        inlineKeyboardButton.setCallbackData(RequestsAndResponses.BACKTOSTARTMENU);
        inlineKeyboardButtons.add(inlineKeyboardButton);
        list.add(inlineKeyboardButtons);

        return inlineKeyboardMarkup;
    }

}
