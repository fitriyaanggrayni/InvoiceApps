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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    private InvoiceHistoryAdapter adapter;
    private final List<Invoice> invoiceList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser user;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewInvoices);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fabAddInvoice);

        // ===== Inisialisasi Firebase =====
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // ===== RecyclerView =====
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvoiceHistoryAdapter(invoiceList, this, true);
        recyclerView.setAdapter(adapter);

        // ===== Listener tombol delete =====
        adapter.setOnItemDeleteListener(position -> {
            Invoice invoice = invoiceList.get(position);
            if (invoice.getId() != null) {
                db.collection("invoices")
                        .document(invoice.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(MainActivity.this, "Invoice berhasil dihapus", Toast.LENGTH_SHORT).show();
                            // Jangan remove dari list manual, Firestore listener akan update otomatis
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(MainActivity.this, "Gagal menghapus invoice: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }
        });


        // ===== FAB tambah invoice =====
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddInvoiceActivity.class))
        );

        // ===== Load invoice =====
        loadInvoices();
    }


    private void loadInvoices() {

        if (user == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        listener = db.collection("invoices")
                .whereEqualTo("userId", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {

                    progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        Toast.makeText(MainActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    invoiceList.clear();

                    if (snapshots == null || snapshots.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {

                        Invoice invoice = new Invoice(
                                doc.getString("noInvoice"),
                                doc.getString("namaCustomer"),
                                doc.getString("tanggal"),
                                doc.getDouble("total") != null
                                        ? doc.getDouble("total") : 0
                        );

                        invoice.setId(doc.getId());

                        // ===== ITEMS =====
                        List<Map<String, Object>> items =
                                (List<Map<String, Object>>) doc.get("items");

                        if (items != null) {
                            for (Map<String, Object> map : items) {

                                Number qty = (Number) map.get("qty");
                                Number harga = (Number) map.get("hargaSatuan");
                                Number diskon = (Number) map.get("diskon");

                                invoice.addItem(new ItemInvoice(
                                        (String) map.get("namaBarang"),
                                        qty != null ? qty.intValue() : 0,
                                        harga != null ? harga.doubleValue() : 0,
                                        diskon != null ? diskon.doubleValue() : 0
                                ));
                            }
                        }

                        invoiceList.add(invoice);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            listener.remove();
        }
    }
}