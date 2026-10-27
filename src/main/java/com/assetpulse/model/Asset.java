package com.assetpulse.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents one IT asset stored in the database.
 */
public class Asset {
    private final int assetId;
    private final String assetTag;
    private final String assetName;
    private final String category;
    private final String assignedTo;
    private final String status;
    private final BigDecimal purchaseCost;
    private final LocalDate purchaseDate;

    public Asset(
            int assetId,
            String assetTag,
            String assetName,
            String category,
            String assignedTo,
            String status,
            BigDecimal purchaseCost,
            LocalDate purchaseDate
    ) {
        this.assetId = assetId;
        this.assetTag = assetTag;
        this.assetName = assetName;
        this.category = category;
        this.assignedTo = assignedTo;
        this.status = status;
        this.purchaseCost = purchaseCost;
        this.purchaseDate = purchaseDate;
    }

    public int getAssetId() { return assetId; }
    public String getAssetTag() { return assetTag; }
    public String getAssetName() { return assetName; }
    public String getCategory() { return category; }
    public String getAssignedTo() { return assignedTo; }
    public String getStatus() { return status; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
}
