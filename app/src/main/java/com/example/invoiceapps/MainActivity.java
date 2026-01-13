package com.example.invoiceapps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    private InvoiceHistoryAdapter adapter;
    private final List<Invoice> invoiceList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private static final int REQUEST_ADD_EDIT = 200;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewInvoices);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fabAddInvoice);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvoiceHistoryAdapter(invoiceList, this, true);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Tombol tambah invoice
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddInvoiceActivity.class);
            startActivityForResult(intent, REQUEST_ADD_EDIT);
        });

        // Tombol edit invoice dari adapter
        adapter.setOnItemDeleteListener(position -> {
            Invoice invoice = invoiceList.get(position);

            if (invoice.getId() == null || invoice.getId().isEmpty()) {
                Toast.makeText(MainActivity.this, "Invoice ID tidak tersedia", Toast.LENGTH_SHORT).show();
                return;
            }

            // Intent ke AddInvoiceActivity untuk edit
            Intent intent = new Intent(MainActivity.this, AddInvoiceActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("invoiceId", invoice.getId());
            startActivityForResult(intent, REQUEST_ADD_EDIT);
        });

        // Load awal dari server
        loadInvoicesFromServer();
    }

    private void loadInvoicesFromServer() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        db.collection("invoices")
                .get(Source.SERVER)
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    invoiceList.clear();
                    if (snapshots != null) {
                        List<DocumentSnapshot> docs = snapshots.getDocuments();
                        // Urut DESC berdasarkan createdAt
                        Collections.sort(docs, (d1, d2) -> {
                            Date t1 = d1.getDate("createdAt");
                            Date t2 = d2.getDate("createdAt");

                            if (t1 == null && t2 == null) return 0;
                            if (t1 == null) return 1;
                            if (t2 == null) return -1;
                            return t2.compareTo(t1);
                        });

                        for (DocumentSnapshot doc : docs) {
                            invoiceList.add(parseInvoice(doc));
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // Mulai realtime listener
                    startRealtimeListener();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Gagal load invoice: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void startRealtimeListener() {
        listener = db.collection("invoices")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    invoiceList.clear();
                    List<DocumentSnapshot> docs = snapshots.getDocuments();
                    Collections.sort(docs, (d1, d2) -> {
                        Date t1 = d1.getDate("createdAt");
                        Date t2 = d2.getDate("createdAt");

                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t2.compareTo(t1);
                    });

                    for (DocumentSnapshot doc : docs) {
                        invoiceList.add(parseInvoice(doc));
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private Invoice parseInvoice(DocumentSnapshot doc) {
        double total = doc.getDouble("total") != null ? doc.getDouble("total") : 0;
        Date createdAt = doc.getDate("createdAt");
        String tanggal = createdAt != null ?
                new SimpleDateFormat("dd/MM/yyyy").format(createdAt) : "";

        Invoice invoice = new Invoice(
                doc.getString("noInvoice"),
                doc.getString("namaCustomer"),
                tanggal,
                total
        );

        invoice.setId(doc.getId());

        Object itemsObj = doc.get("items");
        if (itemsObj instanceof List) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
            for (Map<String, Object> map : items) {
                String namaBarang = map.get("namaBarang") != null ? map.get("namaBarang").toString() : "";
                int qty = map.get("qty") instanceof Number ? ((Number) map.get("qty")).intValue() : 0;
                double hargaSatuan = map.get("hargaSatuan") instanceof Number ? ((Number) map.get("hargaSatuan")).doubleValue() : 0;
                double diskon = map.get("diskon") instanceof Number ? ((Number) map.get("diskon")).doubleValue() : 0;

                invoice.addItem(new ItemInvoice(namaBarang, qty, hargaSatuan, diskon));
            }
        }

        return invoice;
    }

    // ================= REFRESH SETELAH EDIT =================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_EDIT && resultCode == RESULT_OK) {
            // refresh list
            loadInvoicesFromServer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
