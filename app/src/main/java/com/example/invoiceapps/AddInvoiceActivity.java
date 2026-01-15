package com.example.invoiceapps;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.util.Base64;
import android.widget.AutoCompleteTextView;


public class AddInvoiceActivity extends AppCompatActivity {

    // ====== HEADER USAHA ======
    private TextInputEditText etNamaUsaha, etAlamatUsaha, etTelpUsaha;
    private ImageView imgLogo;
    private Button btnPilihLogo;
    private Uri logoUri;
    private String logoBase64 = "";
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
    private final List<ItemInvoice> listInvoice = new ArrayList<>();
    private DecimalFormat rupiahFormat;

    private final String namaPenerima = "-";

    // ====== EDIT MODE ======
    private boolean isEdit = false;
    private String invoiceId;
    private View loadingOverlay;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_invoice);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        initViews();
        setupRupiah();
        setupMetodePembayaran();
        setupTanggal();
        setupRecycler();
        setupListener();

        Intent intent = getIntent();
        isEdit = intent.getBooleanExtra("isEdit", false);

        if (isEdit) {
            invoiceId = intent.getStringExtra("invoiceId");
            if (invoiceId != null && !invoiceId.isEmpty()) {
                setTitle(getString(R.string.edit_invoice));
                btnSimpan.setText(getString(R.string.update_invoice));
                etNoInvoice.setEnabled(false);
                etNoInvoice.setFocusable(false);
                loadInvoiceForEdit(invoiceId);
            } else {
                Toast.makeText(this, "Invoice ID tidak ditemukan untuk edit", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            setTitle(getString(R.string.tambah_invoice));
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

        loadingOverlay = findViewById(R.id.loadingOverlay);

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, metode);
        etPembayaran.setAdapter(adapter);
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
    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;

        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSimpan.setEnabled(!show);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_LOGO && resultCode == RESULT_OK && data != null) {
            try {
                logoUri = data.getData();
                getContentResolver().takePersistableUriPermission(logoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(logoUri));
                imgLogo.setImageBitmap(bitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                logoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            } catch (Exception e) {
                Toast.makeText(this, "Gagal memilih logo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

        int qty;
        double harga, diskon;

        try {
            qty = Integer.parseInt(qtyStr);
            harga = Double.parseDouble(hargaStr);
            diskon = diskonStr.isEmpty() ? 0 : Double.parseDouble(diskonStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Format angka tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tambah item ke list
        listInvoice.add(new ItemInvoice(nama, qty, harga, diskon));
        invoiceAdapter.notifyItemInserted(listInvoice.size() - 1);

        // Reset input form
        etNamaBarang.setText("");
        etQty.setText("");
        etHargaSatuan.setText("");
        etDiskon.setText("0");

        etNamaBarang.requestFocus(); // Fokus ke nama barang

        // Tampilkan card/list barang jika perlu (optional)
        View cardDaftarBarang = findViewById(R.id.cardDaftarBarang);
        if (cardDaftarBarang != null && cardDaftarBarang.getVisibility() != View.VISIBLE) {
            cardDaftarBarang.setVisibility(View.VISIBLE);
        }

        // Hitung ulang total
        hitungTotal();
    }



    // ================= HITUNG =================
    private void hitungTotal() {
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

        showLoading(true);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("logoBase64", logoBase64); // simpan Base64
        data.put("namaUsaha", etNamaUsaha.getText().toString());
        data.put("alamatUsaha", etAlamatUsaha.getText().toString());
        data.put("telpUsaha", etTelpUsaha.getText().toString());

        data.put("noInvoice", etNoInvoice.getText().toString().trim());
        data.put("namaCustomer", etNama.getText().toString().trim());
        data.put("tanggal", etTanggal.getText().toString().trim());
        data.put("metodePembayaran", etPembayaran.getText().toString().trim());
        data.put("alamatCustomer", etAlamat.getText().toString().trim());
        data.put("telpCustomer", etNoTelepon.getText().toString().trim());

        data.put("pajak", etPajak.getText().toString().isEmpty() ? 0 : Double.parseDouble(etPajak.getText().toString()));
        data.put("biayaPengiriman", etBiayaPengiriman.getText().toString().isEmpty() ? 0 : Double.parseDouble(etBiayaPengiriman.getText().toString()));

        data.put("namaAdmin", "-");
        data.put("namaPenerima", namaPenerima);
        data.put("subTotal", parse(tvSubTotal.getText().toString()));
        data.put("totalDiskon", parse(tvDiskonTotal.getText().toString()));
        data.put("total", parse(tvTotal.getText().toString()));
        data.put("userId", user.getUid());

        if (isEdit) data.put("updatedAt", FieldValue.serverTimestamp());
        else data.put("createdAt", FieldValue.serverTimestamp());

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

        // Company profile
        Map<String, Object> company = new HashMap<>();
        company.put("namaUsaha", etNamaUsaha.getText().toString());
        company.put("alamat", etAlamatUsaha.getText().toString());
        company.put("telp", etTelpUsaha.getText().toString());
        company.put("logoBase64", logoBase64);

        data.put("company", company);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (isEdit && invoiceId != null) {
            db.collection("invoices").document(invoiceId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> {
                        showLoading(false);
                        Toast.makeText(this, "Invoice berhasil diperbarui", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, InvoiceDetailActivity.class);
                        intent.putExtra("invoiceId", invoiceId);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });


        } else {
            db.collection("invoices").add(data)
                    .addOnSuccessListener(d -> {
                        showLoading(false);
                        Toast.makeText(this, "Invoice berhasil disimpan", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Gagal simpan: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });


        }
    }

    // ================= LOAD DATA EDIT =================
    @SuppressWarnings("unchecked")
    private void loadInvoiceForEdit(String invoiceId) {
        FirebaseFirestore.getInstance().collection("invoices").document(invoiceId).get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);
                    if (!doc.exists()) {
                        Toast.makeText(this, "Data invoice tidak ditemukan", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

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

                    Object companyObj = doc.get("company");
                    if (companyObj instanceof Map) {
                        Map<String, Object> company = (Map<String, Object>) companyObj;
                        etNamaUsaha.setText(company.get("namaUsaha") != null ? company.get("namaUsaha").toString() : "");
                        etAlamatUsaha.setText(company.get("alamat") != null ? company.get("alamat").toString() : "");
                        etTelpUsaha.setText(company.get("telp") != null ? company.get("telp").toString() : "");
                        String logo = company.get("logoBase64") != null ? company.get("logoBase64").toString() : "";
                        if (!logo.isEmpty()) {
                            byte[] bytes = Base64.decode(logo, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            imgLogo.setImageBitmap(bitmap);
                            logoBase64 = logo;
                        }
                    }

                    // Load items
                    listInvoice.clear();
                    Object itemsObj = doc.get("items");
                    if (itemsObj instanceof List) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                        for (Map<String, Object> m : items) {
                            String namaBarang = m.get("namaBarang") != null ? m.get("namaBarang").toString() : "";
                            int qty = m.get("qty") instanceof Number ? ((Number) m.get("qty")).intValue() : 0;
                            double harga = m.get("hargaSatuan") instanceof Number ? ((Number) m.get("hargaSatuan")).doubleValue() : 0.0;
                            double diskon = m.get("diskon") instanceof Number ? ((Number) m.get("diskon")).doubleValue() : 0.0;
                            listInvoice.add(new ItemInvoice(namaBarang, qty, harga, diskon));
                        }
                    }

                    invoiceAdapter.notifyDataSetChanged();
                    hitungTotal();

                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memuat data invoice: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            return Double.parseDouble(r.replace("Rp", "").replace(".", "").replace(",", ".").trim());
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