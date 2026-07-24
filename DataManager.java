package com.smartxplorer.bestsystemlottery.model;

public class BodyTicket {
    private String lottery;
    private String type;
    private String optionLot;
    private String numbers;
    private String amount;

    public BodyTicket() {

    }

    public BodyTicket(String lottery, String type, String optionLot,
                      String numbers, String amount) {
        this.lottery = lottery;
        this.type = type;
        this.optionLot = optionLot;
        this.numbers = numbers;
        this.amount = amount;
    }

    public String getLottery() {
        return lottery;
    }

    public void setLottery(String lottery) {
        this.lottery = lottery;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOptionLot() {
        return optionLot;
    }

    public void setOptionLot(String optionLot) {
        this.optionLot = optionLot;
    }

    public String getNumbers() {
        return numbers;
    }

    public void setNumbers(String numbers) {
        this.numbers = numbers;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
