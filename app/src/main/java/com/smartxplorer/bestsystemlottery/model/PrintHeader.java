package com.smartxplorer.bestsystemlottery.model;

public class PrintHeader {

    private String merchantName;
    private String agentAddress;
    private String phoneMerchant;
    private String Status;
    private String agentName;
    private String ticketNo;
    private String dates;
    private String salesReport;
    private String ticketPending;
    private String winningNumber;
    private String winningTicket;
    private String winningPrize;
    private String historicalSales;

    public PrintHeader() {
    }

    public PrintHeader(String merchantName, String agentAddress,
                       String phoneMerchant, String status,
                       String agentName, String ticketNo,
                       String dates, String salesReport,
                       String ticketPending, String winningNumber,
                       String winningTicket, String winningPrize,
                       String historicalSales) {
        this.merchantName = merchantName;
        this.agentAddress = agentAddress;
        this.phoneMerchant = phoneMerchant;
        Status = status;
        this.agentName = agentName;
        this.ticketNo = ticketNo;
        this.dates = dates;
        this.salesReport = salesReport;
        this.ticketPending = ticketPending;
        this.winningNumber = winningNumber;
        this.winningTicket = winningTicket;
        this.winningPrize = winningPrize;
        this.historicalSales = historicalSales;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public String getPhoneMerchant() {
        return phoneMerchant;
    }

    public void setPhoneMerchant(String phoneMerchant) {
        this.phoneMerchant = phoneMerchant;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getDates() {
        return dates;
    }

    public void setDates(String dates) {
        this.dates = dates;
    }

    public String getSalesReport() {
        return salesReport;
    }

    public void setSalesReport(String salesReport) {
        this.salesReport = salesReport;
    }

    public String getTicketPending() {
        return ticketPending;
    }

    public void setTicketPending(String ticketPending) {
        this.ticketPending = ticketPending;
    }

    public String getWinningNumber() {
        return winningNumber;
    }

    public void setWinningNumber(String winningNumber) {
        this.winningNumber = winningNumber;
    }

    public String getWinningTicket() {
        return winningTicket;
    }

    public void setWinningTicket(String winningTicket) {
        this.winningTicket = winningTicket;
    }

    public String getWinningPrize() {
        return winningPrize;
    }

    public void setWinningPrize(String winningPrize) {
        this.winningPrize = winningPrize;
    }

    public String getHistoricalSales() {
        return historicalSales;
    }

    public void setHistoricalSales(String historicalSales) {
        this.historicalSales = historicalSales;
    }
}
