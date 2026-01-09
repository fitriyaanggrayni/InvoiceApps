package com.example.invoiceapps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Invoice implements Serializable {

    private String id;
    private String noInvoice;
    private String namaCustomer;
    private String tanggal;
    private double total;
    private List<ItemInvoice> items;

    public Invoice() {
        items = new ArrayList<>();
    }

    public Invoice(String noInvoice, String namaCustomer, String tanggal, double total) {
        this.noInvoice = noInvoice;
        this.namaCustomer = namaCustomer;
        this.tanggal = tanggal;
        this.total = total;
        this.items = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNoInvoice() {
        return noInvoice;
    }

    public String getNamaCustomer() {
        return namaCustomer;
    }

    public String getTanggal() {
        return tanggal;
    }

    public double getTotal() {
        return total;
    }

    public List<ItemInvoice> getItems() {
        return items;
    }

    public void addItem(ItemInvoice item) {
        items.add(item);
    }
}
