package com.example.lovecounter.utils.handle

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

object PermissionUtils {

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (permission.isEmpty()) return true
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(
        activity: Activity,
        permissions: List<String>,
        onResult: (Boolean) -> Unit
    ) {
        val validPermissions = permissions.filter { it.isNotEmpty() }
        if (validPermissions.isEmpty()) {
            onResult(true)
            return
        }

        Dexter.withContext(activity)
            .withPermissions(validPermissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    onResult(report?.areAllPermissionsGranted() == true)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }
}
