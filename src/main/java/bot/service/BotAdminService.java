package bot.service;

import bot.utils.RequestsAndResponses;
import bot.utils.TelegramBotUtils;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.SneakyThrows;
import model.ConversionHistory;
import model.Currency;
import model.User;
import com.itextpdf.text.Document;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import service.ConversionHistoryService;
import service.CurrencyService;
import service.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotAdminService implements RequestsAndResponses, TelegramBotUtils {

    public static Map<String, String> state = new HashMap<>();

    UserService userService = new UserService();
    CurrencyService currencyService = new CurrencyService();
    ConversionHistoryService conversionHistoryService = new ConversionHistoryService();

    // User ni admin yoki admin emasligini aniqlash metodi
    public boolean isAdmin(Update update) {
        return userService.check(update.getMessage().getChatId().toString());
    }

    // Menu ni chiqarish metodi
    public SendMessage menu(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(CHOOSECOMMAND);
        sendMessage.setReplyMarkup(getButtons());

        state.put(update.getMessage().getChatId().toString(), CHOOSECOMMAND);

        return sendMessage;
    }

    // Fo'ydalanuvchilar ro'yxatini yuboruvchi metod
    @SneakyThrows
    public SendDocument usersInfo(Update update) {
        List<User> users = userService.getUsersList();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(update.getMessage().getChatId().toString());
        sendDocument.setCaption(LISTOFUSERS);

        FileInputStream fileInputStream = new FileInputStream(PATHUSERSINFO);

        HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
        HSSFSheet sheet = workbook.getSheet("sheet1");

        HSSFRow row = sheet.getRow(0);
        row.getCell(0).setCellValue("chatId");
        row.getCell(1).setCellValue("firstname");
        row.getCell(2).setCellValue("lastname");
        row.getCell(3).setCellValue("phoneNumber");
        row.getCell(4).setCellValue("userRole");
        row.getCell(5).setCellValue("registeredDate");
        row.getCell(6).setCellValue("smsCode");

        int index = 1;
        for(User user: users) {
            HSSFRow row1 = sheet.getRow(index++);

            row1.getCell(0).setCellValue(user.getChatId());
            row1.getCell(1).setCellValue(user.getFirstname());
            row1.getCell(2).setCellValue(user.getLastname());
            row1.getCell(3).setCellValue(user.getPhoneNumber());
            row1.getCell(4).setCellValue(user.getUserRole().toString());
            row1.getCell(5).setCellValue(user.getRegisteredDate());
            row1.getCell(6).setCellValue(user.getSmsCode());
        }

        FileOutputStream fileOutputStream = new FileOutputStream(PATHUSERSINFO);
        workbook.write(fileOutputStream);

        fileInputStream.close();
        fileOutputStream.close();
        workbook.close();

        sendDocument.setDocument(new InputFile(new File(PATHUSERSINFO)));

        state.put(update.getMessage().getChatId().toString(), LISTOFUSERS);

        return sendDocument;

    }

    // Amalga oshirilgan barcha konvertatsiyalarni yuboruvchi metod
    @SneakyThrows
    public SendDocument conversionsInfo(Update update) {
        List<ConversionHistory> conversionHistories = conversionHistoryService.getConversionsList();

        if(conversionHistories == null)
            conversionHistories = new ArrayList<>();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(update.getMessage().getChatId().toString());
        sendDocument.setCaption(LISTOFCONVERSIONS);

        FileInputStream fileInputStream = new FileInputStream(PATHCONVERSIONSINFO);

        HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
        HSSFSheet sheet = workbook.getSheet("sheet1");

        HSSFRow row = sheet.getRow(0);
        row.getCell(0).setCellValue("chatId");
        row.getCell(1).setCellValue("date");
        row.getCell(2).setCellValue("from");
        row.getCell(3).setCellValue("amount");
        row.getCell(4).setCellValue("to");
        row.getCell(5).setCellValue("total");

        int index = 1;
        for(ConversionHistory conversionHistory :conversionHistories) {
            HSSFRow row1 = sheet.getRow(index++);

            row1.getCell(0).setCellValue(conversionHistory.getChatId());
            row1.getCell(1).setCellValue(conversionHistory.getDate());
            row1.getCell(2).setCellValue(conversionHistory.getFrom());
            row1.getCell(3).setCellValue(conversionHistory.getAmount());
            row1.getCell(4).setCellValue(conversionHistory.getTo());
            row1.getCell(5).setCellValue(conversionHistory.getTotal());
        }

        FileOutputStream fileOutputStream = new FileOutputStream(PATHCONVERSIONSINFO);
        workbook.write(fileOutputStream);

        fileInputStream.close();
        fileOutputStream.close();
        workbook.close();

        sendDocument.setDocument(new InputFile(new File(PATHCONVERSIONSINFO)));

        state.put(update.getMessage().getChatId().toString(), LISTOFCONVERSIONS);

        return sendDocument;

    }

    // So'mning boshqa valyutalarga nisbatan kurslarini yuboruvchi metod
    @SneakyThrows
    public SendDocument currenciesInfo(Update update) {
        List<Currency> currencies = currencyService.getCurrenciesList();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(update.getMessage().getChatId().toString());
        sendDocument.setCaption(LISTOFCURRENCIES);

        Document doc = new Document();

        try {
            PdfWriter pdfWriter = PdfWriter.getInstance(doc, new FileOutputStream(PATHCURRENCIESINFO));
            doc.open();
            doc.add(new Paragraph(RequestsAndResponses.DATE + currencies.get(0).getDate()));
            doc.add(new Paragraph("-----------------------------------------------------"));
            for (Currency currency : currencies) {
                doc.add(new Paragraph("1 " + currency.getName() + " : " + currency.getRate() + RequestsAndResponses.SUM));
            }
            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendDocument.setDocument(new InputFile(new File(PATHCURRENCIESINFO)));

        state.put(update.getMessage().getChatId().toString(), LISTOFCURRENCIES);

        return sendDocument;
    }

    // Yangilik/Reklama yozish uchun metod
    public SendMessage writeText(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(WRITETEXT);

        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));

        state.put(update.getMessage().getChatId().toString(), WRITETEXT);

        return sendMessage;
    }

    // Button larni chiqaruvchi metod
    private ReplyKeyboardMarkup getButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(LISTOFUSERS);
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(LISTOFCONVERSIONS);
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(LISTOFCURRENCIES);
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(SENDNEWSORADS);
        keyboardRowList.add(keyboardRow);

        return replyKeyboardMarkup;
    }



}
