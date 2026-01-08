package com.example.invoiceapps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private List<ItemInvoice> listInvoice;
    private OnItemClickListener listener;
    private DecimalFormat rupiahFormat;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public InvoiceAdapter(List<ItemInvoice> listInvoice, OnItemClickListener listener) {
        this.listInvoice = listInvoice;
        this.listener = listener;

        // Setup format rupiah
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        rupiahFormat = new DecimalFormat("Rp #,###", symbols);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_invoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemInvoice item = listInvoice.get(position);

        holder.tvNamaBarang.setText(item.getNamaBarang());
        holder.tvQty.setText("Qty: " + item.getQty());
        holder.tvHargaSatuan.setText(formatRupiah(item.getHargaSatuan()));
        holder.tvDiskon.setText("Diskon: " + item.getDiskon() + "%");
        holder.tvTotalHarga.setText(formatRupiah(item.getTotalSetelahDiskon()));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listInvoice.size();
    }

    private String formatRupiah(double nilai) {
        if (nilai == 0) {
            return "Rp 0";
        }
        return rupiahFormat.format(nilai);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNamaBarang, tvQty, tvHargaSatuan, tvDiskon, tvTotalHarga;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
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