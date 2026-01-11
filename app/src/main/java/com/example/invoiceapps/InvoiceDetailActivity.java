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
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InvoiceDetailActivity extends AppCompatActivity {

    private static final int MAX_ITEM_NAME_LENGTH = 18;

    private TextView tvNoInvoice, tvNamaCustomer, tvTanggal, tvTotal;
    private RecyclerView rvDetailBarang;
    private Button btnDownloadPdf;

    private Invoice invoice;
    private List<ItemInvoice> itemList;
    private InvoiceAdapter adapter;

    private FirebaseFirestore db;
    private String invoiceId;
    private NumberFormat rupiahFormat;

    private String jenisPembayaran = "-";
    private String namaAdmin = "Administrator";
    private String namaPenerima = "Penerima";

    private Uri logoUri;
    private String namaToko = "";
    private String alamatToko = "";
    private String telpToko = "";
    private String alamatCustomer = "-";
    private String telpCustomer = "-";

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
            Toast.makeText(this, "Invoice tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        logoUri = getIntent().getParcelableExtra("logoUri");

        loadInvoiceDetail();

        btnDownloadPdf.setOnClickListener(v -> {
            if (invoice == null) return;

            File pdf = generatePdf();
            if (pdf != null && pdf.exists()) {
                Toast.makeText(
                        this,
                        "Invoice berhasil diunduh di:\n" + pdf.getAbsolutePath(),
                        Toast.LENGTH_LONG
                ).show();

                try {
                    Uri uri = FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            pdf
                    );

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(intent, "Buka PDF dengan"));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Gagal membuka PDF", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Gagal membuat PDF", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

                    invoice.setPajak(getDouble(doc, "pajak"));
                    invoice.setBiayaPengiriman(getDouble(doc, "biayaPengiriman"));

                    jenisPembayaran = safeString(doc, "metodePembayaran", "-");
                    namaAdmin = safeString(doc, "namaAdmin", "Administrator");
                    namaPenerima = safeString(doc, "namaPenerima", "Penerima");
                    alamatCustomer = safeString(doc, "alamatCustomer", "-");
                    telpCustomer = safeString(doc, "telpCustomer", "-");

                    // COMPANY PER INVOICE
                    Object companyObj = doc.get("company");
                    if (companyObj instanceof Map) {
                        Map<String, Object> companyMap = (Map<String, Object>) companyObj;
                        Company company = new Company(
                                getMapString(companyMap, "namaUsaha"),
                                getMapString(companyMap, "alamat"),
                                getMapString(companyMap, "telp"),
                                getMapString(companyMap, "logoUri")
                        );

                        invoice.setCompany(company);

                        // Set info toko untuk PDF
                        namaToko = company.getNamaUsaha();
                        alamatToko = company.getAlamat();
                        telpToko = company.getTelp();
                        logoUri = company.getLogoUri() != null ? Uri.parse(company.getLogoUri()) : null;
                    }

                    // ITEMS
                    Object itemsObj = doc.get("items");
                    itemList.clear();
                    if (itemsObj instanceof List) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
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
                    hitungInvoice();

                    tvNoInvoice.setText(invoice.getNoInvoice());
                    tvNamaCustomer.setText(invoice.getNamaCustomer());
                    tvTanggal.setText(invoice.getTanggal());
                    tvTotal.setText(rupiah(invoice.getTotal()));

                    adapter.notifyDataSetChanged();
                });
    }

    private void hitungInvoice() {
        double subTotal = 0;
        double totalDiskon = 0;

        for (ItemInvoice item : invoice.getItems()) {
            double kotor = item.getQty() * item.getHargaSatuan();
            double diskon = kotor * (item.getDiskon() / 100);
            subTotal += kotor;
            totalDiskon += diskon;
        }

        double setelahDiskon = subTotal - totalDiskon;
        double pajakNilai = setelahDiskon * (invoice.getPajak() / 100);
        double total = setelahDiskon + pajakNilai + invoice.getBiayaPengiriman();

        invoice.setSubTotal(subTotal);
        invoice.setTotalDiskon(totalDiskon);
        invoice.setNilaiPajak(pajakNilai);
        invoice.setTotal(total);
    }

    private File generatePdf() {
        PdfDocument pdf = new PdfDocument();
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

        PdfDocument.Page page =
                pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();
        int y = 40;

        // LOGO
        if (logoUri != null) {
            try (InputStream is = getContentResolver().openInputStream(logoUri)) {
                Bitmap logo = BitmapFactory.decodeStream(is);
                if (logo != null) {
                    Bitmap scaled = Bitmap.createScaledBitmap(logo, 70, 70, false);
                    canvas.drawBitmap(scaled, 40, y, normal);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // INFO TOKO
        bold.setTextSize(14);
        canvas.drawText(namaToko, 120, y + 20, bold);
        normal.setTextSize(11);
        canvas.drawText(alamatToko, 120, y + 38, normal);
        canvas.drawText("Telp: " + telpToko, 120, y + 54, normal);

        // JUDUL INVOICE
        bold.setTextSize(22);
        canvas.drawText("INVOICE", 420, y + 40, bold);

        y += 90;
        canvas.drawLine(40, y, 555, y, line);

        // HEADER (Customer & Invoice info)
        y += 25;
        generatePdfHeader(canvas, y);

        y += 70;
        canvas.drawLine(40, y, 555, y, line);

        // TABLE HEADER
        y += 25;
        canvas.drawText("Barang", 40, y, tableHeader);
        canvas.drawText("Qty", 240, y, tableHeader);
        canvas.drawText("Harga", 290, y, tableHeader);
        canvas.drawText("Diskon", 380, y, tableHeader);
        canvas.drawText("Total", 470, y, tableHeader);

        y += 10;
        canvas.drawLine(40, y, 555, y, line);

        y += 18;
        for (ItemInvoice item : invoice.getItems()) {
            double kotor = item.getQty() * item.getHargaSatuan();
            double diskonNominal = kotor * (item.getDiskon() / 100);
            double totalItem = kotor - diskonNominal;

            canvas.drawText(limitText(item.getNamaBarang(), MAX_ITEM_NAME_LENGTH), 40, y, tableText);
            canvas.drawText(String.valueOf(item.getQty()), 240, y, tableText);
            canvas.drawText(rupiah(item.getHargaSatuan()), 290, y, tableText);

            String diskonText = String.format(Locale.getDefault(), "%.0f%% (Rp %s)",
                    item.getDiskon(),
                    rupiah(diskonNominal));
            canvas.drawText(diskonText, 380, y, tableText);

            canvas.drawText(rupiah(totalItem), 470, y, tableText);

            y += 14;
        }


        y += 10;
        canvas.drawLine(330, y, 555, y, line);

        // SUMMARY
        y += 20;
        drawSummary(canvas, "Sub Total", invoice.getSubTotal(), y);
        y += 18;
        drawSummary(canvas, "Diskon", -invoice.getTotalDiskon(), y);
        y += 18;
        drawSummary(canvas, "Pajak (" + invoice.getPajak() + "%)", invoice.getNilaiPajak(), y);
        y += 18;
        drawSummary(canvas, "Ongkir", invoice.getBiayaPengiriman(), y);
        y += 18;
// Tambahkan garis pemisah sebelum total
        canvas.drawLine(330, y, 555, y, line);

        y += 20;  // beri jarak setelah garis

// Gambar total dengan font bold
        Paint boldSummary = new Paint();
        boldSummary.setTextSize(12);
        boldSummary.setFakeBoldText(true);
        canvas.drawText("Total", 330, y, boldSummary);
        canvas.drawText(rupiah(invoice.getTotal()), 470, y, boldSummary);


        y += 70;
        canvas.drawText("Penerima", 100, y, normal);
        canvas.drawText("Administrator", 400, y, normal);

        y += 60;
        canvas.drawLine(60, y, 200, y, line);
        canvas.drawLine(350, y, 520, y, line);

    /*
        y += 15;
        canvas.drawText(namaPenerima, 90, y, normal);
        canvas.drawText(namaAdmin, 380, y, normal);
    */

        pdf.finishPage(page);

        // SIMPAN PDF
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Invoice Apps");
        if (!folder.exists() && !folder.mkdirs()) {
            Toast.makeText(this, "Gagal membuat folder untuk PDF", Toast.LENGTH_SHORT).show();
            return null;
        }

        File file = new File(folder, "Invoice_" + invoice.getNoInvoice() + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            pdf.writeTo(fos);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            pdf.close();
        }

        return file;
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

        // Kalau alamat/telp kosong, ganti dengan "N/A"
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

        // Invoice info kanan
        canvas.drawText("No Invoice", rightX, y, bold);
        canvas.drawText(":", rightX + 100, y, bold);
        canvas.drawText(invoice.getNoInvoice(), rightX + 110, y, normal);

        canvas.drawText("Tanggal", rightX, y + 18, bold);
        canvas.drawText(":", rightX + 100, y + 18, bold);
        canvas.drawText(invoice.getTanggal(), rightX + 110, y + 18, normal);

        canvas.drawText("Jenis Pembayaran", rightX, y + 36, bold);
        canvas.drawText(":", rightX + 100, y + 36, bold);
        canvas.drawText(jenisPembayaran, rightX + 110, y + 36, normal);
    }


    private void drawKeyValueRight(Canvas c, String key, String value, int y) {
        Paint b = new Paint();
        Paint n = new Paint();
        b.setFakeBoldText(true);
        b.setTextSize(11);
        n.setTextSize(11);

        int rightX = 360;
        c.drawText(key, rightX, y, b);
        c.drawText(":", rightX + 60, y, b);
        c.drawText(value, rightX + 70, y, n);
    }

    private double getDouble(DocumentSnapshot doc, String key) {
        Double v = doc.getDouble(key);
        return v != null ? v : 0;
    }

    private String safeString(DocumentSnapshot doc, String key, String def) {
        String v = doc.getString(key);
        return v != null ? v : def;
    }

    private void drawKeyValue(Canvas c, String key, String value, int y) {
        Paint b = new Paint();
        Paint n = new Paint();
        b.setFakeBoldText(true);
        b.setTextSize(11);
        n.setTextSize(11);

        c.drawText(key, 40, y, b);
        c.drawText(":", 120, y, b);
        c.drawText(value, 130, y, n);
    }

    private void drawSummary(Canvas c, String label, double value, int y) {
        Paint p = new Paint();
        p.setTextSize(10);
        c.drawText(label, 330, y, p);
        c.drawText(rupiah(value), 470, y, p);
    }

    private String rupiah(double v) {
        return rupiahFormat.format(v);
    }

    private String limitText(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    private String getMapString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
