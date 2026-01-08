package com.example.invoiceapps;

public class ItemInvoice {
    private String namaBarang;
    private int qty;
    private double hargaSatuan;
    private double diskon; // dalam persen

    public ItemInvoice(String namaBarang, int qty, double hargaSatuan, double diskon) {
        this.namaBarang = namaBarang;
        this.qty = qty;
        this.hargaSatuan = hargaSatuan;
        this.diskon = diskon;
    }

    public String getNamaBarang() {
        return namaBarang;
    }

    public void setNamaBarang(String namaBarang) {
        this.namaBarang = namaBarang;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getHargaSatuan() {
        return hargaSatuan;
    }

    public void setHargaSatuan(double hargaSatuan) {
        this.hargaSatuan = hargaSatuan;
    }

    public double getDiskon() {
        return diskon;
    }

    public void setDiskon(double diskon) {
        this.diskon = diskon;
    }

    // Hitung total harga sebelum diskon
    public double getTotalHarga() {
        return qty * hargaSatuan;
    }

    // Hitung jumlah diskon dalam rupiah
    public double getJumlahDiskon() {
        return getTotalHarga() * (diskon / 100);
    }

    // Hitung total setelah diskon
    public double getTotalSetelahDiskon() {
        return getTotalHarga() - getJumlahDiskon();
    }
}