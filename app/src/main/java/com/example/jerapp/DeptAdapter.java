package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class DeptAdapter extends RecyclerView.Adapter<DeptAdapter.ViewHolder> {

    private List<DepartmentModel> deptList;
    private OnDeptClickListener listener;

    public interface OnDeptClickListener {
        void onSelect(DepartmentModel dept);
        void onNavigate(DepartmentModel dept);
    }

    public DeptAdapter(List<DepartmentModel> deptList, OnDeptClickListener listener) {
        this.deptList = deptList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DepartmentModel dept = deptList.get(position);

        holder.name.setText(dept.place_name);
        holder.address.setText(dept.full_address);
        holder.phone.setText("Contact: " + dept.contact);

        // Dynamic info from JSON
        holder.details.setText(String.format("%s | %s | %s", dept.district, dept.category, dept.type));
        holder.fee.setText("Fee: " + dept.fee_detail);
        holder.distance.setText(String.format(Locale.getDefault(), "%.2f km away", dept.distance));

        holder.btnSelect.setOnClickListener(v -> listener.onSelect(dept));
        holder.btnNavigate.setOnClickListener(v -> listener.onNavigate(dept));
    }

    @Override
    public int getItemCount() { return deptList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, address, phone, distance, details, fee;
        Button btnSelect, btnNavigate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.deptName);
            address = itemView.findViewById(R.id.deptAddress);
            phone = itemView.findViewById(R.id.deptPhone);
            distance = itemView.findViewById(R.id.deptDistance);
            details = itemView.findViewById(R.id.deptDetails);
            fee = itemView.findViewById(R.id.deptFee);
            btnSelect = itemView.findViewById(R.id.btnSelectDept);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
        }
    }
}