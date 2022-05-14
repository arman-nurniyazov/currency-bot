package service;

import bot.utils.RequestsAndResponses;
import bot.utils.TelegramBotUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Currency;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class CurrencyService implements RequestsAndResponses, TelegramBotUtils {
    // Faqat run bo'lgan paytda api bilan currency larni olib kelyapdi
    ArrayList<Currency> currencies = null;

    {
        try {
            URL url = new URL(URL);
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            currencies = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (Exception e) {
            System.out.println(NOCONNECTION);
        }
    }

    // Currency lar ro'yxatidan currency ni index orqali olish metodi
    public Currency getCurrency(String index) {
        for(int i = 0; i < getCurrenciesList().size(); i++) {
            if(String.valueOf(i + 1).equals(index))
                return getCurrenciesList().get(i);
        }
        return null;
    }

    // Currency lar ro'yxatini olish metodi
    public List<Currency> getCurrenciesList() {
        return currencies;
    }

    // Currency haqida ma'lumot yuborish metodi
    public Currency infoCurrency(int id) {

        for(int i = 0; i < getCurrenciesList().size(); i++) {
            if(getCurrenciesList().get(i).getId() == id) {
                return getCurrenciesList().get(i);
            }
        }

        return null;
    }

}
