package com.hugopello.smartclock.traffic

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

/**
 * A key shipped inside an APK can be extracted. Restricting it in Google Cloud to "Android apps"
 * (package name + SHA-1 of the signing certificate) makes Google reject calls that don't identify
 * as this app. For REST calls (no Google SDK) the app has to send that identity itself, in these headers.
 */
class AppIdentityInterceptor(context: Context) : Interceptor {

    private val packageName = context.packageName
    private val certSha1: String? by lazy { signingCertSha1(context) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Android-Package", packageName)
            .apply { certSha1?.let { header("X-Android-Cert", it) } }
            .build()
        return chain.proceed(request)
    }
}

// PackageManagerGetSignatures: that lint warns against *trusting* GET_SIGNATURES to verify other apps.
// Here we only read our own certificate to send its fingerprint; it is the only option below Android 9.
@SuppressLint("PackageManagerGetSignatures")
private fun signingCertSha1(context: Context): String? {
    val pm = context.packageManager
    val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
    }
    val cert = signatures?.firstOrNull() ?: return null
    return MessageDigest.getInstance("SHA-1").digest(cert.toByteArray()).joinToString("") { "%02X".format(it) }
}
