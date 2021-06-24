package dev.tphucnha.moneylogger.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class TotalAmountDTO implements Serializable {
    BigDecimal value;

    public TotalAmountDTO(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
