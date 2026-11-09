package com.example.ipadlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class AppInfo(val label: String, val pkg: String, val icon: Drawable, val intent: Intent)

class MainActivity : Activity() {
    private var all = listOf<AppInfo>()
    private lateinit var adapter: AppAdapter
    private lateinit var search: EditText
    private lateinit var dock: LinearLayout
    private lateinit var recycler: RecyclerView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun glass(radius: Int, alpha: Int) = GradientDrawable().apply {
        setColor(Color.argb(alpha, 255, 255, 255))
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(70, 255, 255, 255))
    }

    private fun squircle(size: Float): Path {
        val path = Path(); val r = size / 2f; val n = 5.0; val steps = 160
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val c = Math.cos(t); val s = Math.sin(t)
            val x = r + r * Math.signum(c) * Math.pow(Math.abs(c), 2.0 / n)
            val y = r + r * Math.signum(s) * Math.pow(Math.abs(s), 2.0 / n)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close(); return path
    }

    private fun iosIcon(src: Drawable): Drawable {
        val size = dp(120)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val path = squircle(size.toFloat())
        c.save(); c.clipPath(path)
        if (Build.VERSION.SDK_INT >= 26 && src is AdaptiveIconDrawable) {
            val bo = (size * 0.25f).toInt(); val fo = (size * 0.15f).toInt()
            src.background?.apply { setBounds(-bo, -bo, size + bo, size + bo); draw(c) }
            src.foreground?.apply { setBounds(-fo, -fo, size + fo, size + fo); draw(c) }
        } else {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, size.toFloat(), Color.WHITE, Color.rgb(225, 228, 235), Shader.TileMode.CLAMP) }
            c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
            val m = (size * 0.14f).toInt()
            src.setBounds(m, m, size - m, size - m); src.draw(c)
        }
        val gloss = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, size.toFloat(),
                intArrayOf(Color.argb(55, 255, 255, 255), Color.argb(0, 255, 255, 255), Color.argb(28, 0, 0, 0)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), gloss)
        c.restore()
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(1).toFloat(); color = Color.argb(45, 255, 255, 255) })
        return BitmapDrawable(resources, bmp)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(40), dp(48), dp(20))
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL }
        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = glass(28, 55)
            setPadding(dp(28), dp(18), dp(28), dp(18))
        }
        clockCard.addView(TextClock(this).apply {
            format12Hour = "h:mm"; format24Hour = "HH:mm"
            textSize = 48f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        })
        clockCard.addView(TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"; format24Hour = "EEEE, d MMMM"
            textSize = 15f; setTextColor(Color.argb(220, 255, 255, 255))
        })
        top.addView(clockCard)
        root.addView(top)

        search = EditText(this).apply {
            hint = "Search"; setHintTextColor(Color.argb(200, 255, 255, 255)); setTextColor(Color.WHITE)
            background = glass(20, 50); setSingleLine(); textSize = 15f; gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = filter(s.toString())
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(dp(260), dp(40)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(20); bottomMargin = dp(12)
        })

        adapter = AppAdapter()
        recycler = RecyclerView(this).apply {
            val span = (resources.displayMetrics.widthPixels / dp(130)).coerceIn(4, 9)
            layoutManager = GridLayoutManager(this@MainActivity, span)
            this.adapter = this@MainActivity.adapter
            clipToPadding = false
        }
        root.addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = glass(34, 60); setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        root.addView(dock, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL })

        setContentView(root)
    }

    override fun onResume() { super.onResume(); loadApps() }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i); search.setText(""); recycler.scrollToPosition(0)
    }

    override fun onBackPressed() { }

    private fun loadApps() {
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        all = packageManager.queryIntentActivities(q, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName,
                    iosIcon(it.loadIcon(packageManager)),
                    packageManager.getLaunchIntentForPackage(it.activityInfo.packageName)
                        ?: Intent(Intent.ACTION_MAIN).setClassName(it.activityInfo.packageName, it.activityInfo.name))
            }.sortedBy { it.label.lowercase() }
        filter(search.text.toString())
        buildDock()
    }

    private fun filter(q: String) {
        adapter.items = if (q.isBlank()) all else all.filter { it.label.contains(q, true) }
        adapter.notifyDataSetChanged()
    }

    private fun buildDock() {
        dock.removeAllViews()
        val prefs = listOf("phone", "messag", "chrome", "camera", "gallery", "settings")
        val picks = prefs.mapNotNull { p -> all.firstOrNull { it.pkg.contains(p, true) } }
            .ifEmpty { all.take(5) }.distinctBy { it.pkg }
        picks.forEach { app ->
            val iv = ImageView(this).apply { setImageDrawable(app.icon)
                setOnClickListener { startActivity(app.intent) } }
            dock.addView(iv, LinearLayout.LayoutParams(dp(68), dp(68)).apply { marginStart = dp(8); marginEnd = dp(8) })
        }
    }

    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        var items = listOf<AppInfo>()
        inner class VH(val col: LinearLayout, val iv: ImageView, val tv: TextView) : RecyclerView.ViewHolder(col)

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val col = LinearLayout(p.context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(4), dp(14), dp(4), dp(14))
                layoutParams = RecyclerView.LayoutParams(-1, -2)
            }
            val iv = ImageView(p.context)
            val tv = TextView(p.context).apply {
                setTextColor(Color.WHITE); textSize = 12f; maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END; gravity = Gravity.CENTER
                setShadowLayer(4f, 0f, 1f, Color.argb(120, 0, 0, 0))
            }
            col.addView(iv, LinearLayout.LayoutParams(dp(76), dp(76)))
            col.addView(tv, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            return VH(col, iv, tv)
        }

        override fun onBindViewHolder(h: VH, i: Int) {
            val a = items[i]
            h.iv.setImageDrawable(a.icon); h.tv.text = a.label
            h.col.setOnClickListener { startActivity(a.intent) }
            h.col.setOnLongClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${a.pkg}"))); true }
        }
        override fun getItemCount() = items.size
    }
}
