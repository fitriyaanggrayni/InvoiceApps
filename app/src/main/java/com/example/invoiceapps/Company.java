package com.example.invoiceapps;

public class Company {
    private String namaUsaha;
    private String alamat;
    private String telp;
    private String logoUri;

    public Company() {}

    public Company(String namaUsaha, String alamat, String telp, String logoUri) {
        this.namaUsaha = namaUsaha;
        this.alamat = alamat;
        this.telp = telp;
        this.logoUri = logoUri;
    }

    public String getNamaUsaha() { return namaUsaha; }
    public String getAlamat() { return alamat; }
    public String getTelp() { return telp; }
    public String getLogoUri() { return logoUri; }
}

