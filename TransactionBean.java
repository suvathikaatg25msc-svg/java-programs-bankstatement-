package com.wipro.bankstatement.bean;

import java.time.LocalDate;

/**
 * Represents a single parsed bank transaction.
 */
public class TransactionBean {

    private LocalDate date;
    private String description;
    private double amount;
    private String type; // DEBIT or CREDIT

    public TransactionBean() {
    }

    public TransactionBean(LocalDate date, String description, double amount, String type) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TransactionBean{" +
                "date=" + date +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                '}';
    }
}
