package com.example.jerapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<AlertModel> fullHistoryList = new ArrayList<>();
    private List<AlertModel> historyList = new ArrayList<>();
    private TextView emptyTextView;
    private android.widget.Spinner spinnerSort;
    private android.widget.CalendarView calendarView;
    private TextView selectedDateText;
    private Long selectedDayStartMs = null;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = view.findViewById(R.id.historyRecyclerView);
        emptyTextView = view.findViewById(R.id.tvEmptyHistory);
        calendarView = view.findViewById(R.id.historyCalendar);
        selectedDateText = view.findViewById(R.id.tvSelectedHistoryDate);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(historyList, new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AlertModel alert) {
                Intent intent = new Intent(getActivity(), UserHistoryDetailActivity.class);
                intent.putExtra("alert_key", alert.getKey());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(AlertModel alert) {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Delete History")
                        .setMessage("Are you sure you want to delete this record?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            String uid = getEffectiveUid();
                            if (uid != null && alert.getKey() != null) {
                                FirebaseDatabase.getInstance().getReference("UserHistory")
                                        .child(uid).child(alert.getKey()).removeValue();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        setupSortSpinner(view);
        setupCalendar();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadUserHistory);
        }

        loadUserHistory();
        return view;
    }

    private String getEffectiveUid() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().isAnonymous())) {
            uid = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("guest_uid", null);
        }
        return uid;
    }

    private void setupSortSpinner(View view) {
        spinnerSort = view.findViewById(R.id.spinnerSort);
        if (spinnerSort != null) {
            android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Latest First", "Oldest First"});
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSort.setAdapter(spinnerAdapter);

            spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    applyFilterAndSort(position);
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
    }

    private void setupCalendar() {
        if (calendarView != null) {
            // REQUIREMENT 5: Prevent Picking Future Dates
            calendarView.setMaxDate(System.currentTimeMillis());

            calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
                java.util.Calendar selected = java.util.Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                selected.set(java.util.Calendar.MILLISECOND, 0);
                selectedDayStartMs = selected.getTimeInMillis();
                if (selectedDateText != null) {
                    selectedDateText.setText("Showing emergencies on " +
                            new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(selected.getTime()));
                    selectedDateText.setVisibility(View.VISIBLE);
                }
                applyFilterAndSort(spinnerSort != null ? spinnerSort.getSelectedItemPosition() : 0);
            });
        }
    }

    private void loadUserHistory() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        
        String uid = getEffectiveUid();
        if (uid == null) {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            if (emptyTextView != null) {
                emptyTextView.setText("No Emergency History Found");
                emptyTextView.setVisibility(View.VISIBLE);
            }
            return;
        }

        FirebaseDatabase.getInstance().getReference("UserHistory").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullHistoryList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (ds.getValue() instanceof java.util.Map) {
                                AlertModel alert = ds.getValue(AlertModel.class);
                                if (alert != null) {
                                    alert.setKey(ds.getKey());
                                    fullHistoryList.add(alert);
                                }
                            }
                        }
                        applyFilterAndSort(spinnerSort != null ? spinnerSort.getSelectedItemPosition() : 0);
                        updateCalendarBounds(); // REQUIREMENT 3: Focus range to highlight data dates
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void applyFilterAndSort(int filterType) {
        historyList.clear();
        for (AlertModel alert : fullHistoryList) {
            long timeLong = getAlertTimestamp(alert);
            boolean match = true;
            
            if (selectedDayStartMs != null) {
                // REQUIREMENT: If a date is picked, ONLY show matches for that date. 
                // Ignore "unknown" emergencies (timestamp 0) during date filtering.
                if (timeLong > 0) {
                    match = timeLong >= selectedDayStartMs && timeLong < selectedDayStartMs + 24L * 60L * 60L * 1000L;
                } else {
                    match = false;
                }
            }

            if (match) historyList.add(alert);
        }

        java.util.Collections.sort(historyList, (a, b) -> {
            long tA = getAlertTimestamp(a);
            long tB = getAlertTimestamp(b);
            return (filterType == 1) ? Long.compare(tA, tB) : Long.compare(tB, tA);
        });

        adapter.notifyDataSetChanged();
        if (emptyTextView != null) {
            emptyTextView.setText("No Emergency History Found"); // REQUIREMENT 4
            emptyTextView.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateCalendarBounds() {
        if (calendarView == null) return;
        
        long now = System.currentTimeMillis();
        calendarView.setMaxDate(now);

        if (fullHistoryList.isEmpty()) return;
        
        long min = Long.MAX_VALUE;
        long max = 0;
        for (AlertModel alert : fullHistoryList) {
            long time = getAlertTimestamp(alert);
            if (time > 0) {
                min = Math.min(min, time);
                max = Math.max(max, time);
            }
        }
        
        if (min != Long.MAX_VALUE) {
            calendarView.setMinDate(min);
        }
        
        // Highlights valid data range by restricting the calendar view
        if (max > 0) {
            calendarView.setMaxDate(Math.min(max, now));
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
