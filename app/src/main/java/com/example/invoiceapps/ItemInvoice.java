package com.example.invoiceapps;

import java.io.Serializable;

public class ItemInvoice implements Serializable {

    private String namaBarang;
    private int qty;
    private double hargaSatuan;
    private double diskon; // persen (0 - 100)

    // WAJIB untuk Firebase / Serializable
    public ItemInvoice() {}

    public ItemInvoice(String namaBarang, int qty, double hargaSatuan, double diskon) {
        this.namaBarang = namaBarang;
        this.qty = qty;
        this.hargaSatuan = hargaSatuan;
        this.diskon = diskon;
    }

    // ===== GETTER =====
    public String getNamaBarang() {
        return namaBarang != null ? namaBarang : "";
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

    // ===== HELPER =====
    public double getTotalHarga() {
        double total = qty * hargaSatuan;
        if (diskon > 0) {
            total = total * (1 - diskon / 100);
        }
        return total;
    }
}
