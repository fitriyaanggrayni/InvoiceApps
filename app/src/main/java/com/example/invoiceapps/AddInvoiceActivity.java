package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddInvoiceActivity extends AppCompatActivity {

    // ====== HEADER USAHA ======
    private TextInputEditText etNamaUsaha, etAlamatUsaha, etTelpUsaha;
    private ImageView imgLogo;
    private Button btnPilihLogo;
    private Uri logoUri;
    private static final int PICK_LOGO = 1001;

    // ====== CUSTOMER ======
    private TextInputEditText etNama, etNoTelepon, etAlamat;

    // ====== INVOICE ======
    private TextInputEditText etNoInvoice, etTanggal;
    private AutoCompleteTextView etPembayaran;

    // ====== ITEM ======
    private TextInputEditText etNamaBarang, etQty, etHargaSatuan, etDiskon;
    private Button btnTambahBarang;
    private RecyclerView rvBarang;

    // ====== TOTAL ======
    private TextView tvSubTotal, tvDiskonTotal, tvPajak, tvTotal;
    private TextInputEditText etPajak, etBiayaPengiriman;

    private Button btnSimpan;

    // ====== DATA ======
    private InvoiceAdapter invoiceAdapter;
    private List<ItemInvoice> listInvoice = new ArrayList<>();
    private DecimalFormat rupiahFormat;

    // ====== PEMBAYARAN ======
    private String namaAdmin = "-";
    private String namaPenerima = "-";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_invoice);

        initViews();
        setupRupiah();
        setupMetodePembayaran();
        setupTanggal();
        setupRecycler();
        setupListener();
    }

    // ================= INIT =================
    private void initViews() {

        imgLogo = findViewById(R.id.imgLogo);
        btnPilihLogo = findViewById(R.id.btnPilihLogo);

        etNamaUsaha = findViewById(R.id.etNamaUsaha);
        etAlamatUsaha = findViewById(R.id.etAlamatUsaha);
        etTelpUsaha = findViewById(R.id.etTelpUsaha);

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

    // ================= SETUP =================
    private void setupRupiah() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("id", "ID"));
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        rupiahFormat = new DecimalFormat("Rp #,###", s);
    }

    private void setupMetodePembayaran() {
        String[] metode = {"Tunai", "Transfer Bank", "E-Wallet", "Kartu Debit", "Kartu Kredit", "COD"};
        etPembayaran.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, metode));
    }

    private void setupTanggal() {
        etTanggal.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                c.set(y, m, d);
                etTanggal.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupRecycler() {
        invoiceAdapter = new InvoiceAdapter(listInvoice, pos -> {
            listInvoice.remove(pos);
            invoiceAdapter.notifyItemRemoved(pos);
            hitungTotal();
        }, true);

        rvBarang.setLayoutManager(new LinearLayoutManager(this));
        rvBarang.setAdapter(invoiceAdapter);
    }

    private void setupListener() {

        btnPilihLogo.setOnClickListener(v -> pilihLogo());
        btnTambahBarang.setOnClickListener(v -> tambahBarang());
        btnSimpan.setOnClickListener(v -> simpanInvoice());

        etPajak.addTextChangedListener(simpleWatcher);
        etBiayaPengiriman.addTextChangedListener(simpleWatcher);
    }

    // ================= LOGO =================
    private void pilihLogo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK_LOGO);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_LOGO && res == RESULT_OK && data != null) {
            logoUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        logoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            imgLogo.setImageURI(logoUri);
        }
    }

    // ================= ITEM =================
    private void tambahBarang() {
        String nama = etNamaBarang.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String hargaStr = etHargaSatuan.getText().toString().trim();
        String diskonStr = etDiskon.getText().toString().trim();

        if (nama.isEmpty() || qtyStr.isEmpty() || hargaStr.isEmpty()) {
            Toast.makeText(this, "Nama, Qty, dan Harga wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        double harga = Double.parseDouble(hargaStr);
        double diskon = diskonStr.isEmpty() ? 0 : Double.parseDouble(diskonStr);

        listInvoice.add(new ItemInvoice(nama, qty, harga, diskon));
        invoiceAdapter.notifyItemInserted(listInvoice.size() - 1);

        etNamaBarang.setText("");
        etQty.setText("");
        etHargaSatuan.setText("");
        etDiskon.setText("");

        hitungTotal();
    }


    // ================= HITUNG =================
    private void hitungTotal() {
        if (listInvoice.isEmpty()) {
            tvSubTotal.setText("Rp 0");
            tvDiskonTotal.setText("Rp 0");
            tvPajak.setText("Rp 0");
            tvTotal.setText("Rp 0");
            return;
        }

        double sub = 0, disk = 0;

        for (ItemInvoice i : listInvoice) {
            double t = i.getQty() * i.getHargaSatuan();
            sub += t;
            disk += t * (i.getDiskon() / 100);
        }

        double pajak = etPajak.getText().toString().isEmpty() ? 0 : Double.parseDouble(etPajak.getText().toString());
        double kirim = etBiayaPengiriman.getText().toString().isEmpty() ? 0 : Double.parseDouble(etBiayaPengiriman.getText().toString());

        double total = sub - disk + ((sub - disk) * pajak / 100) + kirim;

        tvSubTotal.setText(rupiah(sub));
        tvDiskonTotal.setText(rupiah(disk));
        tvPajak.setText(rupiah((sub - disk) * pajak / 100));
        tvTotal.setText(rupiah(total));
    }

    // ================= SIMPAN =================
    private void simpanInvoice() {
        if (listInvoice.isEmpty()) {
            Toast.makeText(this, "Tambahkan minimal 1 barang", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();

        data.put("logoUri", logoUri != null ? logoUri.toString() : "");
        data.put("namaUsaha", etNamaUsaha.getText().toString());
        data.put("alamatUsaha", etAlamatUsaha.getText().toString());
        data.put("telpUsaha", etTelpUsaha.getText().toString());

        data.put("noInvoice", etNoInvoice.getText() != null ? etNoInvoice.getText().toString().trim() : "");
        data.put("namaCustomer", etNama.getText() != null ? etNama.getText().toString().trim() : "");
        data.put("tanggal", etTanggal.getText() != null ? etTanggal.getText().toString().trim() : "");
        data.put("metodePembayaran", etPembayaran.getText() != null ? etPembayaran.getText().toString().trim() : "");
        data.put("alamatCustomer", etAlamat.getText() != null ? etAlamat.getText().toString().trim() : "");
        data.put("telpCustomer", etNoTelepon.getText() != null ? etNoTelepon.getText().toString().trim() : "");


        data.put("pajak", etPajak.getText().toString().isEmpty() ? 0 :
                Double.parseDouble(etPajak.getText().toString()));

        data.put("biayaPengiriman", etBiayaPengiriman.getText().toString().isEmpty() ? 0 :
                Double.parseDouble(etBiayaPengiriman.getText().toString()));


        data.put("namaAdmin", namaAdmin);
        data.put("namaPenerima", namaPenerima);

        data.put("subTotal", parse(tvSubTotal.getText().toString()));
        data.put("totalDiskon", parse(tvDiskonTotal.getText().toString()));
        data.put("total", parse(tvTotal.getText().toString()));
        data.put("userId", user.getUid());
        data.put("createdAt", new java.util.Date());

        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemInvoice i : listInvoice) {
            Map<String, Object> m = new HashMap<>();
            m.put("namaBarang", i.getNamaBarang());
            m.put("qty", i.getQty());
            m.put("hargaSatuan", i.getHargaSatuan());
            m.put("diskon", i.getDiskon());
            items.add(m);
        }
        data.put("items", items);

        // ================= COMPANY PROFILE (PER INVOICE) =================
        Map<String, Object> company = new HashMap<>();
        company.put("namaUsaha", etNamaUsaha.getText().toString());
        company.put("alamat", etAlamatUsaha.getText().toString());
        company.put("telp", etTelpUsaha.getText().toString());
        company.put("logoUri", logoUri != null ? logoUri.toString() : "");

// simpan ke invoice
        data.put("company", company);


        FirebaseFirestore.getInstance()
                .collection("invoices")
                .add(data)
                .addOnSuccessListener(d -> {
                    Toast.makeText(this, "Invoice berhasil disimpan", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // ================= UTIL =================
    private void generateNoInvoice() {
        String t = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        etNoInvoice.setText("INV-" + t + "-" + (int)(Math.random() * 1000));
    }

    private void setTanggalHariIni() {
        etTanggal.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime()));
    }

    private String rupiah(double v) {
        return v == 0 ? "Rp 0" : rupiahFormat.format(v);
    }

    private double parse(String r) {
        if (r == null || r.isEmpty()) return 0;
        try {
            return Double.parseDouble(
                    r.replace("Rp", "")
                            .replace(".", "")
                            .replace(",", ".")
                            .trim()
            );
        } catch (NumberFormatException e) {
            return 0;
        }
    }



    private final TextWatcher simpleWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { hitungTotal(); }
    };
}
