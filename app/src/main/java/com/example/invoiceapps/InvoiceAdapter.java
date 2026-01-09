package com.example.invoiceapps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    private List<ItemInvoice> list;
    private OnItemDeleteListener listener;
    private boolean isEditable; // <-- kontrol tombol delete

    public InvoiceAdapter(List<ItemInvoice> list, OnItemDeleteListener listener, boolean isEditable) {
        this.list = list;
        this.listener = listener;
        this.isEditable = isEditable;
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
        holder.tvHargaSatuan.setText("Rp " + item.getHargaSatuan());
        holder.tvDiskon.setText("Diskon: " + item.getDiskon() + "%");

        double totalHarga = item.getQty() * item.getHargaSatuan()
                * (1 - item.getDiskon() / 100);
        holder.tvTotalHarga.setText("Rp " + (int) totalHarga);

        if (isEditable) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemDelete(holder.getAdapterPosition());
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE); // hide tombol delete
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

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
