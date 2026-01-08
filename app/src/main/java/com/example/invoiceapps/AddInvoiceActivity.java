package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddInvoiceActivity extends AppCompatActivity {

    // Informasi Pelanggan
    private TextInputEditText etNama, etNoTelepon, etAlamat;

    // Informasi Invoice
    private TextInputEditText etNoInvoice, etTanggal;
    private AutoCompleteTextView etPembayaran;

    // Detail Barang
    private TextInputEditText etNamaBarang, etQty, etHargaSatuan, etDiskon;
    private Button btnTambahBarang;

    // Daftar Barang
    private RecyclerView rvBarang;
    private InvoiceAdapter invoiceAdapter;
    private List<ItemInvoice> listInvoice;

    // Ringkasan
    private TextView tvSubTotal, tvDiskonTotal, tvPajak, tvTotal;
    private TextInputEditText etPajak, etBiayaPengiriman;

    // Tombol Simpan
    private Button btnSimpan;

    // Format Mata Uang
    private DecimalFormat rupiahFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_invoice);

        initViews();
        setupFormatRupiah();
        setupMetodePembayaran();
        setupDatePicker();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        etNama = findViewById(R.id.etNama);
        etNoTelepon = findViewById(R.id.etNoTelepon);
        etAlamat = findViewById(R.id.etAlamat);

        etNoInvoice = findViewById(R.id.etNoInvoice);
        etTanggal = findViewById(R.id.etTanggal);
        etPembayaran = findViewById(R.id.etPembayaran);

        etNamaBarang = findViewById(R.id.etNamaBarang);
        etQty = findViewById(R.id.etQty);
        etHargaSatuan = findViewById(R.id.etHargaSatuan);
        etDiskon = findViewById(R.id.etDiskon);
        btnTambahBarang = findViewById(R.id.btnTambahBarang);

        rvBarang = findViewById(R.id.rvBarang);

        tvSubTotal = findViewById(R.id.tvSubTotal);
        tvDiskonTotal = findViewById(R.id.tvDiskonTotal);
        tvPajak = findViewById(R.id.tvPajak);
        tvTotal = findViewById(R.id.tvTotal);
        etPajak = findViewById(R.id.etPajak);
        etBiayaPengiriman = findViewById(R.id.etBiayaPengiriman);

        btnSimpan = findViewById(R.id.btnSimpan);

        generateNoInvoice();
        setTanggalHariIni();
    }

    private void setupFormatRupiah() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        rupiahFormat = new DecimalFormat("Rp #,###", symbols);
    }

    private void setupMetodePembayaran() {
        String[] metodePembayaran = {"Tunai", "Transfer Bank", "Kartu Kredit", "Kartu Debit", "E-Wallet", "COD"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, metodePembayaran);
        etPembayaran.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etTanggal.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddInvoiceActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etTanggal.setText(sdf.format(selectedDate.getTime()));
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void setupRecyclerView() {
        listInvoice = new ArrayList<>();
        invoiceAdapter = new InvoiceAdapter(listInvoice, position -> {
            listInvoice.remove(position);
            invoiceAdapter.notifyItemRemoved(position);
            hitungTotal();
            Toast.makeText(AddInvoiceActivity.this, "Barang dihapus", Toast.LENGTH_SHORT).show();
        });

        rvBarang.setLayoutManager(new LinearLayoutManager(this));
        rvBarang.setAdapter(invoiceAdapter);
    }

    private void setupListeners() {
        btnTambahBarang.setOnClickListener(v -> tambahBarang());

        etPajak.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { hitungTotal(); }
        });

        etBiayaPengiriman.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { hitungTotal(); }
        });

        btnSimpan.setOnClickListener(v -> simpanInvoice());
    }

    private void tambahBarang() {
        String namaBarang = etNamaBarang.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String hargaStr = etHargaSatuan.getText().toString().trim();
        String diskonStr = etDiskon.getText().toString().trim();

        if (namaBarang.isEmpty()) { etNamaBarang.setError("Nama barang harus diisi"); etNamaBarang.requestFocus(); return; }
        if (qtyStr.isEmpty()) { etQty.setError("Qty harus diisi"); etQty.requestFocus(); return; }
        if (hargaStr.isEmpty()) { etHargaSatuan.setError("Harga satuan harus diisi"); etHargaSatuan.requestFocus(); return; }

        int qty = Integer.parseInt(qtyStr);
        double hargaSatuan = Double.parseDouble(hargaStr);
        double diskon = diskonStr.isEmpty() ? 0 : Double.parseDouble(diskonStr);

        ItemInvoice itemInvoice = new ItemInvoice(namaBarang, qty, hargaSatuan, diskon);
        listInvoice.add(itemInvoice);
        invoiceAdapter.notifyItemInserted(listInvoice.size() - 1);

        etNamaBarang.setText(""); etQty.setText(""); etHargaSatuan.setText(""); etDiskon.setText("");
        hitungTotal();
        Toast.makeText(this, "Barang ditambahkan", Toast.LENGTH_SHORT).show();
    }

    private void hitungTotal() {
        double subTotal = 0, totalDiskon = 0;

        for (ItemInvoice item : listInvoice) {
            double hargaTotal = item.getQty() * item.getHargaSatuan();
            double diskonItem = hargaTotal * (item.getDiskon() / 100);
            subTotal += hargaTotal;
            totalDiskon += diskonItem;
        }

        double persentasePajak = etPajak.getText().toString().trim().isEmpty() ? 0 :
                Double.parseDouble(etPajak.getText().toString().trim());
        double totalPajak = (subTotal - totalDiskon) * (persentasePajak / 100);

        double biayaPengiriman = etBiayaPengiriman.getText().toString().trim().isEmpty() ? 0 :
                Double.parseDouble(etBiayaPengiriman.getText().toString().trim());

        double totalAkhir = subTotal - totalDiskon + totalPajak + biayaPengiriman;

        tvSubTotal.setText(formatRupiah(subTotal));
        tvDiskonTotal.setText(formatRupiah(totalDiskon));
        tvPajak.setText(formatRupiah(totalPajak));
        tvTotal.setText(formatRupiah(totalAkhir));
    }

    private void simpanInvoice() {
        if (etNama.getText().toString().trim().isEmpty()) { etNama.setError("Nama harus diisi"); etNama.requestFocus(); return; }
        if (etNoTelepon.getText().toString().trim().isEmpty()) { etNoTelepon.setError("No telepon harus diisi"); etNoTelepon.requestFocus(); return; }
        if (etAlamat.getText().toString().trim().isEmpty()) { etAlamat.setError("Alamat harus diisi"); etAlamat.requestFocus(); return; }
        if (etNoInvoice.getText().toString().trim().isEmpty()) { etNoInvoice.setError("No invoice harus diisi"); etNoInvoice.requestFocus(); return; }
        if (etTanggal.getText().toString().trim().isEmpty()) { etTanggal.setError("Tanggal harus diisi"); etTanggal.requestFocus(); return; }
        if (etPembayaran.getText().toString().trim().isEmpty()) { etPembayaran.setError("Metode pembayaran harus dipilih"); etPembayaran.requestFocus(); return; }
        if (listInvoice.isEmpty()) { Toast.makeText(this, "Tambahkan minimal 1 barang", Toast.LENGTH_SHORT).show(); return; }

        // Buat objek Invoice
        Invoice invoice = new Invoice(
                etNoInvoice.getText().toString().trim(),
                etNama.getText().toString().trim(),
                etTanggal.getText().toString().trim(),
                etPembayaran.getText().toString().trim(),
                listInvoice,
                parseDouble(tvTotal.getText().toString())
        );

        // Kembalikan ke MainActivity
        Intent intent = new Intent();
        intent.putExtra("newInvoice", invoice);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void generateNoInvoice() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String tanggal = sdf.format(Calendar.getInstance().getTime());
        int random = (int) (Math.random() * 1000);
        etNoInvoice.setText("INV-" + tanggal + "-" + String.format("%03d", random));
    }

    private void setTanggalHariIni() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etTanggal.setText(sdf.format(Calendar.getInstance().getTime()));
    }

    private String formatRupiah(double nilai) {
        if (nilai == 0) return "Rp 0";
        return rupiahFormat.format(nilai);
    }

    private double parseDouble(String rupiah) {
        // Hapus Rp, titik, spasi
        return Double.parseDouble(rupiah.replaceAll("[Rp.\\s]", "").replace(",", "."));
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
