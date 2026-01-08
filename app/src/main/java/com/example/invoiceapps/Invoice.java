package com.example.invoiceapps;

import java.io.Serializable;
import java.util.List;

public class Invoice implements Serializable {
    private String noInvoice;
    private String namaCustomer;
    private String tanggal;
    private String metodePembayaran;
    private List<ItemInvoice> listBarang;
    private double total;

    public Invoice(String noInvoice, String namaCustomer, String tanggal, String metodePembayaran,
                   List<ItemInvoice> listBarang, double total) {
        this.noInvoice = noInvoice;
        this.namaCustomer = namaCustomer;
        this.tanggal = tanggal;
        this.metodePembayaran = metodePembayaran;
        this.listBarang = listBarang;
        this.total = total;
    }

    public String getNoInvoice() { return noInvoice; }
    public String getNamaCustomer() { return namaCustomer; }
    public String getTanggal() { return tanggal; }
    public String getMetodePembayaran() { return metodePembayaran; }
    public List<ItemInvoice> getListBarang() { return listBarang; }
    public double getTotal() { return total; }
}
