package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDetailActivity extends AppCompatActivity {

    private TextView tvNoInvoice, tvNamaCustomer, tvTanggal, tvTotal;
    private RecyclerView rvDetailBarang;
    private Button btnDownloadPdf;

    private Invoice invoice;
    private List<ItemInvoice> itemList;
    private InvoiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_detail);

        tvNoInvoice = findViewById(R.id.tvNoInvoice);
        tvNamaCustomer = findViewById(R.id.tvNamaCustomer);
        tvTanggal = findViewById(R.id.tvTanggal);
        tvTotal = findViewById(R.id.tvTotal);
        rvDetailBarang = findViewById(R.id.rvDetailBarang);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);

        invoice = (Invoice) getIntent().getSerializableExtra("invoice");
        if (invoice == null) {
            finish();
            return;
        }

        tvNoInvoice.setText(invoice.getNoInvoice());
        tvNamaCustomer.setText(invoice.getNamaCustomer());
        tvTanggal.setText(invoice.getTanggal());
        tvTotal.setText("Rp " + (int) invoice.getTotal());

        // ================ RecyclerView ================
        itemList = new ArrayList<>(invoice.getItems());
        // modeReadOnly = true, tombol delete disembunyikan
        adapter = new InvoiceAdapter(itemList, position -> {}, false); // tombol delete **tidak tampil**

        rvDetailBarang.setLayoutManager(new LinearLayoutManager(this));
        rvDetailBarang.setAdapter(adapter);

        // ================ Tombol PDF ================
        btnDownloadPdf.setOnClickListener(v -> {
            File pdf = generatePdf();
            if (pdf != null) sharePdf(pdf);
        });
    }

    // ================= PDF GENERATOR =================
    private File generatePdf() {

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(2);

        PdfDocument.Page page =
                pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();

        int y = 40;

        // LOGO
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 120, 120, false);
            canvas.drawBitmap(scaledLogo, 40, y, paint);
        }

        // TITLE
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        canvas.drawText("INVOICE", 400, y + 50, paint);

        y += 140;
        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        canvas.drawText("No Invoice : " + invoice.getNoInvoice(), 40, y, paint);
        y += 20;
        canvas.drawText("Customer  : " + invoice.getNamaCustomer(), 40, y, paint);
        y += 20;
        canvas.drawText("Tanggal   : " + invoice.getTanggal(), 40, y, paint);

        y += 20;
        canvas.drawLine(40, y, 555, y, linePaint);

        // TABLE HEADER
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Barang", 40, y, paint);
        canvas.drawText("Qty", 300, y, paint);
        canvas.drawText("Harga", 350, y, paint);
        canvas.drawText("Total", 470, y, paint);

        paint.setFakeBoldText(false);
        y += 10;
        canvas.drawLine(40, y, 555, y, linePaint);

        // ITEMS
        y += 25;
        for (ItemInvoice item : invoice.getItems()) {
            double totalItem = item.getQty() * item.getHargaSatuan() * (1 - item.getDiskon() / 100);

            canvas.drawText(item.getNamaBarang(), 40, y, paint);
            canvas.drawText(String.valueOf(item.getQty()), 300, y, paint);
            canvas.drawText("Rp " + (int) item.getHargaSatuan(), 350, y, paint);
            canvas.drawText("Rp " + (int) totalItem, 470, y, paint);
            y += 25;
        }

        y += 20;
        canvas.drawLine(40, y, 555, y, linePaint);

        // TOTAL
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("TOTAL : Rp " + (int) invoice.getTotal(), 350, y, paint);

        pdf.finishPage(page);

        // ================= SIMPAN KE DOCUMENTS / Invoice Apps =================
        File folder = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS
                ),
                "Invoice Apps"
        );

        if (!folder.exists()) folder.mkdirs();

        File file = new File(
                folder,
                "Invoice_" + invoice.getNoInvoice() + ".pdf"
        );

        try {
            pdf.writeTo(new FileOutputStream(file));
            Toast.makeText(this,
                    "PDF tersimpan di Documents/Invoice Apps",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        } finally {
            pdf.close();
        }

        return file;
    }

    // ================= SHARE PDF =================
    private void sharePdf(File file) {

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Bagikan Invoice PDF"));
    }
}
