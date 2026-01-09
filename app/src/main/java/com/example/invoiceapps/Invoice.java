package com.example.invoiceapps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Invoice implements Serializable {

    private String id;
    private String noInvoice;
    private String namaCustomer;
    private String noTelepon;
    private String alamat;
    private String tanggal;
    private String metodePembayaran;

    private double subTotal;
    private double totalDiskon;
    private double pajak;          // persen
    private double biayaPengiriman;
    private double total;

    private Date createdAt;

    private List<ItemInvoice> items = new ArrayList<>();

    // ===== EMPTY CONSTRUCTOR (Firestore) =====
    public Invoice() {}

    // ===== BASIC CONSTRUCTOR =====
    public Invoice(String noInvoice, String namaCustomer, String tanggal, double total) {
        this.noInvoice = noInvoice;
        this.namaCustomer = namaCustomer;
        this.tanggal = tanggal;
        this.total = total;
    }

    // ===== GETTER & SETTER =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNoInvoice() { return noInvoice; }
    public String getNamaCustomer() { return namaCustomer; }
    public String getNoTelepon() { return noTelepon; }
    public String getAlamat() { return alamat; }
    public String getTanggal() { return tanggal; }
    public String getMetodePembayaran() { return metodePembayaran; }

    public double getSubTotal() { return subTotal; }
    public double getTotalDiskon() { return totalDiskon; }
    public double getPajak() { return pajak; }
    public double getBiayaPengiriman() { return biayaPengiriman; }
    public double getTotal() { return total; }

    public Date getCreatedAt() { return createdAt; }

    public List<ItemInvoice> getItems() { return items; }
    public void setItems(List<ItemInvoice> items) { this.items = items; }

    public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }
    public void setAlamat(String alamat) { this.alamat = alamat; }
    public void setMetodePembayaran(String metodePembayaran) { this.metodePembayaran = metodePembayaran; }

    public void setSubTotal(double subTotal) { this.subTotal = subTotal; }
    public void setTotalDiskon(double totalDiskon) { this.totalDiskon = totalDiskon; }
    public void setPajak(double pajak) { this.pajak = pajak; }
    public void setBiayaPengiriman(double biayaPengiriman) { this.biayaPengiriman = biayaPengiriman; }
    public void setTotal(double total) { this.total = total; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    // ===== ADD ITEM (WAJIB) =====
    public void addItem(ItemInvoice item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }
}
