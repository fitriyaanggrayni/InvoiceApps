package com.example.invoiceapps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    // ===== INTERFACE DELETE =====
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    private final List<ItemInvoice> list;
    private final OnItemDeleteListener listener;
    private final boolean isEditable;

    // Format Rupiah Indonesia
    private final NumberFormat rupiahFormat =
            NumberFormat.getNumberInstance(new Locale("id", "ID"));

    // ===== CONSTRUCTOR =====
    public InvoiceAdapter(List<ItemInvoice> list,
                          OnItemDeleteListener listener,
                          boolean isEditable) {
        this.list = list;
        this.listener = listener;
        this.isEditable = isEditable;
        rupiahFormat.setMaximumFractionDigits(0);
    }

    // ===== UPDATE DATA =====
    public void updateData(List<ItemInvoice> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemInvoice item = list.get(position);

        holder.tvNamaBarang.setText(item.getNamaBarang());
        holder.tvQty.setText("Qty: " + item.getQty());
        holder.tvHargaSatuan.setText("Rp " + format(item.getHargaSatuan()));

        // Hitung nominal diskon per item
        double kotor = item.getQty() * item.getHargaSatuan();
        double nominalDiskon = kotor * (item.getDiskon() / 100);

        // Diskon dengan nominal
        if (item.getDiskon() > 0) {
            holder.tvDiskon.setVisibility(View.VISIBLE);
            String diskonText = item.getDiskon() + "% (Rp " + format(nominalDiskon) + ")";
            holder.tvDiskon.setText("Diskon: " + diskonText);
        } else {
            holder.tvDiskon.setVisibility(View.GONE);
        }

        double totalHarga = kotor - nominalDiskon;
        holder.tvTotalHarga.setText("Rp " + format(totalHarga));

        // Tombol delete (mode edit)
        if (isEditable) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemDelete(pos);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    // ===== UTIL =====
    private String format(double value) {
        return rupiahFormat.format(value);
    }

    // ===== VIEW HOLDER =====
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvNamaBarang, tvQty, tvHargaSatuan, tvDiskon, tvTotalHarga;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNamaBarang = itemView.findViewById(R.id.tvNamaBarang);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvHargaSatuan = itemView.findViewById(R.id.tvHargaSatuan);
            tvDiskon = itemView.findViewById(R.id.tvDiskon);
            tvTotalHarga = itemView.findViewById(R.id.tvTotalHarga);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
