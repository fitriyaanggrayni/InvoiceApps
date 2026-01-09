package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InvoiceDetailActivity extends AppCompatActivity {

    private TextView tvNoInvoice, tvNamaCustomer, tvTanggal, tvTotal;
    private RecyclerView rvDetailBarang;
    private Button btnDownloadPdf;

    private Invoice invoice;
    private List<ItemInvoice> itemList;
    private InvoiceAdapter adapter;

    private FirebaseFirestore db;
    private String invoiceId;

    private NumberFormat rupiahFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_detail);

        rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        rupiahFormat.setMaximumFractionDigits(0);

        initViews();
        setupRecyclerView();

        db = FirebaseFirestore.getInstance();

        invoiceId = getIntent().getStringExtra("invoiceId");
        if (invoiceId == null) {
            finish();
            return;
        }

        loadInvoiceDetail();

        btnDownloadPdf.setOnClickListener(v -> {
            if (invoice == null) return;
            File pdf = generatePdf();
            if (pdf != null) openPdf(pdf);
        });
    }

    // ================= INIT =================
    private void initViews() {
        tvNoInvoice = findViewById(R.id.tvNoInvoice);
        tvNamaCustomer = findViewById(R.id.tvNamaCustomer);
        tvTanggal = findViewById(R.id.tvTanggal);
        tvTotal = findViewById(R.id.tvTotal);
        rvDetailBarang = findViewById(R.id.rvDetailBarang);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
    }

    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        adapter = new InvoiceAdapter(itemList, position -> {}, false);
        rvDetailBarang.setLayoutManager(new LinearLayoutManager(this));
        rvDetailBarang.setAdapter(adapter);
    }

    // ================= SAFE GETTER =================
    private double getDoubleSafe(DocumentSnapshot doc, String field) {
        Double value = doc.getDouble(field);
        return value != null ? value : 0;
    }

    // ================= LOAD DATA =================
    private void loadInvoiceDetail() {
        db.collection("invoices")
                .document(invoiceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    invoice = new Invoice(
                            doc.getString("noInvoice"),
                            doc.getString("namaCustomer"),
                            doc.getString("tanggal"),
                            0
                    );

                    invoice.setPajak(getDoubleSafe(doc, "pajak"));
                    invoice.setBiayaPengiriman(getDoubleSafe(doc, "biayaPengiriman"));

                    List<Map<String, Object>> items =
                            (List<Map<String, Object>>) doc.get("items");

                    itemList.clear();

                    if (items != null) {
                        for (Map<String, Object> map : items) {

                            Number qty = (Number) map.get("qty");
                            Number harga = (Number) map.get("hargaSatuan");
                            Number diskon = (Number) map.get("diskon");

                            itemList.add(new ItemInvoice(
                                    (String) map.get("namaBarang"),
                                    qty != null ? qty.intValue() : 0,
                                    harga != null ? harga.doubleValue() : 0,
                                    diskon != null ? diskon.doubleValue() : 0
                            ));
                        }
                    }

                    invoice.setItems(itemList);
                    calculateInvoiceSummary();

                    // ===== UI =====
                    tvNoInvoice.setText(invoice.getNoInvoice());
                    tvNamaCustomer.setText(invoice.getNamaCustomer());
                    tvTanggal.setText(invoice.getTanggal());
                    tvTotal.setText(rupiah(invoice.getTotal()));

                    adapter.notifyDataSetChanged();
                });
    }

    // ================= CALCULATION =================
    private void calculateInvoiceSummary() {
        double subTotal = 0;
        double totalDiskon = 0;

        for (ItemInvoice item : invoice.getItems()) {
            double hargaKotor = item.getQty() * item.getHargaSatuan();
            double diskonItem = hargaKotor * (item.getDiskon() / 100);

            subTotal += hargaKotor;
            totalDiskon += diskonItem;
        }

        double setelahDiskon = subTotal - totalDiskon;
        double pajakNilai = setelahDiskon * (invoice.getPajak() / 100);
        double totalAkhir = setelahDiskon + pajakNilai + invoice.getBiayaPengiriman();

        invoice.setSubTotal(subTotal);
        invoice.setTotalDiskon(totalDiskon);
        invoice.setTotal(totalAkhir);
    }

    // ================= PDF =================
    private File generatePdf() {

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        Paint bold = new Paint();
        Paint line = new Paint();

        bold.setFakeBoldText(true);
        bold.setTextSize(12);
        paint.setTextSize(11);
        line.setStrokeWidth(2);

        PdfDocument.Page page =
                pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();

        int y = 40;

        // ===== LOGO =====
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80, false);
        canvas.drawBitmap(scaledLogo, 40, y, paint);

        // ===== TITLE =====
        bold.setTextSize(22);
        canvas.drawText("INVOICE", 420, y + 45, bold);

        // ===== COMPANY =====
        paint.setTextSize(11);
        canvas.drawText("Invoice Apps", 40, y + 95, paint);
        canvas.drawText("Jl. Contoh Alamat No. 123", 40, y + 110, paint);
        canvas.drawText("Telp: 0812-xxxx-xxxx", 40, y + 125, paint);

        y += 150;
        canvas.drawLine(40, y, 555, y, line);

        // ===== INFO =====
        y += 25;
        drawKeyValue(canvas, "No Invoice", invoice.getNoInvoice(), y);
        y += 18;
        drawKeyValue(canvas, "Customer", invoice.getNamaCustomer(), y);
        y += 18;
        drawKeyValue(canvas, "Tanggal", invoice.getTanggal(), y);

        y += 20;
        canvas.drawLine(40, y, 555, y, line);

        // ===== TABLE HEADER =====
        y += 25;
        bold.setTextSize(12);
        canvas.drawText("Barang", 40, y, bold);
        canvas.drawText("Qty", 280, y, bold);
        canvas.drawText("Harga", 330, y, bold);
        canvas.drawText("Total", 470, y, bold);

        y += 10;
        canvas.drawLine(40, y, 555, y, line);

        // ===== ITEMS =====
        y += 20;
        paint.setTextSize(11);

        for (ItemInvoice item : invoice.getItems()) {
            double totalItem = item.getQty() * item.getHargaSatuan()
                    * (1 - item.getDiskon() / 100);

            canvas.drawText(item.getNamaBarang(), 40, y, paint);
            canvas.drawText(String.valueOf(item.getQty()), 280, y, paint);
            canvas.drawText(rupiah(item.getHargaSatuan()), 330, y, paint);
            canvas.drawText(rupiah(totalItem), 470, y, paint);
            y += 18;
        }

        y += 10;
        canvas.drawLine(330, y, 555, y, line);

        // ===== SUMMARY =====
        y += 20;
        drawSummary(canvas, "Sub Total", invoice.getSubTotal(), y);
        y += 18;
        drawSummary(canvas, "Diskon", -invoice.getTotalDiskon(), y);

        double pajakNilai =
                (invoice.getSubTotal() - invoice.getTotalDiskon())
                        * (invoice.getPajak() / 100);

        y += 18;
        drawSummary(canvas, "Pajak (" + invoice.getPajak() + "%)", pajakNilai, y);
        y += 18;
        drawSummary(canvas, "Ongkir", invoice.getBiayaPengiriman(), y);

        y += 22;
        bold.setTextSize(13);
        canvas.drawText("TOTAL", 330, y, bold);
        canvas.drawText(rupiah(invoice.getTotal()), 470, y, bold);

        pdf.finishPage(page);

        File folder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Invoice Apps"
        );
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, "Invoice_" + invoice.getNoInvoice() + ".pdf");

        try {
            pdf.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF tersimpan", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            return null;
        } finally {
            pdf.close();
        }

        return file;
    }

    // ================= HELPERS =================
    private void drawKeyValue(Canvas c, String key, String value, int y) {
        Paint bold = new Paint();
        Paint normal = new Paint();
        bold.setFakeBoldText(true);
        bold.setTextSize(11);
        normal.setTextSize(11);

        c.drawText(key, 40, y, bold);
        c.drawText(":", 120, y, bold);
        c.drawText(value, 130, y, normal);
    }

    private void drawSummary(Canvas c, String label, double value, int y) {
        Paint normal = new Paint();
        normal.setTextSize(11);

        c.drawText(label, 330, y, normal);
        c.drawText(rupiah(value), 470, y, normal);
    }

    private String rupiah(double value) {
        return rupiahFormat.format(value);
    }

    // ================= OPEN PDF =================
    private void openPdf(File file) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}
