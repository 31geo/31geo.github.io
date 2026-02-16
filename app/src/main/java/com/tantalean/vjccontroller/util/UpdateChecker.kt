package com.tantalean.vjccontroller.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/**
 * Verificador simple de actualizaciones remotas.
 * - Lee `version.json` en remoto: { "latest_version":"1.2.0", "update_url":"https://..." }
 * - Compara versiones semanticamente y muestra diálogo si hay actualización.
 */
object UpdateChecker {
    private val client = OkHttpClient()

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.0.0"
        } catch (t: Throwable) {
            "0.0.0"
        }
    }

    fun checkForUpdate(context: Context, url: String = context.getString(com.tantalean.vjccontroller.R.string.version_json_url)) {
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Silencioso: no hacemos nada si falla la conexión
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    try {
                        val json = JSONObject(body)
                        val latestVersion = json.optString("latest_version", "0.0.0")
                        val updateUrl = json.optString("update_url", "")

                        val currentVersion = getAppVersion(context)

                        if (isVersionNewer(latestVersion, currentVersion)) {
                            (context as? android.app.Activity)?.runOnUiThread {
                                showUpdateDialog(context, updateUrl, latestVersion)
                            }
                        }
                    } catch (t: Throwable) {
                        // ignore JSON errors
                    }
                }
            }
        })
    }

    // Comparación semántica simple: 1.2.0 > 1.1.9
    private fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val l = latest.split('.')
            val c = current.split('.')
            val max = maxOf(l.size, c.size)
            for (i in 0 until max) {
                val lv = l.getOrNull(i)?.toIntOrNull() ?: 0
                val cv = c.getOrNull(i)?.toIntOrNull() ?: 0
                if (lv > cv) return true
                if (lv < cv) return false
            }
        } catch (t: Throwable) {
            return latest != current
        }
        return false
    }

    private fun showUpdateDialog(context: Context, url: String, latestVersion: String) {
        AlertDialog.Builder(context)
            .setTitle("Actualización disponible")
            .setMessage("Versión $latestVersion disponible. ¿Deseas actualizar ahora?")
            .setCancelable(true)
            .setPositiveButton("Actualizar") { _, _ ->
                if (url.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }
}
