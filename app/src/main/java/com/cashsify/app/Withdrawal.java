package com.cashsify.app;

public class Withdrawal {

    private double amount;
    private String upiId;
    private String status;

    public Withdrawal(double amount, String upiId, String status) {
        this.amount = amount;
        this.upiId = upiId;
        this.status = status;
    }

    public double getAmount() {
        return amount;
    }

    public String getUpiId() {
        return upiId;
    }

    public String getStatus() {
        return status;
    }
}
