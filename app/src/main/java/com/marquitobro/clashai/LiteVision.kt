package com.marquitobro.clashai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Rect
import java.nio.FloatBuffer
import kotlin.math.*

/** Local YOLO inference. Only card classes with known costs and enemy-colour
 * evidence are forwarded to the experimental tracker. */
class LiteVision(context: Context): AutoCloseable {
    data class Seen(val card:String,val cost:Int,val x:Float,val y:Float)
    data class Box(val x1:Float,val y1:Float,val x2:Float,val y2:Float,val score:Float,val cls:Int)
    private val env=OrtEnvironment.getEnvironment()
    private val session=OrtSession.SessionOptions().use {
        it.setIntraOpNumThreads(3)
        env.createSession(context.assets.open("arena.onnx").use { s->s.readBytes() },it)
    }
    private val labels=context.assets.open("arena-labels.txt").bufferedReader().use { it.readLines() }
    private val costs=context.assets.open("card-costs.tsv").bufferedReader().useLines { lines ->
        lines.mapNotNull { val p=it.split('\t'); if(p.size==2) p[1].toIntOrNull()?.let { n->p[0] to n } else null }.toMap()
    }
    private val size=640
    private fun canonical(s:String):String {
        val name=s.lowercase().trim().replace('_',' ').replace(Regex("[^a-z0-9 ]")," ").trim().replace(Regex(" +"),"-")
        val base=name.removeSuffix("-enemy").removeSuffix("-ally").removeSuffix("-red").removeSuffix("-blue")
            .removeSuffix("-evolution").removeSuffix("-evo").removeSuffix("-hero")
        return when(base) {
            "log"->"the-log"; "snowball"->"giant-snowball"; "skeleton-army-unit"->"skeleton-army"
            "goblin"->"goblins"; "archer"->"archers"; "barbarian"->"barbarians"; "minion"->"minions"
            "skeleton"->"skeletons"; "bat"->"bats"; "spear-goblin"->"spear-goblins"
            else->base
        }
    }
    private fun overlap(a:Box,b:Box):Float {
        val inter=max(0f,min(a.x2,b.x2)-max(a.x1,b.x1))*max(0f,min(a.y2,b.y2)-max(a.y1,b.y1))
        val union=(a.x2-a.x1)*(a.y2-a.y1)+(b.x2-b.x1)*(b.y2-b.y1)-inter
        return if(union>0) inter/union else 0f
    }
    private fun enemyEvidence(source:Bitmap,x:Float,y:Float,w:Float,h:Float):Boolean {
        val hsv=FloatArray(3); var red=0; var blue=0
        val left=(x-w*.7f).toInt().coerceIn(0,source.width-1)
        val right=(x+w*.7f).toInt().coerceIn(left+1,source.width)
        val top=(y-h*.9f).toInt().coerceIn(0,source.height-1)
        val bottom=(y-h*.12f).toInt().coerceIn(top+1,source.height)
        for(yy in top until bottom step 2) for(xx in left until right step 2) {
            Color.colorToHSV(source.getPixel(xx,yy),hsv)
            if(hsv[1]>.55f && hsv[2]>.38f) {
                if(hsv[0]>=333 || hsv[0]<=8) red++
                if(hsv[0] in 195f..230f) blue++
            }
        }
        return red>=4 && red>2*blue
    }
    fun detect(source:Bitmap):List<Seen> {
        val top=(source.height*.10).toInt()
        val bottom=(source.height*.81).toInt()
        val ch=bottom-top
        val scale=min(size.toFloat()/source.width,size.toFloat()/ch)
        val nw=(source.width*scale).roundToInt(); val nh=(ch*scale).roundToInt()
        val px=(size-nw)/2; val py=(size-nh)/2
        val input=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
        Canvas(input).apply {
            drawColor(Color.rgb(114,114,114))
            drawBitmap(source,Rect(0,top,source.width,bottom),Rect(px,py,px+nw,py+nh),
                android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        }
        val pixels=IntArray(size*size); input.getPixels(pixels,0,size,0,0,size,size); input.recycle()
        val data=FloatArray(3*pixels.size)
        for(i in pixels.indices) {
            val c=pixels[i]; data[i]=Color.red(c)/255f
            data[pixels.size+i]=Color.green(c)/255f; data[2*pixels.size+i]=Color.blue(c)/255f
        }
        val candidates=mutableListOf<Box>()
        OnnxTensor.createTensor(env,FloatBuffer.wrap(data),longArrayOf(1,3,size.toLong(),size.toLong())).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { output ->
                val batch=output[0].value as? Array<*> ?: return emptyList()
                val rows=batch.firstOrNull() as? Array<*> ?: return emptyList()
                val first=rows.firstOrNull() as? FloatArray ?: return emptyList()
                if(rows.size==labels.size+4) {
                    val values=rows.map { it as FloatArray }
                    for(i in first.indices) {
                        var cls=0; var conf=0f
                        for(k in labels.indices) if(values[k+4][i]>conf) { conf=values[k+4][i]; cls=k }
                        if(conf<.58f) continue
                        val x=values[0][i]; val y=values[1][i]; val w=values[2][i]; val h=values[3][i]
                        candidates.add(Box(x-w/2,y-h/2,x+w/2,y+h/2,conf,cls))
                    }
                } else if(first.size==6) {
                    for(row in rows) {
                        val r=row as FloatArray; val cls=r[5].toInt()
                        if(r[4]>=.58f && cls in labels.indices) candidates.add(Box(r[0],r[1],r[2],r[3],r[4],cls))
                    }
                } else throw IllegalStateException("Unsupported detector output")
            }
        }
        val keep=mutableListOf<Box>()
        for(b in candidates.sortedByDescending { it.score }.take(150)) {
            if(keep.none { it.cls==b.cls && overlap(it,b)>.45f }) keep.add(b)
            if(keep.size>=40) break
        }
        return keep.mapNotNull { b ->
            val label=labels[b.cls]; val name=canonical(label); val cost=costs[name] ?: return@mapNotNull null
            if(cost !in 1..9 || name=="mirror") return@mapNotNull null
            val x=((b.x1+b.x2)/2-px)/scale
            val y=((b.y1+b.y2)/2-py)/scale+top
            val w=(b.x2-b.x1)/scale; val h=(b.y2-b.y1)/scale
            if(x !in 0f..source.width.toFloat() || y !in top.toFloat()..bottom.toFloat() || w<3 || h<3) return@mapNotNull null
            if(!enemyEvidence(source,x,y,w,h)) return@mapNotNull null
            Seen(name,cost,x/source.width,y/source.height)
        }
    }
    override fun close(){ session.close() }
}
