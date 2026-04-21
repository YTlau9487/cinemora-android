package com.cinemora.movieorder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for displaying user's order history.
 * Shows order ID, date, total cost, progress status, and item count.
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orders;
    private Context context;

    public OrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        // Format order ID (show last 5 characters)
        String orderId = order.getOrderId();
        String displayId = "Order #" + (orderId != null && orderId.length() >= 5 ? orderId.substring(orderId.length() - 5) : orderId);
        holder.tvOrderId.setText(displayId);

        // Format date
        holder.tvOrderDate.setText(DateUtils.formatOrderDate(order.getOrderDate()));

        // Format cost
        holder.tvTotalCost.setText(DateUtils.formatCurrency(order.getTotalCost()));

        // Set progress status with color
        holder.tvOrderStatus.setText(order.getProgress());
        holder.tvOrderStatus.setTextColor(order.getProgressColor());

        // Set item count
        holder.tvItemsCount.setText(order.getItemCount() + " item(s)");

        // Click listener to open order detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            // TASK 1: Use the constant from OrderDetailActivity to ensure matching keys
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getOrderId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId;
        TextView tvOrderDate;
        TextView tvTotalCost;
        TextView tvOrderStatus;
        TextView tvItemsCount;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvTotalCost = itemView.findViewById(R.id.tv_total_cost);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvItemsCount = itemView.findViewById(R.id.tv_items_count);
        }
    }
}
