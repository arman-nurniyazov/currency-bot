package service;

import bot.utils.TelegramBotUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.ConversionHistory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConversionHistoryService implements TelegramBotUtils {
    ObjectMapper objectMapper = new ObjectMapper();

    // Conversions ro'yxatini olish metodi
    public List<ConversionHistory> getConversionsList() {
        ArrayList<ConversionHistory> conversionHistories = new ArrayList<>();

        try {
            conversionHistories = objectMapper.readValue(new File(URLCONVERSIONHISTORY), new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
        }

        return conversionHistories;
    }

    // Conversions ro'yxatiga conversion qo'shish metodi
    public boolean add(ConversionHistory conversionHistory) {
        List<ConversionHistory> conversionHistories = getConversionsList();

        if (conversionHistories == null)
            conversionHistories = new ArrayList<>();

        conversionHistories.add(conversionHistory);

        try {
            objectMapper.writeValue(new File(URLCONVERSIONHISTORY), conversionHistories);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
