package com.example.jerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.content.Intent;

public class DeptAdapter extends RecyclerView.Adapter<DeptAdapter.ViewHolder> {

    private List<DepartmentModel> deptList;
    private OnDeptClickListener listener;

    public interface OnDeptClickListener {
        void onSelect(DepartmentModel dept);
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
        holder.name.setText(dept.name);
        holder.capability.setText("Capability: " + dept.capability);

        // Note: Distance is calculated in the Activity and can be passed here or set as a placeholder
        holder.distance.setText("Active Responder Found");

        holder.btnSelect.setOnClickListener(v -> listener.onSelect(dept));

        holder.itemView.setOnClickListener(v -> {
            if ("SOS FLASH LIGHT".equalsIgnoreCase(dept.name)) {
                Intent intent = new Intent(v.getContext(), FlashlightActivity.class);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() { return deptList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, capability, distance;
        Button btnSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.deptName);
            capability = itemView.findViewById(R.id.deptCapability);
            distance = itemView.findViewById(R.id.deptDistance);
            btnSelect = itemView.findViewById(R.id.btnSelectDept);
        }
    }
}