package com.example.invoiceapps;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_INVOICE = 100;

    private List<Invoice> invoiceList = new ArrayList<>();
    private RecyclerView rvInvoices;
    private InvoiceHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FloatingActionButton untuk tambah invoice
        FloatingActionButton fabAddInvoice = findViewById(R.id.fabAddInvoice);
        fabAddInvoice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddInvoiceActivity.class);
            startActivityForResult(intent, REQUEST_ADD_INVOICE);
        });

        // Setup RecyclerView
        rvInvoices = findViewById(R.id.recyclerViewInvoices);
        adapter = new InvoiceHistoryAdapter(invoiceList, invoice -> {
            // Klik invoice -> buka detail / cetak PDF
            Intent intentDetail = new Intent(MainActivity.this, InvoiceDetailActivity.class);
            intentDetail.putExtra("invoiceDetail", invoice);
            startActivity(intentDetail);
        });
        rvInvoices.setLayoutManager(new LinearLayoutManager(this));
        rvInvoices.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_INVOICE && resultCode == RESULT_OK && data != null) {
            Invoice newInvoice = (Invoice) data.getSerializableExtra("newInvoice");
            if (newInvoice != null) {
                invoiceList.add(newInvoice);
                adapter.notifyItemInserted(invoiceList.size() - 1);
            }
        }
    }
}
