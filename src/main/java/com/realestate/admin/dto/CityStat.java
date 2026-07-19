package com.realestate.admin.dto;

import java.math.BigDecimal;

public record CityStat(String city, long count, double percentage, BigDecimal avgPrice) {
}
