package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Currency {
    private int id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("CcyNm_UZ")
    private String name;

    @JsonProperty("Rate")
    private String rate;

    @JsonProperty("Date")
    private String date;
}
