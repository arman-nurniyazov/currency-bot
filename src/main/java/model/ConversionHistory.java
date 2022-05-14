package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConversionHistory {
    private String chatId;
    private String date;
    private String from;
    private int amount;
    private String to;
    private double total;
}
