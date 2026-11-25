package com.marquitobro.clashai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.nio.FloatBuffer
import kotlin.math.*

/** Badge-based vision, adapted from bytelabs-oss/clash-jev (MIT).
 * Model and attribution are packaged at build time. */
class LiteVision(context: Context): AutoCloseable {
    data class Seen(val card: String, val cost: Int, val x: Float, val y: Float)
    private val env = OrtEnvironment.getEnvironment()
    private val session = OrtSession.SessionOptions().use { opts ->
        opts.setIntraOpNumThreads(2)
        env.createSession(context.assets.open("troop_model.onnx").use { it.readBytes() }, opts)
    }
    private val names = arrayOf("Arqueras","Bombardero","Gigante","Jaula del forzudo","Choza de duendes","Duendes","Caballero","Mini P.E.K.K.A","Esbirros","Mosquetera","Esqueletos","Duendes con lanza")
    private val costs = intArrayOf(3,2,5,4,5,2,3,4,3,4,1,2)
    private val mean = floatArrayOf(.406f,.456f,.485f)
    private val std = floatArrayOf(.225f,.224f,.229f)
    private fun classify(frame: Bitmap, x: Int, y: Int): Pair<Int,Double>? {
        val w=frame.width; val h=frame.height
        val l=(x-.045*w).toInt().coerceIn(0,w-1)
        val r=(x+.045*w).toInt().coerceIn(l+1,w)
        val t=(y-.0054*h).toInt().coerceIn(0,h-1)
        val b=(y+.0786*h).toInt().coerceIn(t+1,h)
        val crop=Bitmap.createBitmap(frame,l,t,r-l,b-t)
        val small=Bitmap.createScaledBitmap(crop,96,96,true)
        val pixels=IntArray(96*96)
        small.getPixels(pixels,0,96,0,0,96,96)
        if(small!==crop) small.recycle()
        if(crop!==frame) crop.recycle()
        val data=FloatArray(3*pixels.size)
        for(i in pixels.indices) {
            val c=pixels[i]
            val v=intArrayOf(Color.blue(c),Color.green(c),Color.red(c))
            for(k in 0..2) data[k*pixels.size+i]=(v[k]/255f-mean[k])/std[k]
        }
        OnnxTensor.createTensor(env,FloatBuffer.wrap(data),longArrayOf(1,3,96,96)).use { input ->
            session.run(mapOf(session.inputNames.first() to input)).use { output ->
                val rows=output[0].value as? Array<*> ?: return null
                val logits=rows.firstOrNull() as? FloatArray ?: return null
                if(logits.size!=names.size) return null
                val best=logits.indices.maxByOrNull { logits[it] } ?: return null
                val conf=1.0/logits.sumOf { exp((it-logits[best]).toDouble()) }
                return best to conf
            }
        }
    }
    fun detect(source: Bitmap): List<Seen> {
        // Preserve pixel aspect; the classifier was trained on a 419x633 arena.
        val w=419
        val h=(source.height.toDouble()*w/source.width).roundToInt().coerceAtLeast(1)
        val frame=Bitmap.createScaledBitmap(source,w,h,true)
        try {
            val pixels=IntArray(w*h); frame.getPixels(pixels,0,w,0,0,w,h)
            val mask=BooleanArray(w*h); val white=BooleanArray(w*h)
            val hsv=FloatArray(3)
            val low=(h*.13).toInt(); val high=(h*.79).toInt()
            for(y in low until high) for(x in 0 until w) {
                val i=y*w+x
                Color.colorToHSV(pixels[i],hsv)
                mask[i]=hsv[0] in 334f..350f && hsv[1]>=.39f
                white[i]=hsv[1]<.16f && hsv[2]>.92f
            }
            // Small dilation bridges the dark outlines of badge digits.
            val grown=BooleanArray(mask.size)
            for(y in low+1 until high-1) for(x in 1 until w-1) if(mask[y*w+x]) {
                for(dy in -1..1) for(dx in -1..1) grown[(y+dy)*w+x+dx]=true
            }
            val queue=IntArray(mask.size); val out=mutableListOf<Seen>()
            for(i in low*w until high*w) {
                if(!grown[i]) continue
                var head=0; var tail=1; queue[0]=i; grown[i]=false
                var x0=i%w; var x1=x0; var y0=i/w; var y1=y0
                while(head<tail) {
                    val j=queue[head++]; val x=j%w; val y=j/w
                    x0=min(x0,x); x1=max(x1,x); y0=min(y0,y); y1=max(y1,y)
                    for(dy in -1..1) for(dx in -1..1) {
                        val nx=x+dx; val ny=y+dy
                        if(nx !in 0 until w || ny !in 0 until h) continue
                        val k=ny*w+nx
                        if(grown[k]) { grown[k]=false; queue[tail++]=k }
                    }
                }
                val side=min(x1-x0+1,y1-y0+1)
                if(side !in 8..15 || y1-y0+1>side*2.5) continue
                var digits=0; var color=0
                for(y in y0 until min(y0+side,h)) for(x in x0 until min(x0+side,w)) {
                    if(white[y*w+x]) digits++
                    if(mask[y*w+x]) color++
                }
                if(digits<5 || color<side*side*.22) continue
                val x=x0+side/2; val y=y0+side/2
                val match=classify(frame,x,y) ?: continue
                if(match.second>=.75) {
                    val k=match.first
                    out.add(Seen(names[k],costs[k],x.toFloat()/w,y.toFloat()/h))
                }
                if(out.size>=12) break
            }
            return out
        } finally { if(frame!==source) frame.recycle() }
    }
    override fun close() { session.close() }
}
