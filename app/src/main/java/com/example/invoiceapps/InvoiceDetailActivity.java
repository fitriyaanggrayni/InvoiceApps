package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.appbar.MaterialToolbar;

import android.content.ContentValues;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.widget.Toast;



public class InvoiceDetailActivity extends AppCompatActivity {

    private static final int MAX_ITEM_NAME_LENGTH = 18;

    private TextView tvNoInvoice, tvNamaCustomer, tvTanggal, tvTotal;
    private RecyclerView rvDetailBarang;
    private MaterialButton btnDownloadPdf, btnEdit;

    private Invoice invoice;
    private List<ItemInvoice> itemList;
    private InvoiceAdapter adapter;

    private FirebaseFirestore db;
    private String invoiceId;
    private NumberFormat rupiahFormat;

    // PDF data
    private String jenisPembayaran = "-";
    private String namaToko = "";
    private String alamatToko = "";
    private String telpToko = "";
    private String alamatCustomer = "-";
    private String telpCustomer = "-";
    private Bitmap logoBitmap;

    //private ActivityResultLauncher<Intent> savePdfLauncher;
    private PdfDocument pendingPdf;
    private String pendingFileName;

    private final ActivityResultLauncher<Intent> savePdfLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                    Uri uri = result.getData().getData();

                    try {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        pendingPdf.writeTo(os);
                        os.close();
                        pendingPdf.close();

                        Toast.makeText(this, "PDF berhasil disimpan", Toast.LENGTH_SHORT).show();

                        // buka otomatis
                        Intent open = new Intent(Intent.ACTION_VIEW);
                        open.setDataAndType(uri, "application/pdf");
                        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(open);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Gagal menyimpan PDF", Toast.LENGTH_LONG).show();
                    }
                }
            });

    private void uploadPdfToDrive(byte[] pdfBytes, String fileName) {

        new Thread(() -> {
            try {
                String base64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP);

                URL url = new URL(
                        "https://script.google.com/macros/s/AKfycbxsLJubgC9SyUPT75l1tLK7mYu10YXfvWzR0WFEbCtL59siy-JZpYtrLspLbC42kSu8/exec"
                );

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "application/json");

                // JSON BODY
                String jsonBody =
                        "{"
                                + "\"fileName\":\"" + fileName + "\","
                                + "\"file\":\"" + base64 + "\""
                                + "}";

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this,
                                "PDF berhasil diupload ke Google Drive",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Upload gagal (" + responseCode + ")",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error upload PDF: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        rupiahFormat.setMaximumFractionDigits(0);

        initViews();
        setupRecyclerView();

        db = FirebaseFirestore.getInstance();

        invoiceId = getIntent().getStringExtra("invoiceId");
        if (invoiceId == null) {
            Toast.makeText(this, "Invoice tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadInvoiceDetail();

        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, AddInvoiceActivity.class);
            i.putExtra("isEdit", true);
            i.putExtra("invoiceId", invoiceId);
            editLauncher.launch(i);  // pakai launcher ini
        });


        btnDownloadPdf.setOnClickListener(v -> {
            if (invoice == null) return;

            String[] pilihan = {"Kasir 1", "Kasir 2", "Kasir 3"};

            new AlertDialog.Builder(this)
                    .setTitle("Pilih Template PDF")
                    .setItems(pilihan, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                generatePdfKasir1();
                                break;
                            case 1:
                                generatePdfKasir2();
                                break;
                            case 2:
                                generatePdfKasir3();
                                break;
                        }
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });



    }

    private void savePdf(PdfDocument pdf, String fileName) {

        pendingPdf = pdf;
        pendingFileName = fileName;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        savePdfLauncher.launch(intent);
    }


    private void openPdf(File pdf) {
        if (pdf == null || !pdf.exists()) {
            Toast.makeText(this, "Gagal membuat PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdf
            );

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);

        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private void reloadInvoice() {
        if (invoiceId != null) {
            loadInvoiceDetail();
        }
    }

    private void initViews() {
        tvNoInvoice = findViewById(R.id.tvNoInvoice);
        tvNamaCustomer = findViewById(R.id.tvNamaCustomer);
        tvTanggal = findViewById(R.id.tvTanggal);
        tvTotal = findViewById(R.id.tvTotal);
        rvDetailBarang = findViewById(R.id.rvDetailBarang);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        btnEdit = findViewById(R.id.btnEdit);
    }

    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        adapter = new InvoiceAdapter(itemList, position -> {}, false);
        rvDetailBarang.setLayoutManager(new LinearLayoutManager(this));
        rvDetailBarang.setAdapter(adapter);
    }

    private void loadInvoiceDetail() {
        db.collection("invoices")
                .document(invoiceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Invoice tidak ditemukan", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    invoice = new Invoice(
                            doc.getString("noInvoice"),
                            doc.getString("namaCustomer"),
                            doc.getString("tanggalText"),

                            0
                    );

                    invoice.setPajak(getDouble(doc, "pajak"));
                    invoice.setBiayaPengiriman(getDouble(doc, "biayaPengiriman"));

                    alamatCustomer = safeString(doc, "alamatCustomer", "-");
                    telpCustomer = safeString(doc, "telpCustomer", "-");
                    jenisPembayaran = safeString(doc, "metodePembayaran", "-");

                    Object companyObj = doc.get("company");
                    if (companyObj instanceof Map) {
                        Map<String, Object> c = (Map<String, Object>) companyObj;
                        namaToko = getMapString(c, "namaUsaha");
                        alamatToko = getMapString(c, "alamat");
                        telpToko = getMapString(c, "telp");

                        String logoBase64 = getMapString(c, "logoBase64");
                        if (!logoBase64.isEmpty()) {
                            try {
                                byte[] bytes = Base64.decode(logoBase64, Base64.DEFAULT);
                                logoBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            } catch (Exception e) {
                                e.printStackTrace();
                                logoBitmap = null;
                            }
                        } else {
                            logoBitmap = null;
                        }
                    } else {
                        namaToko = "";
                        alamatToko = "";
                        telpToko = "";
                        logoBitmap = null;
                    }

                    Object itemsObj = doc.get("items");
                    itemList.clear();

                    if (itemsObj instanceof List) {
                        for (Object o : (List<?>) itemsObj) {
                            if (o instanceof Map) {
                                Map<String, Object> m = (Map<String, Object>) o;

                                String namaBarang = m.get("namaBarang") != null
                                        ? m.get("namaBarang").toString()
                                        : "";

                                int qty = m.get("qty") instanceof Number
                                        ? ((Number) m.get("qty")).intValue()
                                        : 0;

                                double hargaSatuan = m.get("hargaSatuan") instanceof Number
                                        ? ((Number) m.get("hargaSatuan")).doubleValue()
                                        : 0;

                                double diskon = m.get("diskon") instanceof Number
                                        ? ((Number) m.get("diskon")).doubleValue()
                                        : 0;

                                itemList.add(new ItemInvoice(namaBarang, qty, hargaSatuan, diskon));
                            }
                        }
                    }


                    invoice.setItems(itemList);
                    hitungInvoice();

                    tvNoInvoice.setText(invoice.getNoInvoice());
                    tvNamaCustomer.setText(invoice.getNamaCustomer());
                    tvTanggal.setText(invoice.getTanggal());
                    tvTotal.setText(rupiah(invoice.getTotal()));

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memuat invoice", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void hitungInvoice() {
        double sub = 0, disc = 0;
        for (ItemInvoice i : invoice.getItems()) {
            double k = i.getQty() * i.getHargaSatuan();
            double d = k * (i.getDiskon() / 100);
            sub += k;
            disc += d;
        }
        double after = sub - disc;
        double tax = after * (invoice.getPajak() / 100);
        invoice.setSubTotal(sub);
        invoice.setTotalDiskon(disc);
        invoice.setNilaiPajak(tax);
        invoice.setTotal(after + tax + invoice.getBiayaPengiriman());
    }

    private double getDouble(DocumentSnapshot doc, String key) {
        Double v = doc.getDouble(key);
        return v != null ? v : 0;
    }

    private String safeString(DocumentSnapshot doc, String key, String def) {
        String v = doc.getString(key);
        return v != null ? v : def;
    }

    private String getMapString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private String rupiah(double v) {
        return rupiahFormat.format(v);
    }

    private ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            reloadInvoice();
                        }
                    }
            );

    // =============== PDF GENERATION =================
    private void generatePdfKasir1() {

        // VALIDASI DATA
        if (invoice == null || invoice.getItems() == null || invoice.getItems().isEmpty()) {
            Toast.makeText(this, "Data invoice belum siap", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdf = new PdfDocument();

        try {

            Paint normal = new Paint();
            Paint bold = new Paint();
            Paint line = new Paint();
            Paint tableText = new Paint();
            Paint tableHeader = new Paint();

            normal.setTextSize(11);
            bold.setTextSize(12);
            bold.setFakeBoldText(true);
            tableText.setTextSize(10f);
            tableHeader.setTextSize(11);
            tableHeader.setFakeBoldText(true);
            line.setStrokeWidth(2);

            PdfDocument.Page page = pdf.startPage(
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create()
            );

            Canvas canvas = page.getCanvas();
            int y = 40;

            // ================= LOGO =================
            if (logoBitmap != null && !logoBitmap.isRecycled()) {
                Bitmap scaled = Bitmap.createScaledBitmap(logoBitmap, 70, 70, false);
                canvas.drawBitmap(scaled, 40, y, null);
            }

            // ================= INFO TOKO =================
            bold.setTextSize(14);
            canvas.drawText(String.valueOf(namaToko), 120, y + 20, bold);

            normal.setTextSize(11);
            canvas.drawText(String.valueOf(alamatToko), 120, y + 38, normal);
            canvas.drawText("Telp: " + String.valueOf(telpToko), 120, y + 54, normal);

            bold.setTextSize(22);
            canvas.drawText("INVOICE", 420, y + 40, bold);

            y += 90;
            canvas.drawLine(40, y, 555, y, line);

            // HEADER CUSTOMER
            y += 25;
            generatePdfHeader(canvas, y);

            y += 70;
            canvas.drawLine(40, y, 555, y, line);

            // ================= TABLE HEADER =================
            y += 25;
            canvas.drawText("Barang", 40, y, tableHeader);
            canvas.drawText("Qty", 240, y, tableHeader);
            canvas.drawText("Harga", 290, y, tableHeader);
            canvas.drawText("Diskon", 380, y, tableHeader);
            canvas.drawText("Total", 470, y, tableHeader);

            y += 10;
            canvas.drawLine(40, y, 555, y, line);

            // ================= ITEMS =================
            y += 18;
            for (ItemInvoice item : invoice.getItems()) {

                double kotor = item.getQty() * item.getHargaSatuan();
                double diskonNominal = kotor * (item.getDiskon() / 100.0);
                double totalItem = kotor - diskonNominal;

                canvas.drawText(limitText(item.getNamaBarang(), MAX_ITEM_NAME_LENGTH), 40, y, tableText);
                canvas.drawText(String.valueOf(item.getQty()), 240, y, tableText);
                canvas.drawText(rupiah(item.getHargaSatuan()), 290, y, tableText);

                String diskonText = String.format(Locale.getDefault(),
                        "%.0f%% (Rp %s)", item.getDiskon(), rupiah(diskonNominal));

                canvas.drawText(diskonText, 380, y, tableText);
                canvas.drawText(rupiah(totalItem), 470, y, tableText);

                y += 14;
            }

            // ================= SUMMARY =================
            y += 10;
            canvas.drawLine(330, y, 555, y, line);

            y += 20;
            drawSummary(canvas, "Sub Total", invoice.getSubTotal(), y);
            y += 18;
            drawSummary(canvas, "Diskon", -invoice.getTotalDiskon(), y);
            y += 18;
            drawSummary(canvas, "Pajak (" + invoice.getPajak() + "%)", invoice.getNilaiPajak(), y);
            y += 18;
            drawSummary(canvas, "Ongkir", invoice.getBiayaPengiriman(), y);

            y += 18;
            canvas.drawLine(330, y, 555, y, line);

            Paint boldSummary = new Paint();
            boldSummary.setTextSize(12);
            boldSummary.setFakeBoldText(true);

            y += 20;
            canvas.drawText("Total", 330, y, boldSummary);
            canvas.drawText(rupiah(invoice.getTotal()), 470, y, boldSummary);

            // ================= TTD =================
            y += 70;
            canvas.drawText("Penerima", 100, y, normal);
            canvas.drawText("Administrator", 400, y, normal);

            y += 60;
            canvas.drawLine(60, y, 200, y, line);
            canvas.drawLine(350, y, 520, y, line);

            pdf.finishPage(page);

            // ================= SAVE FILE =================
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                pdf.writeTo(baos);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal membuat PDF", Toast.LENGTH_LONG).show();
                return;
            } finally {
                pdf.close();
            }

            uploadPdfToDrive(
                    baos.toByteArray(),
                    "Invoice_" + invoice.getNoInvoice() + ".pdf"
            );



        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


        // Simpan PDF ke folder app
        /*ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "Invoice_" + invoice.getNoInvoice() + ".pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/Invoice Apps");

        Uri uri = getContentResolver().insert(
                MediaStore.Files.getContentUri("external"), values);

        if (uri == null) return null;

        try (java.io.OutputStream os =
                     getContentResolver().openOutputStream(uri)) {

            pdf.writeTo(os);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            pdf.close();
        }

        return uri;
    } */

    private  void generatePdfKasir2() {
        if (invoice == null || invoice.getItems() == null) {
            Toast.makeText(this, "Data invoice belum siap", Toast.LENGTH_SHORT).show();
            return;
        }


        PdfDocument pdf = new PdfDocument();
        Paint normal = new Paint();
        Paint bold = new Paint();
        Paint line = new Paint();

        normal.setTextSize(11);
        bold.setTextSize(12);
        bold.setFakeBoldText(true);
        line.setStrokeWidth(2);

        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();

        int y = 40;

        // ================= INFO TOKO (kotak) =================
        Paint rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(2);
        canvas.drawRect(40, y, 300, y + 60, rectPaint);

        bold.setTextSize(12);
        canvas.drawText(namaToko, 45, y + 20, bold);
        normal.setTextSize(11);
        canvas.drawText(alamatToko, 45, y + 35, normal);
        canvas.drawText("No. HP: " + telpToko, 45, y + 50, normal);

        // Judul kanan atas
        bold.setTextSize(22);           // lebih besar
        bold.setFakeBoldText(true);
        canvas.drawText("SURAT JALAN", 380, y + 32, bold);

        y += 80;

// ================= DATA INVOICE =================
        bold.setTextSize(11);

// No Nota
        canvas.drawText("No. Nota", 40, y, bold);
        canvas.drawText(":", 120, y, bold);
        canvas.drawText(invoice.getNoInvoice(), 130, y, normal);

        y += 18;

// Tanggal Pengiriman (di bawah No Nota)
        canvas.drawText("Tgl. Pengiriman", 40, y, bold);
        canvas.drawText(":", 120, y, bold);
        canvas.drawText(invoice.getTanggal(), 130, y, normal);

        y += 18;

// Kepada Yth (di bawah tanggal)
        canvas.drawText("Kepada Yth", 40, y, bold);
        canvas.drawText(":", 120, y, bold);
        canvas.drawText(invoice.getNamaCustomer(), 130, y, normal);

        y += 30;


        // ================= TABEL BARANG =================
        // Header
        int startX = 40;
        int[] colX = {startX, 70, 250, 330, 380, 440, 510}; // NO, NAMA, HARGA, UNIT, JUMLAH, TOTAL

        // Garis atas tabel
        canvas.drawLine(startX, y, 555, y, line);
        y += 15;

        bold.setTextSize(11);
        canvas.drawText("NO", colX[0], y, bold);
        canvas.drawText("NAMA BARANG", colX[1], y, bold);
        canvas.drawText("HARGA", colX[2], y, bold);
        canvas.drawText("UNIT", colX[3], y, bold);
        canvas.drawText("JUMLAH", colX[4], y, bold);
        canvas.drawText("TOTAL", colX[5], y, bold);

        y += 10;
        canvas.drawLine(startX, y, 555, y, line);
        y += 15;

        // Isi tabel
        int no = 1;
        for (ItemInvoice item : invoice.getItems()) {
            double totalItem = item.getQty() * item.getHargaSatuan();

            normal.setTextSize(11);
            canvas.drawText(String.valueOf(no), colX[0], y, normal);
            canvas.drawText(limitText(item.getNamaBarang(), 30), colX[1], y, normal);
            canvas.drawText(rupiah(item.getHargaSatuan()), colX[2], y, normal);
            canvas.drawText("PCS", colX[3], y, normal);

            String qtyText = String.valueOf(item.getQty());
            float qtyX = centerTextX(normal, qtyText, colX[4], colX[5]);
            canvas.drawText(qtyText, qtyX, y, normal);

            canvas.drawText(rupiah(totalItem), colX[5], y, normal);

            y += 20;
            no++;
        }

        // Garis bawah tabel
        canvas.drawLine(startX, y, 555, y, line);
        y += 25;

        // ================= TOTAL =================
        bold.setTextSize(12);
        canvas.drawText("Total Keseluruhan", 40, y, bold);
        canvas.drawText(":", 170, y, bold);
        canvas.drawText(rupiah(invoice.getTotal()), 180, y, bold);
        y += 50;


// ================= TANDA TANGAN =================

// Garis tanda tangan
        float penerimaStart = 50, penerimaEnd = 150;
        float pengirimStart = 220, pengirimEnd = 320;
        float adminStart = 400, adminEnd = 500;

        canvas.drawLine(penerimaStart, y, penerimaEnd, y, line);
        canvas.drawLine(pengirimStart, y, pengirimEnd, y, line);
        canvas.drawLine(adminStart, y, adminEnd, y, line);

// Teks keterangan (center otomatis)
        normal.setTextSize(11);
        float textY = y + 15;

        String t1 = "Penerima";
        String t2 = "Pengirim";
        String t3 = "Admin";

        float x1 = penerimaStart + ((penerimaEnd - penerimaStart - normal.measureText(t1)) / 2);
        float x2 = pengirimStart + ((pengirimEnd - pengirimStart - normal.measureText(t2)) / 2);
        float x3 = adminStart + ((adminEnd - adminStart - normal.measureText(t3)) / 2);

        canvas.drawText(t1, x1, textY, normal);
        canvas.drawText(t2, x2, textY, normal);
        canvas.drawText(t3, x3, textY, normal);

        y += 40;

        pdf.finishPage(page);
        // ================= SAVE FILE =================
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            pdf.writeTo(baos);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat PDF", Toast.LENGTH_LONG).show();
            return;
        } finally {
            pdf.close();
        }

        uploadPdfToDrive(
                baos.toByteArray(),
                "Invoice_" + invoice.getNoInvoice() + ".pdf"
        );


// ================= SIMPAN PDF (MediaStore) =================
     /*   ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "SuratJalan_" + invoice.getNoInvoice() + ".pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/Invoice Apps");

        Uri uri = getContentResolver().insert(
                MediaStore.Files.getContentUri("external"), values);

        if (uri == null) {
            Toast.makeText(this, "Gagal membuat file PDF", Toast.LENGTH_SHORT).show();
            pdf.close();
            return null;
        }

        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            pdf.writeTo(os);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            pdf.close();
        }

        Toast.makeText(this,
                "PDF disimpan di Documents / Invoice Apps",
                Toast.LENGTH_SHORT).show();

        return uri;  */
    }
    private float centerTextX(Paint paint, String text, float colStart, float colEnd) {
        float textWidth = paint.measureText(text);
        return colStart + ((colEnd - colStart - textWidth) / 2);
    }

    private void generatePdfKasir3() {

        if (invoice == null || invoice.getItems() == null) {
            Toast.makeText(this, "Data invoice belum siap", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdf = new PdfDocument();

        Paint normal = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint bold = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint header = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        normal.setTextSize(11);
        bold.setTextSize(12);
        bold.setFakeBoldText(true);
        header.setTextSize(11);
        header.setFakeBoldText(true);
        linePaint.setStrokeWidth(2);

        PdfDocument.Page page = pdf.startPage(
                new PdfDocument.PageInfo.Builder(595, 842, 1).create()
        );
        Canvas canvas = page.getCanvas();

        int y = 40;

        // ================= JUDUL =================
        bold.setTextSize(16);
        canvas.drawText("INVOICE", 260, y, bold);
        y += 30;

        // ================= INFO TOKO =================
        bold.setTextSize(12);
        canvas.drawText(namaToko, 40, y, bold);
        canvas.drawText(alamatToko, 40, y + 15, normal);
        canvas.drawText("Telp : " + telpToko, 40, y + 30, normal);
        y += 60;

        // ================= KEPADA + INFO INVOICE =================
        bold.setTextSize(11);
        canvas.drawText("Kepada Yth :", 40, y, bold);
        canvas.drawText(invoice.getNamaCustomer(), 40, y + 15, normal);
        canvas.drawText(alamatCustomer, 40, y + 30, normal);
        canvas.drawText(telpCustomer, 40, y + 45, normal);

        int rightX = 330;
        canvas.drawText("No Invoice", rightX, y, normal);
        canvas.drawText(": " + invoice.getNoInvoice(), rightX + 80, y, normal);
        canvas.drawText("Tanggal", rightX, y + 15, normal);
        canvas.drawText(": " + invoice.getTanggal(), rightX + 80, y + 15, normal);
        canvas.drawText("Payment", rightX, y + 30, normal);
        canvas.drawText(": " + jenisPembayaran, rightX + 80, y + 30, normal);

        y += 80;

        // ================= TABEL =================
        int[] col = {40, 80, 280, 360, 450};

        canvas.drawLine(40, y, 555, y, linePaint);
        y += 18;

        canvas.drawText("No", col[0], y, header);
        canvas.drawText("Nama Produk", col[1], y, header);
        canvas.drawText("Qty", col[2], y, header);
        canvas.drawText("Harga", col[3], y, header);
        canvas.drawText("Total", col[4], y, header);

        y += 8;
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 18;

        int no = 1;
        for (ItemInvoice item : invoice.getItems()) {
            double totalItem = item.getQty() * item.getHargaSatuan();

            canvas.drawText(String.valueOf(no++), col[0], y, normal);
            canvas.drawText(limitText(item.getNamaBarang(), 30), col[1], y, normal);
            canvas.drawText(String.valueOf(item.getQty()), col[2], y, normal);
            canvas.drawText(rupiah(item.getHargaSatuan()), col[3], y, normal);
            canvas.drawText(rupiah(totalItem), col[4], y, normal);

            y += 18;
        }

        canvas.drawLine(40, y, 555, y, linePaint);
        y += 10;

        // ================= TERBILANG =================
        String terbilangText = angkaKeTerbilang((long) invoice.getTotal());
        if (terbilangText == null || terbilangText.trim().isEmpty()) {
            terbilangText = "-";
        }

        int lineHeight = 15;
        int boxLeft = 40;
        int boxRight = 350;
        int boxTop = y + 18;

        int paddingLeft = 55;
        int paddingRight = 10;
        int paddingTop = 16;

        Paint italicBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        italicBold.setTextSize(11);
        italicBold.setFakeBoldText(true);
        italicBold.setTextSkewX(-0.15f); // aman emulator

        float maxTextWidth = boxRight - boxLeft - paddingLeft - paddingRight;

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : terbilangText.split(" ")) {
            String testLine = currentLine.length() == 0
                    ? word
                    : currentLine + " " + word;

            if (italicBold.measureText(testLine) <= maxTextWidth) {
                currentLine.setLength(0);
                currentLine.append(testLine);
            } else {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        int lastIndex = lines.size() - 1;
        lines.set(lastIndex, lines.get(lastIndex) + " Rupiah");

        int boxBottom = boxTop + (lines.size() * lineHeight) + paddingTop;

        bold.setTextSize(11);
        canvas.drawText("Terbilang:", boxLeft, y + 12, bold);

        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2);
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint);

        int safeOffset = 3;
        int textY = boxTop + paddingTop;

        for (String textLine : lines) {
            canvas.drawText(
                    textLine,
                    boxLeft + paddingLeft + safeOffset,
                    textY,
                    italicBold
            );
            textY += lineHeight;
        }

        y = boxBottom + 10;

        // ================= TOTAL =================
        bold.setTextSize(12);
        canvas.drawText("Total:", col[3], boxTop + 30, bold);
        canvas.drawText(rupiah(invoice.getTotal()), col[4], boxTop + 30, bold);

        // ================= TTD =================
        y += 30;
        canvas.drawText("Diterima oleh,", 80, y, normal);
        canvas.drawText("Hormat kami,", 380, y, normal);

        y += 60;
        canvas.drawLine(60, y, 180, y, linePaint);
        canvas.drawLine(360, y, 500, y, linePaint);

        pdf.finishPage(page);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            pdf.writeTo(baos);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat PDF", Toast.LENGTH_LONG).show();
            return;
        } finally {
            pdf.close();
        }

        uploadPdfToDrive(
                baos.toByteArray(),
                "Invoice_" + invoice.getNoInvoice() + ".pdf"
        );

    }

        // ================= SIMPAN =================
      /*  ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "Invoice_" + invoice.getNoInvoice() + "_Kasir2.pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/Invoice Apps");

        Uri uri = getContentResolver().insert(
                MediaStore.Files.getContentUri("external"), values);

        if (uri == null) return null;

        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            pdf.writeTo(os);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            pdf.close();
        }

        return uri;
    }*/



    private String angkaKeTerbilang(long angka) {
        String[] satuan = {
                "", "Satu", "Dua", "Tiga", "Empat", "Lima",
                "Enam", "Tujuh", "Delapan", "Sembilan",
                "Sepuluh", "Sebelas"
        };

        if (angka < 12) {
            return satuan[(int) angka];
        } else if (angka < 20) {
            return satuan[(int) (angka - 10)] + " Belas";
        } else if (angka < 100) {
            return satuan[(int) (angka / 10)] + " Puluh " +
                    angkaKeTerbilang(angka % 10);
        } else if (angka < 200) {
            return "Seratus " + angkaKeTerbilang(angka - 100);
        } else if (angka < 1000) {
            return satuan[(int) (angka / 100)] + " Ratus " +
                    angkaKeTerbilang(angka % 100);
        } else if (angka < 2000) {
            return "Seribu " + angkaKeTerbilang(angka - 1000);
        } else if (angka < 1000000) {
            return angkaKeTerbilang(angka / 1000) + " Ribu " +
                    angkaKeTerbilang(angka % 1000);
        } else if (angka < 1000000000) {
            return angkaKeTerbilang(angka / 1000000) + " Juta " +
                    angkaKeTerbilang(angka % 1000000);
        } else {
            return "Angka terlalu besar";
        }
    }



    private void generatePdfHeader(Canvas canvas, int yStart) {
        int y = yStart;
        Paint bold = new Paint();
        Paint normal = new Paint();
        bold.setFakeBoldText(true);
        bold.setTextSize(11);
        normal.setTextSize(11);

        int leftX = 40;
        int rightX = 350;

        String telp = (telpCustomer == null || telpCustomer.trim().isEmpty() || telpCustomer.equals("-")) ? "N/A" : telpCustomer;
        String alamat = (alamatCustomer == null || alamatCustomer.trim().isEmpty() || alamatCustomer.equals("-")) ? "N/A" : alamatCustomer;

        // Customer info kiri
        canvas.drawText("Customer", leftX, y, bold);
        canvas.drawText(":", leftX + 80, y, bold);
        canvas.drawText(invoice.getNamaCustomer(), leftX + 90, y, normal);

        canvas.drawText("Telp", leftX, y + 18, bold);
        canvas.drawText(":", leftX + 80, y + 18, bold);
        canvas.drawText(telp, leftX + 90, y + 18, normal);

        canvas.drawText("Alamat", leftX, y + 36, bold);
        canvas.drawText(":", leftX + 80, y + 36, bold);
        canvas.drawText(alamat, leftX + 90, y + 36, normal);

        int colonOffset = 8;

// Invoice info kanan
        canvas.drawText("No Invoice", rightX, y, bold);
        canvas.drawText(":", rightX + 100 + colonOffset, y, bold);
        canvas.drawText(invoice.getNoInvoice(), rightX + 110 + colonOffset, y, normal);

        canvas.drawText("Tanggal", rightX, y + 18, bold);
        canvas.drawText(":", rightX + 100 + colonOffset, y + 18, bold);
        canvas.drawText(invoice.getTanggal(), rightX + 110 + colonOffset, y + 18, normal);

        canvas.drawText("Jenis Pembayaran", rightX, y + 36, bold);
        canvas.drawText(":", rightX + 100 + colonOffset, y + 36, bold);
        canvas.drawText(jenisPembayaran, rightX + 110 + colonOffset, y + 36, normal);

    }

    private void drawSummary(Canvas c, String label, double value, int y) {
        Paint p = new Paint();
        p.setTextSize(10);
        c.drawText(label, 330, y, p);
        c.drawText(rupiah(value), 470, y, p);
    }

    private String limitText(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }
}