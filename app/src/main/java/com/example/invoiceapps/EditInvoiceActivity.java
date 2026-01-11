package com.example.invoiceapps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditInvoiceActivity extends AppCompatActivity {

    private TextInputEditText etNamaCustomer, etAlamatCustomer, etTelpCustomer;
    private MaterialButton btnUpdate;

    private RecyclerView rvItems;
    private InvoiceAdapter adapter;
    private List<ItemInvoice> itemList;

    private FirebaseFirestore db;
    private String invoiceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_invoice);

        db = FirebaseFirestore.getInstance();
        invoiceId = getIntent().getStringExtra("invoiceId");

        if (invoiceId == null) {
            Toast.makeText(this, "Invoice tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecycler();
        loadInvoice();

        btnUpdate.setOnClickListener(v -> updateInvoice());
    }

    private void initViews() {
        etNamaCustomer = findViewById(R.id.etNamaCustomer);
        etAlamatCustomer = findViewById(R.id.etAlamatCustomer);
        etTelpCustomer = findViewById(R.id.etTelpCustomer);
        btnUpdate = findViewById(R.id.btnUpdateInvoice);
        rvItems = findViewById(R.id.rvItems);
    }

    private void setupRecycler() {
        itemList = new ArrayList<>();
        adapter = new InvoiceAdapter(itemList, position -> {}, true);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
    }

    // ================= LOAD DATA LAMA =================
    private void loadInvoice() {
        db.collection("invoices")
                .document(invoiceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    etNamaCustomer.setText(doc.getString("namaCustomer"));
                    etAlamatCustomer.setText(doc.getString("alamatCustomer"));
                    etTelpCustomer.setText(doc.getString("telpCustomer"));

                    List<Map<String, Object>> items =
                            (List<Map<String, Object>>) doc.get("items");

                    itemList.clear();
                    if (items != null) {
                        for (Map<String, Object> map : items) {
                            itemList.add(new ItemInvoice(
                                    (String) map.get("namaBarang"),
                                    ((Number) map.get("qty")).intValue(),
                                    ((Number) map.get("hargaSatuan")).doubleValue(),
                                    ((Number) map.get("diskon")).doubleValue()
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // ================= UPDATE FIRESTORE =================
    private void updateInvoice() {
        Map<String, Object> data = new HashMap<>();
        data.put("namaCustomer", etNamaCustomer.getText().toString());
        data.put("alamatCustomer", etAlamatCustomer.getText().toString());
        data.put("telpCustomer", etTelpCustomer.getText().toString());
        data.put("items", buildItemsMap());

        db.collection("invoices")
                .document(invoiceId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Invoice berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal update invoice", Toast.LENGTH_SHORT).show()
                );
    }

    private List<Map<String, Object>> buildItemsMap() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ItemInvoice item : itemList) {
            Map<String, Object> map = new HashMap<>();
            map.put("namaBarang", item.getNamaBarang());
            map.put("qty", item.getQty());
            map.put("hargaSatuan", item.getHargaSatuan());
            map.put("diskon", item.getDiskon());
            list.add(map);
        }
        return list;
    }
}
