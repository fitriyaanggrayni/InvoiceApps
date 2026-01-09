package com.example.invoiceapps;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InvoiceHistoryAdapter extends RecyclerView.Adapter<InvoiceHistoryAdapter.ViewHolder> {

    private List<Invoice> invoiceList;
    private Context context;

    public InvoiceHistoryAdapter(List<Invoice> invoiceList, Context context) {
        this.invoiceList = invoiceList;
        this.context = context;
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
        holder.tvTotal.setText("Rp " + (int) invoice.getTotal());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, InvoiceDetailActivity.class);
            intent.putExtra("invoice", invoice);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return invoiceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoInvoice, tvNamaCustomer, tvTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoInvoice = itemView.findViewById(R.id.tvNoInvoice);
            tvNamaCustomer = itemView.findViewById(R.id.tvNamaCustomer);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }
}
