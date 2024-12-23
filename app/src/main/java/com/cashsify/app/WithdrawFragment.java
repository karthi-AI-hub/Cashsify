package com.cashsify.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cashsify.app.databinding.FragmentWithdrawBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WithdrawFragment extends Fragment {

    private FragmentWithdrawBinding binding;
    private int userBalance = Utils.getTotalCash();
    private static final int MIN_WITHDRAW_AMOUNT = 150;
    private final List<String> withdrawalHistory = new ArrayList<>();
    FirebaseFirestore db;
    String documentId = Utils.getDocumentId();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentWithdrawBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        binding.tvUserBalance.setText("Balance: ₹" + userBalance);
        binding.rvWithdrawHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvWithdrawHistory.setAdapter(new WithdrawalHistoryAdapter(withdrawalHistory));
        binding.btnSubmitWithdraw.setOnClickListener(v -> handleWithdrawal());
        fetchWithdrawalHistory();
        return root;
    }

    private void handleWithdrawal() {
        String amountStr = binding.etWithdrawAmount.getText().toString().trim();
        String upiId = binding.etUpiId.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            Utils.showToast(requireActivity(), "Withdraw amount cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(upiId)) {
            Utils.showToast(requireContext(), "Please enter your UPI ID");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            Utils.showToast(requireContext(), "Invalid amount entered");
            return;
        }

        if (amount < MIN_WITHDRAW_AMOUNT) {
            Utils.showToast(requireContext(), "Amount must be atleast ₹" + MIN_WITHDRAW_AMOUNT);
            return;
        }

        if (amount > userBalance) {
            Utils.showToast(requireContext(), "Insufficient balance");
            return;
        }

        processUpiWithdrawal(amount, upiId);
    }

    private void processUpiWithdrawal(int amount, String upiId) {
        userBalance -= amount;
        binding.tvUserBalance.setText("Balance: ₹" + userBalance);
        Utils.setTotalCash(userBalance);

        String historyEntry = "Withdrawal of ₹" + amount + " to " + upiId + " - Success ✅\n";
        withdrawalHistory.add(historyEntry);
        binding.tvNoHistory.setVisibility(View.VISIBLE);
        binding.rvWithdrawHistory.getAdapter().notifyItemInserted(withdrawalHistory.size() - 1);


        storeWithdrawalInFirestore(amount, upiId);

        Utils.showToast(requireContext(), "Withdrawal of ₹" + amount + " successful!");
    }

    private void storeWithdrawalInFirestore(double amount, String upiId) {

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("user", documentId);
        paymentData.put("upiId", upiId);
        paymentData.put("amount", amount);
        paymentData.put("time", FieldValue.serverTimestamp());  // Add a timestamp for when the withdrawal happens

        db.collection("Payments").document(documentId)
                .collection("Withdrawals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int nextId = queryDocumentSnapshots.size() + 1; // Determine the next numeric ID
                    db.collection("Payments").document(documentId)
                            .collection("Withdrawals")
                            .document(String.valueOf(nextId)) // Use the numeric ID as the document ID
                            .set(paymentData)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("WithdrawFragment", "Payment successfully stored with ID: " + nextId))
                            .addOnFailureListener(e ->
                                    Log.e("WithdrawFragment", "Error storing payment", e));
                })
                .addOnFailureListener(e ->
                        Log.e("WithdrawFragment", "Error fetching withdrawal history to determine next ID", e));
    }

    private void fetchWithdrawalHistory() {
        db.collection("Payments").document(documentId)
                .collection("Withdrawals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    withdrawalHistory.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String historyEntry = "Withdrawal of ₹" + document.getDouble("amount") + " to " + document.getString("upiId") + " - Success ✅\n";
                            withdrawalHistory.add(historyEntry);
                        }
                        binding.rvWithdrawHistory.getAdapter().notifyDataSetChanged();
                    }else {
                        binding.tvNoHistory.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("WithdrawFragment", "Error fetching withdrawal history", e));
    }
    private static class WithdrawalHistoryAdapter extends RecyclerView.Adapter<WithdrawalHistoryAdapter.ViewHolder> {

        private final List<String> history;

        WithdrawalHistoryAdapter(List<String> history) {
            this.history = history;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(history.get(position));
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView textView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }

            void bind(String text) {
                textView.setText(text);
            }
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}