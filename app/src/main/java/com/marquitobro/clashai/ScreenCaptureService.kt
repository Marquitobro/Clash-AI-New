package com.marquitobro.clashai

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.abs

class ScreenCaptureService : Service() {
    private val ui=Handler(Looper.getMainLooper())
    private var worker: HandlerThread?=null
    private var projection: MediaProjection?=null
    private var display: VirtualDisplay?=null
    private var reader: ImageReader?=null
    private var panel: View?=null
    private var info: TextView?=null
    private var cycle: TextView?=null
    private var status: TextView?=null
    private var toggle: Button?=null
    @Volatile private var active=false
    @Volatile private var generation=0
    @Volatile private var vision: LiteVision?=null
    private var started=0L
    private var last=0L
    private var elixir=5.0
    private var count=0
    private var lastFrame=0L
    private var lastProcessed=0L
    private var seen=mutableListOf<Track>()
    private val plays=mutableListOf<LiteVision.Seen>()
    private data class Track(var unit: LiteVision.Seen,var last: Long,var hits: Int=1,var counted: Boolean=false)
    private fun now()=SystemClock.elapsedRealtime()
    private fun speed(at: Long): Int {
        val elapsed=(at-started)/1000
        return if(elapsed<120) 1 else if(elapsed<240) 2 else 3
    }
    private fun tick(at: Long) {
        if(!active) return
        var cursor=last
        while(cursor<at) {
            val end=minOf(at,cursor+250)
            elixir=(elixir+(end-cursor)*speed(cursor)/2800.0).coerceAtMost(10.0)
            cursor=end
        }
        last=at
    }
    private val refresh=object: Runnable {
        override fun run() {
            if(panel==null) return
            if(active && now()-started>300000) stopSession()
            tick(now())
            info?.text=if(active) "Elixir ≈ " + String.format(java.util.Locale.US,"%.1f",elixir) + "/10" else "Elixir: —"
            cycle?.text=if(plays.isEmpty()) "Ciclo: sin detecciones" else "Ciclo observado\n"+plays.takeLast(4).reversed().mapIndexed { i,c -> c.card+" · "+(4-i) }.joinToString("\n")
            if(active) status?.text=if(now()-lastFrame>3500) "Sin captura reciente" else "x"+speed(now())+" · "+count+" jugadas (12 tipos)"
            ui.postDelayed(this,250)
        }
    }
    private fun stopSession() {
        active=false; generation++; toggle?.text="Inicio"; status?.text="En pausa"
    }
    private fun updateUnits(units: List<LiteVision.Seen>,at: Long,token: Int) {
        if(!active || token!=generation) return
        lastFrame=at
        tick(now())
        seen.removeAll { at-it.last>2200 }
        val used=mutableSetOf<Track>()
        val newPlays=mutableListOf<LiteVision.Seen>()
        for(u in units) {
            val match=seen.filter { it !in used && abs(it.unit.x-u.x)<.07 && abs(it.unit.y-u.y)<.075 }
                .minByOrNull { abs(it.unit.x-u.x)+abs(it.unit.y-u.y) }
            if(match==null) {
                val t=Track(u,at); seen.add(t); used.add(t)
            } else {
                if(match.unit.card==u.card) match.hits++ else if(!match.counted) match.hits=1
                match.unit=u; match.last=at; used.add(match)
                if(match.hits>=2 && !match.counted) {
                    match.counted=true
                    val sameGroup=seen.any { it!==match && it.counted && it.unit.card==u.card &&
                        abs(it.unit.x-u.x)<.09 && abs(it.unit.y-u.y)<.09 && abs(it.last-at)<1000 }
                    val recently=plays.lastOrNull()?.let { it.card==u.card && at-lastProcessed<3500 } ?: false
                    if(!sameGroup && !recently) newPlays.add(u)
                }
            }
        }
        for(u in newPlays.distinctBy { it.card }) {
            elixir=(elixir-u.cost).coerceAtLeast(0.0)
            plays.add(u); count++; lastProcessed=at
        }
    }
    private fun showPanel() {
        val wm=getSystemService(WINDOW_SERVICE) as WindowManager
        fun dp(v:Int)=(resources.displayMetrics.density*v).toInt()
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(8),dp(4),dp(8),dp(4))
            setBackgroundColor(Color.argb(230,20,24,37))
        }
        val head=TextView(this).apply { text="TRACKER LITE · mover"; setTextColor(Color.LTGRAY); textSize=10f }
        info=TextView(this).apply { text="Elixir: —"; setTextColor(Color.rgb(230,168,255)); textSize=18f; setTypeface(null,Typeface.BOLD) }
        cycle=TextView(this).apply { text="Ciclo: sin detecciones"; setTextColor(Color.WHITE); textSize=11f; maxWidth=dp(160) }
        status=TextView(this).apply { text="Cargando modelo…"; setTextColor(Color.LTGRAY); textSize=10f }
        val controls=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        toggle=Button(this).apply {
            text="Inicio"; isEnabled=false; textSize=11f; minWidth=0; minimumWidth=0
            setOnClickListener {
                if(active) stopSession() else {
                    generation++; started=now(); last=started; elixir=5.0; count=0
                    lastFrame=started; lastProcessed=0; seen.clear(); plays.clear()
                    active=true; text="Pausa"
                }
            }
        }
        val close=Button(this).apply { text="×"; textSize=14f; minWidth=0; minimumWidth=0; setOnClickListener { stopSelf() } }
        controls.addView(toggle); controls.addView(close)
        root.addView(head); root.addView(info); root.addView(cycle); root.addView(status); root.addView(controls)
        val params=WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.START; x=dp(8); y=dp(70)
        }
        var sx=0f; var sy=0f; var px=0; var py=0
        head.setOnTouchListener { _,e ->
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx=e.rawX; sy=e.rawY; px=params.x; py=params.y; true }
                MotionEvent.ACTION_MOVE -> { params.x=(px+e.rawX-sx).toInt(); params.y=(py+e.rawY-sy).toInt(); wm.updateViewLayout(root,params); true }
                else -> true
            }
        }
        wm.addView(root,params); panel=root; ui.post(refresh)
    }
    override fun onBind(intent:Intent?): IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="stop") { stopSelf(); return START_NOT_STICKY }
        if(projection!=null) return START_NOT_STICKY
        val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("data",Intent::class.java)
            else { @Suppress("DEPRECATION") intent?.getParcelableExtra<Intent>("data") }
        if(data==null || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("capture","Captura",NotificationManager.IMPORTANCE_LOW))
        val stop=PendingIntent.getService(this,1,Intent(this,ScreenCaptureService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE)
        val n=Notification.Builder(this,"capture").setContentTitle("Clash Tracker Lite").setContentText("Captura local")
            .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).addAction(Notification.Action.Builder(null,"Cerrar",stop).build()).build()
        if(Build.VERSION.SDK_INT>=29) startForeground(1,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1,n)
        try {
            val p=getSystemService(MediaProjectionManager::class.java).getMediaProjection(intent?.getIntExtra("code",0) ?: 0,data)
            projection=p
            p.registerCallback(object:MediaProjection.Callback(){ override fun onStop(){ stopSelf() } },ui)
            showPanel()
            val thread=HandlerThread("tracker-vision").apply { start() }; worker=thread
            val bg=Handler(thread.looper)
            bg.post {
                try { vision=LiteVision(this); ui.post { status?.text="Listo · 12 tipos"; toggle?.isEnabled=true } }
                catch(e:Exception) { ui.post { status?.text="Error del modelo: "+e.javaClass.simpleName } }
            }
            val dm=android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
            val w=minOf(720,dm.widthPixels); val h=(dm.heightPixels.toDouble()*w/dm.widthPixels).toInt()
            val r=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2); reader=r
            var lastRead=0L
            r.setOnImageAvailableListener({ rr ->
                val image=try { rr.acquireLatestImage() } catch(_:Exception){ null } ?: return@setOnImageAvailableListener
                try {
                    val v=vision; val at=now(); val token=generation
                    if(active && v!=null && at-lastRead>=550) {
                        lastRead=at
                        val plane=image.planes[0]; val stride=plane.rowStride/plane.pixelStride
                        val raw=Bitmap.createBitmap(stride,h,Bitmap.Config.ARGB_8888)
                        try {
                            raw.copyPixelsFromBuffer(plane.buffer)
                            val frame=Bitmap.createBitmap(raw,0,0,w,h)
                            try { val units=v.detect(frame); ui.post { updateUnits(units,at,token) } }
                            finally { if(frame!==raw) frame.recycle() }
                        } finally { raw.recycle() }
                    }
                } catch(e:Exception) { ui.post { stopSession(); status?.text="Error de captura: "+e.javaClass.simpleName } }
                finally { image.close() }
            },bg)
            display=p.createVirtualDisplay("TrackerLite",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,r.surface,null,bg)
        } catch(e:Exception) { Toast.makeText(this,"No se pudo iniciar: "+e.javaClass.simpleName,Toast.LENGTH_LONG).show(); stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        active=false; generation++; ui.removeCallbacks(refresh)
        panel?.let { try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch(_:Exception){} }; panel=null
        display?.release(); display=null
        reader?.setOnImageAvailableListener(null,null); reader?.close(); reader=null
        projection?.stop(); projection=null
        worker?.let { thread -> Handler(thread.looper).post { vision?.close(); vision=null; thread.quitSafely() } }
        worker=null
        super.onDestroy()
    }
}
