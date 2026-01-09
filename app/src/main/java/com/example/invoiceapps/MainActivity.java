package com.example.invoiceapps;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewInvoices;
    private InvoiceHistoryAdapter adapter;
    private List<Invoice> invoiceList;
    private FloatingActionButton fabAddInvoice;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewInvoices = findViewById(R.id.recyclerViewInvoices);
        fabAddInvoice = findViewById(R.id.fabAddInvoice);
        progressBar = findViewById(R.id.progressBar);

        recyclerViewInvoices.setLayoutManager(new LinearLayoutManager(this));
        invoiceList = new ArrayList<>();
        adapter = new InvoiceHistoryAdapter(invoiceList, this);
        recyclerViewInvoices.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        fabAddInvoice.setOnClickListener(v ->
                startActivity(new Intent(this, AddInvoiceActivity.class))
        );

        loadInvoices();
    }

    private void loadInvoices() {
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("invoices")
                .whereEqualTo("userId", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    progressBar.setVisibility(View.GONE);
                    if (error != null) return;

                    invoiceList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {

                        Invoice invoice = new Invoice(
                                doc.getString("noInvoice"),
                                doc.getString("namaCustomer"),
                                doc.getString("tanggal"),
                                doc.getDouble("total") != null ? doc.getDouble("total") : 0
                        );

                        invoice.setId(doc.getId());

                        List<Map<String, Object>> items =
                                (List<Map<String, Object>>) doc.get("items");

                        if (items != null) {
                            for (Map<String, Object> map : items) {
                                invoice.addItem(new ItemInvoice(
                                        (String) map.get("namaBarang"),
                                        ((Long) map.get("qty")).intValue(),
                                        (Double) map.get("hargaSatuan"),
                                        (Double) map.get("diskon")
                                ));
                            }
                        }

                        invoiceList.add(invoice);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
