package com.realestate.admin.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/** Formats numbers with Western digits and separators regardless of the
 *  request's locale - Thymeleaf's #numbers.formatDecimal otherwise renders
 *  Eastern Arabic digits (٠١٢٣) under an Arabic locale even with explicit
 *  COMMA/POINT separator arguments (those control separators, not the
 *  digit script itself). */
@Component("num")
public class NumberFormatService {

    public String money(BigDecimal value) {
        if (value == null) return "0";
        return String.format(Locale.US, "%,.0f", value);
    }

    public String money(Number value) {
        if (value == null) return "0";
        return String.format(Locale.US, "%,.0f", value.doubleValue());
    }

    public String pct(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
