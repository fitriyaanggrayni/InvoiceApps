package com.example.invoiceapps;

import java.io.Serializable;

public class ItemInvoice implements Serializable {

    private String namaBarang;
    private int qty;
    private double hargaSatuan;
    private double diskon;

    public ItemInvoice() {}

    public ItemInvoice(String namaBarang, int qty, double hargaSatuan, double diskon) {
        this.namaBarang = namaBarang;
        this.qty = qty;
        this.hargaSatuan = hargaSatuan;
        this.diskon = diskon;
    }

    public String getNamaBarang() {
        return namaBarang;
    }

    public int getQty() {
        return qty;
    }

    public double getHargaSatuan() {
        return hargaSatuan;
    }

    public double getDiskon() {
        return diskon;
    }
}
