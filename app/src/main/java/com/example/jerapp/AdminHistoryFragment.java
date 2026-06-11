package com.example.jerapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<AlertModel> fullHistoryList = new ArrayList<>();
    private List<AlertModel> filteredList = new ArrayList<>();
    private String departmentFilter;
    private TextView statusText, emptyTextView;
    private CalendarView calendarView;
    private Spinner spinnerSort;
    private TextView selectedDateText;
    private Long selectedDayStartMs = null;

    public static AdminHistoryFragment newInstance(String dept) {
        AdminHistoryFragment fragment = new AdminHistoryFragment();
        Bundle args = new Bundle();
        args.putString("dept_filter", dept);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            departmentFilter = getArguments().getString("dept_filter");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.adminAlertRecyclerView);
        statusText = view.findViewById(R.id.adminStatus);
        emptyTextView = view.findViewById(R.id.tvEmptyHistory);
        calendarView = view.findViewById(R.id.historyCalendar);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        selectedDateText = view.findViewById(R.id.tvSelectedHistoryDate);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminAdapter(filteredList, new AdminAdapter.OnAlertClickListener() {
            @Override
            public void onDetailsClick(AlertModel alert) {
                // Admin detail view logic
            }
            @Override public void onResolve(AlertModel alert, String key) {}
            @Override public void onReceive(AlertModel alert) {}
            @Override
            public void onDelete(AlertModel alert) {
                FirebaseDatabase.getInstance().getReference("ResolvedAlerts")
                        .child(departmentFilter).child(alert.getKey()).removeValue();
            }
        });
        recyclerView.setAdapter(adapter);

        setupSortSpinner();
        setupCalendar();
        loadHistory();
    }

    private void setupSortSpinner() {
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Latest First", "Oldest First"});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCalendar() {
        if (calendarView != null) {
            // REQUIREMENT: Prevent future date selection
            calendarView.setMaxDate(System.currentTimeMillis());

            calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
                java.util.Calendar selected = java.util.Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                selected.set(java.util.Calendar.MILLISECOND, 0);
                selectedDayStartMs = selected.getTimeInMillis();
                if (selectedDateText != null) {
                    selectedDateText.setText("Showing cases on " +
                            new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(selected.getTime()));
                    selectedDateText.setVisibility(View.VISIBLE);
                }
                applyFilterAndSort();
            });
        }
    }

    private void loadHistory() {
        if (departmentFilter == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ResolvedAlerts").child(departmentFilter);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullHistoryList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AlertModel alert = ds.getValue(AlertModel.class);
                    if (alert != null) {
                        alert.setKey(ds.getKey());
                        fullHistoryList.add(alert);
                    }
                }
                applyFilterAndSort();
                updateCalendarBounds();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilterAndSort() {
        filteredList.clear();
        for (AlertModel alert : fullHistoryList) {
            long timestamp = getAlertTimestamp(alert);
            boolean match = true;
            
            if (selectedDayStartMs != null) {
                // If a date is selected, we only match alerts from that specific day.
                // Alerts with unknown timestamps (0) are excluded when a filter is active.
                if (timestamp > 0) {
                    match = timestamp >= selectedDayStartMs && timestamp < selectedDayStartMs + 24L * 60L * 60L * 1000L;
                } else {
                    match = false;
                }
            }

            if (match) filteredList.add(alert);
        }

        int sortPos = spinnerSort.getSelectedItemPosition();
        Collections.sort(filteredList, (a, b) -> {
            long tA = getAlertTimestamp(a);
            long tB = getAlertTimestamp(b);
            return (sortPos == 1) ? Long.compare(tA, tB) : Long.compare(tB, tA);
        });

        adapter.notifyDataSetChanged();
        
        // REQUIREMENT: State "No cases found" instead of leaving it blank or confused
        if (emptyTextView != null) {
            emptyTextView.setText("No Resolved Cases Found");
            emptyTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateCalendarBounds() {
        if (calendarView == null) return;
        
        long now = System.currentTimeMillis();
        calendarView.setMaxDate(now);

        if (fullHistoryList.isEmpty()) return;
        
        long min = Long.MAX_VALUE;
        long dataMax = 0;
        for (AlertModel alert : fullHistoryList) {
            long time = getAlertTimestamp(alert);
            if (time > 0) {
                min = Math.min(min, time);
                dataMax = Math.max(dataMax, time);
            }
        }
        
        if (min != Long.MAX_VALUE) {
            calendarView.setMinDate(min);
        }
        
        // Highlights the data range by restricting the calendar
        if (dataMax > 0) {
            calendarView.setMaxDate(Math.min(dataMax, now));
        }
    }

    private long getAlertTimestamp(AlertModel alert) {
        Object rawTime = alert.getTimestamp();
        if (rawTime instanceof Long) return (Long) rawTime;
        if (rawTime instanceof Double) return ((Double) rawTime).longValue();
        if (rawTime instanceof String) {
            try { return Long.parseLong((String) rawTime); } catch(Exception ignored){}
        }
        return 0;
    }
}
