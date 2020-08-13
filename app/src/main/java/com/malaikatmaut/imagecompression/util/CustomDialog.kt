package com.malaikatmaut.imagecompression.util

import android.content.Context
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.malaikatmaut.imagecompression.R

class CustomDialog(context: Context) {
    private val loadingDialog = MaterialAlertDialogBuilder(context)
        .setView(View.inflate(context, R.layout.custom_loading_dialog, null))
        .setCancelable(false)
        .create()

    fun showLoadingDialog() = loadingDialog.show()

    fun closeLoadingDialog() = loadingDialog.dismiss()

    private val successDialog = MaterialAlertDialogBuilder(context)
        .setView(View.inflate(context, R.layout.custom_success_dialog, null))
        .setTitle(context.getString(R.string.process_finished))
        .setPositiveButton(context.getString(R.string.done)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        .setCancelable(false)
        .create()

    fun showSuccessDialog(message: String) {
        successDialog.setMessage(message)
        successDialog.show()
    }
}