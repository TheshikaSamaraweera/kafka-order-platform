package com.bigdata.order_producer_service.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderRequest(

        @NotBlank String orderId,
        @NotBlank String product,
        @Positive double price
) {}