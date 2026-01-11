package com.example.invoiceapps;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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

import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

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
    private ArrayAdapter<String> pembayaranAdapter;
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

    // ====== EDIT MODE ======
    private boolean isEdit = false;
    private String invoiceId;

    @Override
    protected void onResume() {
        super.onResume();

        if (isEdit && invoiceId != null) {
            loadInvoiceForEdit(invoiceId);
        }
    }

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

        // Cek jika ini mode edit dan ada invoiceId dari Intent
        Intent intent = getIntent();
        isEdit = intent.getBooleanExtra("isEdit", false);
        if (isEdit) {
            invoiceId = intent.getStringExtra("invoiceId");
            if (invoiceId != null && !invoiceId.isEmpty()) {

            } else {
                Toast.makeText(this, "Invoice ID tidak ditemukan untuk edit", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            generateNoInvoice();
            setTanggalHariIni();
        }
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
        pembayaranAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, metode);
        etPembayaran.setAdapter(pembayaranAdapter);
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

        try {
            int qty = Integer.parseInt(qtyStr);
            double harga = Double.parseDouble(hargaStr);
            double diskon = diskonStr.isEmpty() ? 0 : Double.parseDouble(diskonStr);

            listInvoice.add(new ItemInvoice(nama, qty, harga, diskon));
            invoiceAdapter.notifyItemInserted(listInvoice.size() - 1);

            findViewById(R.id.cardDaftarBarang).setVisibility(View.VISIBLE);

            etNamaBarang.setText("");
            etQty.setText("");
            etHargaSatuan.setText("");
            etDiskon.setText("0");

            hitungTotal();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Format angka tidak valid", Toast.LENGTH_SHORT).show();
        }
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();

        // ===== LOGO =====
        data.put("logoUri", logoUri != null ? logoUri.toString() : "");

        // ===== USAHA =====
        data.put("namaUsaha", etNamaUsaha.getText().toString());
        data.put("alamatUsaha", etAlamatUsaha.getText().toString());
        data.put("telpUsaha", etTelpUsaha.getText().toString());

        // ===== INVOICE =====
        data.put("noInvoice", etNoInvoice.getText().toString().trim());
        data.put("namaCustomer", etNama.getText().toString().trim());
        data.put("tanggal", etTanggal.getText().toString().trim());
        data.put("metodePembayaran", etPembayaran.getText().toString().trim());
        data.put("alamatCustomer", etAlamat.getText().toString().trim());
        data.put("telpCustomer", etNoTelepon.getText().toString().trim());

        double pajak = etPajak.getText().toString().isEmpty() ? 0 :
                Double.parseDouble(etPajak.getText().toString());

        double ongkir = etBiayaPengiriman.getText().toString().isEmpty() ? 0 :
                Double.parseDouble(etBiayaPengiriman.getText().toString());

        data.put("pajak", pajak);
        data.put("biayaPengiriman", ongkir);

        // ===== TOTAL =====
        data.put("subTotal", parse(tvSubTotal.getText().toString()));
        data.put("totalDiskon", parse(tvDiskonTotal.getText().toString()));
        data.put("total", parse(tvTotal.getText().toString()));

        data.put("userId", user.getUid());

        if (!isEdit) {
            data.put("createdAt", new java.util.Date());
        }

        // ===== ITEMS =====
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

        // ===== COMPANY =====
        Map<String, Object> company = new HashMap<>();
        company.put("namaUsaha", etNamaUsaha.getText().toString());
        company.put("alamat", etAlamatUsaha.getText().toString());
        company.put("telp", etTelpUsaha.getText().toString());
        company.put("logoUri", logoUri != null ? logoUri.toString() : "");
        data.put("company", company);

        // ===== SIMPAN / UPDATE =====
        if (isEdit && invoiceId != null) {
            db.collection("invoices")
                    .document(invoiceId)
                    .set(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Invoice berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } else {
            db.collection("invoices")
                    .add(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Invoice berhasil disimpan", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Gagal simpan: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }
    }


    private void simpanInvoiceEdit(String invoiceId) {

        Map<String, Object> data = new HashMap<>();

        data.put("noInvoice", etNoInvoice.getText().toString());
        data.put("namaCustomer", etNama.getText().toString());
        data.put("alamatCustomer", etAlamat.getText().toString());
        data.put("telpCustomer", etNoTelepon.getText().toString());
        data.put("tanggal", etTanggal.getText().toString());
        data.put("metodePembayaran", etPembayaran.getText().toString());
        data.put("pajak", Double.parseDouble(etPajak.getText().toString()));
        data.put("biayaPengiriman",
                Double.parseDouble(etBiayaPengiriman.getText().toString()));

        // ===== ITEMS =====
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

        // ===== COMPANY =====
        Map<String, Object> company = new HashMap<>();
        company.put("namaUsaha", etNamaUsaha.getText().toString());
        company.put("alamat", etAlamatUsaha.getText().toString());
        company.put("telp", etTelpUsaha.getText().toString());
        company.put("logoUri", logoUri != null ? logoUri.toString() : "");
        data.put("company", company);

        // ===== UPDATE FIRESTORE =====
        FirebaseFirestore.getInstance()
                .collection("invoices")
                .document(invoiceId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Invoice berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }


    // ================= LOAD DATA EDIT =================
    @SuppressWarnings("unchecked")
    private void loadInvoiceForEdit(String invoiceId) {
        FirebaseFirestore.getInstance()
                .collection("invoices")
                .document(invoiceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Data invoice tidak ditemukan", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // ===== DATA INVOICE =====
                    etNoInvoice.setText(doc.getString("noInvoice"));
                    etNama.setText(doc.getString("namaCustomer"));
                    etTanggal.setText(doc.getString("tanggal"));
                    etPembayaran.setText(doc.getString("metodePembayaran"));
                    etAlamat.setText(doc.getString("alamatCustomer"));
                    etNoTelepon.setText(doc.getString("telpCustomer"));

                    Double pajak = doc.getDouble("pajak");
                    etPajak.setText(pajak != null ? String.valueOf(pajak) : "0");

                    Double ongkir = doc.getDouble("biayaPengiriman");
                    etBiayaPengiriman.setText(ongkir != null ? String.valueOf(ongkir) : "0");

                    // ===== DATA USAHA (COMPANY) =====
                    Object companyObj = doc.get("company");
                    if (companyObj instanceof Map) {
                        Map<String, Object> company = (Map<String, Object>) companyObj;

                        etNamaUsaha.setText(
                                company.get("namaUsaha") != null ? company.get("namaUsaha").toString() : ""
                        );

                        etAlamatUsaha.setText(
                                company.get("alamat") != null ? company.get("alamat").toString() : ""
                        );

                        etTelpUsaha.setText(
                                company.get("telp") != null ? company.get("telp").toString() : ""
                        );

                        String logo = company.get("logoUri") != null
                                ? company.get("logoUri").toString()
                                : "";

                        if (!logo.isEmpty()) {
                            logoUri = Uri.parse(logo);
                            imgLogo.setImageURI(logoUri);
                        }
                    }

                    // ===== ITEM BARANG =====
                    listInvoice.clear();
                    Object itemsObj = doc.get("items");
                    if (itemsObj instanceof List) {
                        List<Map<String, Object>> items =
                                (List<Map<String, Object>>) itemsObj;

                        for (Map<String, Object> m : items) {
                            listInvoice.add(new ItemInvoice(
                                    String.valueOf(m.get("namaBarang")),
                                    ((Number) m.get("qty")).intValue(),
                                    ((Number) m.get("hargaSatuan")).doubleValue(),
                                    ((Number) m.get("diskon")).doubleValue()
                            ));
                        }
                    }

                    invoiceAdapter.notifyDataSetChanged();

                    // ===== HITUNG ULANG TOTAL =====
                    hitungTotal();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Gagal memuat data invoice: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }


    // ================= UTIL =================
    private void generateNoInvoice() {
        String t = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        etNoInvoice.setText("INV-" + t + "-" + (int) (Math.random() * 1000));
    }

    private void setTanggalHariIni() {
        etTanggal.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime()));
    }

    private String rupiah(double v) {
        if (v == 0) return "Rp 0";
        try {
            return rupiahFormat.format(v);
        } catch (Exception e) {
            return "Rp 0";
        }
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
        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            hitungTotal();
        }

    };

}
