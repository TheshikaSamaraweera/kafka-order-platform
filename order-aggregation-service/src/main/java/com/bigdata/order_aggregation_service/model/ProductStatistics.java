package com.bigdata.order_aggregation_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatistics {

    private String product;
    private long orderCount;
    private double totalRevenue;
    private double averagePrice;
    private float minPrice;
    private float maxPrice;
    private long lastUpdated;


    public ProductStatistics update(float newPrice) {
        this.orderCount++;
        this.totalRevenue += newPrice;
        this.averagePrice = this.totalRevenue / this.orderCount;

        if (this.minPrice == 0 || newPrice < this.minPrice) {
            this.minPrice = newPrice;
        }

        if (newPrice > this.maxPrice) {
            this.maxPrice = newPrice;
        }

        this.lastUpdated = System.currentTimeMillis();

        return this;
    }
}