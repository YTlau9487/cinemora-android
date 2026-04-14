package com.cinemora.movieorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class DialogHelper {

    public interface DialogCallback {
        void onAction();
    }

    /**
     * Shows a reusable custom confirmation dialog.
     *
     * @param context         The activity context.
     * @param title           Title text.
     * @param message         Message text.
     * @param confirmText     Text for the right button.
     * @param cancelText      Text for the left button.
     * @param confirmCallback Callback for the confirm action.
     * @param cancelCallback  Callback for the cancel action.
     */
    public static void showConfirmationDialog(
            Context context,
            String title,
            String message,
            String confirmText,
            String cancelText,
            DialogCallback confirmCallback,
            DialogCallback cancelCallback) {

        // Inflate custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirmation, null);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Make the background transparent so the rounded corners of the CardView are visible
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Initialize views
        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.dialog_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        // Set content
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnCancel.setText(cancelText);

        // Set button listeners
        btnConfirm.setOnClickListener(v -> {
            if (confirmCallback != null) {
                confirmCallback.onAction();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (cancelCallback != null) {
                cancelCallback.onAction();
            }
            dialog.dismiss();
        });

        dialog.show();

        // Force the dialog to use the layout's specific width (280dp) at runtime
        if (dialog.getWindow() != null) {
            int width = (int) (280 * context.getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
