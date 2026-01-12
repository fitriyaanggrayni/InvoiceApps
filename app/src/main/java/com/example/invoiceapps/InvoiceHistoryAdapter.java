package com.example.invoiceapps;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class InvoiceHistoryAdapter extends RecyclerView.Adapter<InvoiceHistoryAdapter.ViewHolder> {

    public void updateData(List<Invoice> newInvoiceList) {
        invoiceList.clear();
        if (newInvoiceList != null) {
            invoiceList.addAll(newInvoiceList);
        }
        notifyDataSetChanged();
    }


    // ===== INTERFACE DELETE =====
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    private final List<Invoice> invoiceList;
    private final Context context;
    private final boolean isEditable; // true = tombol delete muncul
    private OnItemDeleteListener deleteListener;

    private final NumberFormat rupiahFormat = NumberFormat.getNumberInstance(new Locale("id", "ID"));

    public InvoiceHistoryAdapter(List<Invoice> invoiceList, Context context, boolean isEditable) {
        this.invoiceList = invoiceList;
        this.context = context;
        this.isEditable = isEditable;
        rupiahFormat.setMaximumFractionDigits(0);
    }

    // ===== SETTER LISTENER =====
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice invoice = invoiceList.get(position);

        holder.tvNoInvoice.setText(invoice.getNoInvoice());
        holder.tvNamaCustomer.setText(invoice.getNamaCustomer());
        holder.tvTotal.setText("Rp " + rupiahFormat.format(invoice.getTotal()));

        // Klik untuk buka detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, InvoiceDetailActivity.class);
            intent.putExtra("invoiceId", invoice.getId());
            context.startActivity(intent);
        });

        // Tombol delete
        if (isEditable) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && deleteListener != null) {
                    deleteListener.onItemDelete(pos);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return invoiceList.size();
    }

    // ===== VIEW HOLDER =====
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoInvoice, tvNamaCustomer, tvTotal;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoInvoice = itemView.findViewById(R.id.tvNoInvoice);
            tvNamaCustomer = itemView.findViewById(R.id.tvNamaCustomer);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem); // pastikan id ini di XML ada
        }
    }
}
