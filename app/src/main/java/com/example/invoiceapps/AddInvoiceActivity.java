package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
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

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class AddInvoiceActivity extends AppCompatActivity {

    // Views
    private TextInputEditText etNama, etNoTelepon, etAlamat;
    private TextInputEditText etNoInvoice, etTanggal;
    private AutoCompleteTextView etPembayaran;
    private TextInputEditText etNamaBarang, etQty, etHargaSatuan, etDiskon;
    private Button btnTambahBarang, btnSimpan;
    private RecyclerView rvBarang;
    private TextView tvSubTotal, tvDiskonTotal, tvPajak, tvTotal;
    private TextInputEditText etPajak, etBiayaPengiriman;

    private InvoiceAdapter invoiceAdapter;
    private List<ItemInvoice> listInvoice;
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
        String[] metode = {"Tunai", "Transfer Bank", "Kartu Kredit", "Kartu Debit", "E-Wallet", "COD"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, metode);
        etPembayaran.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etTanggal.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(
                    AddInvoiceActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        c.set(year, month, dayOfMonth);
                        etTanggal.setText(sdf.format(c.getTime()));
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupRecyclerView() {
        listInvoice = new ArrayList<>();
        invoiceAdapter = new InvoiceAdapter(listInvoice, position -> {
            listInvoice.remove(position);
            invoiceAdapter.notifyItemRemoved(position);
            hitungTotal();
            Toast.makeText(this, "Barang dihapus", Toast.LENGTH_SHORT).show();
        }, true); // <-- tombol delete tampil di AddInvoiceActivity

        rvBarang.setLayoutManager(new LinearLayoutManager(this));
        rvBarang.setAdapter(invoiceAdapter);
    }

    private void setupListeners() {
        btnTambahBarang.setOnClickListener(v -> tambahBarang());

        etPajak.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { hitungTotal(); }
        });
        etBiayaPengiriman.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { hitungTotal(); }
        });

        btnSimpan.setOnClickListener(v -> simpanInvoice());
    }

    private void tambahBarang() {
        String nama = etNamaBarang.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String hargaStr = etHargaSatuan.getText().toString().trim();
        String diskonStr = etDiskon.getText().toString().trim();

        if (nama.isEmpty()) { etNamaBarang.setError("Nama barang harus diisi"); return; }
        if (qtyStr.isEmpty()) { etQty.setError("Qty harus diisi"); return; }
        if (hargaStr.isEmpty()) { etHargaSatuan.setError("Harga harus diisi"); return; }

        int qty = Integer.parseInt(qtyStr);
        double harga = Double.parseDouble(hargaStr);
        double diskon = diskonStr.isEmpty() ? 0 : Double.parseDouble(diskonStr);

        listInvoice.add(new ItemInvoice(nama, qty, harga, diskon));
        invoiceAdapter.notifyItemInserted(listInvoice.size() - 1);

        etNamaBarang.setText(""); etQty.setText(""); etHargaSatuan.setText(""); etDiskon.setText("");
        hitungTotal();
    }

    private void hitungTotal() {
        double subTotal = 0, totalDiskon = 0;
        for (ItemInvoice item : listInvoice) {
            double total = item.getQty() * item.getHargaSatuan();
            double diskon = total * (item.getDiskon() / 100);
            subTotal += total;
            totalDiskon += diskon;
        }

        double pajak = etPajak.getText().toString().trim().isEmpty() ? 0 :
                Double.parseDouble(etPajak.getText().toString().trim());
        double biayaKirim = etBiayaPengiriman.getText().toString().trim().isEmpty() ? 0 :
                Double.parseDouble(etBiayaPengiriman.getText().toString().trim());

        double totalAkhir = subTotal - totalDiskon + (subTotal - totalDiskon) * (pajak / 100) + biayaKirim;

        tvSubTotal.setText(formatRupiah(subTotal));
        tvDiskonTotal.setText(formatRupiah(totalDiskon));
        tvPajak.setText(formatRupiah((subTotal - totalDiskon) * (pajak / 100)));
        tvTotal.setText(formatRupiah(totalAkhir));
    }

    private void simpanInvoice() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("noInvoice", etNoInvoice.getText().toString());
        data.put("namaCustomer", etNama.getText().toString());
        data.put("tanggal", etTanggal.getText().toString());
        data.put("metodePembayaran", etPembayaran.getText().toString());
        data.put("total", parseDouble(tvTotal.getText().toString()));
        data.put("userId", user.getUid());
        data.put("createdAt", new java.util.Date());

        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemInvoice item : listInvoice) {
            Map<String, Object> map = new HashMap<>();
            map.put("namaBarang", item.getNamaBarang());
            map.put("qty", item.getQty());
            map.put("hargaSatuan", item.getHargaSatuan());
            map.put("diskon", item.getDiskon());
            items.add(map);
        }
        data.put("items", items);

        FirebaseFirestore.getInstance()
                .collection("invoices")
                .add(data)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Invoice disimpan", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
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
        return nilai == 0 ? "Rp 0" : rupiahFormat.format(nilai);
    }

    private double parseDouble(String rupiah) {
        return Double.parseDouble(rupiah.replaceAll("[Rp.\\s]", "").replace(",", "."));
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
