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
    private final List<Withdrawal> withdrawalHistory = new ArrayList<>();
    private FirebaseFirestore db;
    private String documentId = Utils.getDocumentId();

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
        binding.etUpiId.setText("");
        binding.etWithdrawAmount.setText("");
        userBalance -= amount;
        binding.tvUserBalance.setText("Balance: ₹" + userBalance);
        Utils.setTotalCash(userBalance);

        Withdrawal withdrawal = new Withdrawal(amount, upiId, "Pending⏳");
        withdrawalHistory.add(withdrawal);
        binding.tvNoHistory.setVisibility(View.GONE);
        binding.tvWithdrawDisclaimer.setVisibility(View.VISIBLE);
        binding.tableHeader.setVisibility(View.VISIBLE);
        binding.rvWithdrawHistory.getAdapter().notifyItemInserted(withdrawalHistory.size() - 1);


        storeWithdrawalInFirestore(amount, upiId);

        Utils.showToast(requireContext(), "Withdrawal of ₹" + amount + " successful!");
    }

    private void storeWithdrawalInFirestore(double amount, String upiId) {

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("user", documentId);
        paymentData.put("upiId", upiId);
        paymentData.put("amount", amount);
        paymentData.put("time", FieldValue.serverTimestamp());
        paymentData.put("status", "Pending");

        db.collection("Payments").document(documentId)
                .collection("Withdrawals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int nextId = queryDocumentSnapshots.size() + 1;
                    db.collection("Payments").document(documentId)
                            .collection("Withdrawals")
                            .document(String.valueOf(nextId))
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
                            String status = "Pending ⏳";
                            if (document.getString("status") != null) {
                                status = document.getString("status").equals("Completed") ? "Completed ✅" : "Pending ⏳";
                            }
                            Withdrawal withdrawal = new Withdrawal(
                                    document.getDouble("amount"),
                                    document.getString("upiId"),
                                    status
                            );
                            withdrawalHistory.add(withdrawal);
                        }
                        binding.rvWithdrawHistory.getAdapter().notifyDataSetChanged();
                        binding.tvNoHistory.setVisibility(View.GONE);
                        binding.tvWithdrawDisclaimer.setVisibility(View.VISIBLE);
                        binding.tableHeader.setVisibility(View.VISIBLE);

                    }else {
                        binding.tvNoHistory.setVisibility(View.VISIBLE);
                        binding.tvWithdrawDisclaimer.setVisibility(View.GONE);
                        binding.tableHeader.setVisibility(View.GONE);

                    }
                })
                .addOnFailureListener(e -> Log.e("WithdrawFragment", "Error fetching withdrawal history", e));
    }

    private static class WithdrawalHistoryAdapter extends RecyclerView.Adapter<WithdrawalHistoryAdapter.ViewHolder> {

        private final List<Withdrawal> history;
        WithdrawalHistoryAdapter(List<Withdrawal> history) {
            this.history = history;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_withdrawal_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Withdrawal withdrawal = history.get(position);
            holder.bind(position + 1, withdrawal);
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView tvSerialNumber;
            private final TextView tvAmount;
            private final TextView tvUpiId;
            private final TextView tvStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSerialNumber = itemView.findViewById(R.id.tvSerialNumber);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvUpiId = itemView.findViewById(R.id.tvUpiId);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }

            void bind(int serialNumber, Withdrawal withdrawal) {
                String sno = serialNumber + ") ";
                tvSerialNumber.setText(sno);
                tvAmount.setText("₹" + withdrawal.getAmount());
                tvUpiId.setText(withdrawal.getUpiId());
                tvStatus.setText(withdrawal.getStatus());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

