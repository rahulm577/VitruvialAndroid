package com.example.vitruvial

import android.app.AlertDialog
import android.content.Context

/**
 * Confirmation dialog for delete operations
 */
class DeleteConfirmationDialog {
    companion object {
        /**
         * Show a confirmation dialog before deletion
         * 
         * @param context The context
         * @param title The dialog title
         * @param message The dialog message
         * @param onConfirm Callback when user confirms deletion
         */
        fun show(context: Context, title: String, message: String, onConfirm: () -> Unit) {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes") { _, _ ->
                    onConfirm()
                }
                .setNegativeButton("No", null)
                .setCancelable(true)
                .show()
        }
    }
} 