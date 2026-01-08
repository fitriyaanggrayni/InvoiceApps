package com.example.invoiceapps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class InvoiceHistoryAdapter extends RecyclerView.Adapter<InvoiceHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Invoice invoice);
    }

    private List<Invoice> invoiceList;
    private OnItemClickListener listener;

    public InvoiceHistoryAdapter(List<Invoice> invoiceList, OnItemClickListener listener) {
        this.invoiceList = invoiceList;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Invoice invoice = invoiceList.get(position);
        holder.tvNoInvoice.setText(invoice.getNoInvoice());
        holder.tvNamaCustomer.setText(invoice.getNamaCustomer());
        holder.tvTotal.setText(String.format("Rp %,d", (int) invoice.getTotal()));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(invoice));
    }

    @Override
    public int getItemCount() { return invoiceList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoInvoice, tvNamaCustomer, tvTotal;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNoInvoice = itemView.findViewById(R.id.tvNoInvoice);
            tvNamaCustomer = itemView.findViewById(R.id.tvNamaCustomer);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }
}
