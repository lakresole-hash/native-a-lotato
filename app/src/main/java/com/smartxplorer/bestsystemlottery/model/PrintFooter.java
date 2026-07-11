package com.smartxplorer.bestsystemlottery.model;

public class PrintFooter {
    private String footer_text_1;
    private String footer_text_2;
    private String footer_text_3;

    public PrintFooter(){

    }
    public PrintFooter(String footer_text_1, String footer_text_2, String footer_text_3) {
        this.footer_text_1 = footer_text_1;
        this.footer_text_2 = footer_text_2;
        this.footer_text_3 = footer_text_3;
    }

    public String getFooter_text_1() {
        return footer_text_1;
    }

    public void setFooter_text_1(String footer_text_1) {
        this.footer_text_1 = footer_text_1;
    }

    public String getFooter_text_2() {
        return footer_text_2;
    }

    public void setFooter_text_2(String footer_text_2) {
        this.footer_text_2 = footer_text_2;
    }

    public String getFooter_text_3() {
        return footer_text_3;
    }

    public void setFooter_text_3(String footer_text_3) {
        this.footer_text_3 = footer_text_3;
    }
}
