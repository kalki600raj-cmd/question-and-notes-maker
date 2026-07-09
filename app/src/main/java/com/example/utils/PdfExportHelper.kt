package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.StudyNote
import com.example.data.StudyQuestion
import com.example.data.MockTest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {

    fun exportNoteToPdf(context: Context, note: StudyNote, onComplete: (File) -> Unit) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = Color.rgb(103, 58, 183) // Neon Purple theme
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubHeader = Paint().apply {
            color = Color.rgb(3, 169, 244) // Bright Blue
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBorder = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Draw header
        canvas.drawText("StudySlay Premium Study Pack 🧠✨", 30f, 50f, paintHeader)
        canvas.drawLine(30f, 60f, 565f, 60f, paintHeader.apply { strokeWidth = 2f })

        // Note Info
        paintText.textSize = 14f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Title: ${note.title}", 30f, 90f, paintText)
        
        paintText.textSize = 11f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Category: ${note.noteType.replace("_", " ").uppercase()} • Created on StudySlay fr", 30f, 110f, paintText)
        
        canvas.drawLine(30f, 120f, 565f, 120f, paintBorder)

        var yPos = 145f

        // Let's print the detailed content with simple wrapping
        paintText.textSize = 11f
        paintText.typeface = Typeface.DEFAULT
        
        val contentLines = note.content.split("\n")
        canvas.drawText("Notes & Details:", 30f, yPos, paintSubHeader)
        yPos += 20f

        for (line in contentLines) {
            if (yPos > 780f) {
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPos = 50f
            }
            
            // Text wrapping
            val words = line.split(" ")
            var lineBuilder = ""
            for (word in words) {
                val testLine = if (lineBuilder.isEmpty()) word else "$lineBuilder $word"
                val width = paintText.measureText(testLine)
                if (width > 500f) {
                    canvas.drawText(lineBuilder, 40f, yPos, paintText)
                    yPos += 15f
                    lineBuilder = word
                    if (yPos > 780f) {
                        pdfDocument.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                        page = pdfDocument.startPage(nextInfo)
                        canvas = page.canvas
                        yPos = 50f
                    }
                } else {
                    lineBuilder = testLine
                }
            }
            if (lineBuilder.isNotEmpty()) {
                canvas.drawText(lineBuilder, 40f, yPos, paintText)
                yPos += 18f
            }
        }

        // Add Bullet Points if any
        try {
            val bullets = JSONArray(note.bulletPointsJson)
            if (bullets.length() > 0) {
                yPos += 10f
                if (yPos > 750f) {
                    pdfDocument.finishPage(page)
                    val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(nextInfo)
                    canvas = page.canvas
                    yPos = 50f
                }
                canvas.drawText("Key Quick Revision Points 📌", 30f, yPos, paintSubHeader)
                yPos += 20f
                for (i in 0 until bullets.length()) {
                    val bulletText = "• " + bullets.getString(i)
                    if (yPos > 780f) {
                        pdfDocument.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                        page = pdfDocument.startPage(nextInfo)
                        canvas = page.canvas
                        yPos = 50f
                    }
                    canvas.drawText(bulletText, 45f, yPos, paintText)
                    yPos += 15f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Visual Flow Chart Printing!
        try {
            val flowNodes = JSONArray(note.flowchartNodesJson)
            if (flowNodes.length() > 0) {
                yPos += 20f
                if (yPos > 650f) { // Need extra space for chart drawing
                    pdfDocument.finishPage(page)
                    val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(nextInfo)
                    canvas = page.canvas
                    yPos = 50f
                }
                canvas.drawText("Flow Chart / Concept Map 🗺️", 30f, yPos, paintSubHeader)
                yPos += 25f

                val paintBox = Paint().apply {
                    color = Color.rgb(224, 242, 241) // Light teal
                    style = Paint.Style.FILL
                }
                val paintBoxBorder = Paint().apply {
                    color = Color.rgb(0, 150, 136) // Dark teal
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val paintNodeText = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                // Draw linear boxes representing connections
                for (i in 0 until flowNodes.length()) {
                    val nodeObj = flowNodes.getJSONObject(i)
                    val text = nodeObj.optString("text", "Block")
                    
                    // Draw node box
                    val rectLeft = 100f
                    val rectTop = yPos
                    val rectRight = 495f
                    val rectBottom = yPos + 30f
                    
                    canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paintBox)
                    canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paintBoxBorder)
                    
                    // Node text
                    canvas.drawText("${i + 1}. $text", rectLeft + 15f, rectTop + 18f, paintNodeText)
                    
                    // Draw connecting arrow if not last
                    if (i < flowNodes.length() - 1) {
                        yPos += 30f
                        canvas.drawLine(297f, yPos, 297f, yPos + 15f, paintBoxBorder)
                        // Arrow head
                        canvas.drawLine(292f, yPos + 10f, 297f, yPos + 15f, paintBoxBorder)
                        canvas.drawLine(302f, yPos + 10f, 297f, yPos + 15f, paintBoxBorder)
                        yPos += 15f
                    } else {
                        yPos += 35f
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Visual Chart Printing!
        try {
            val chartPoints = JSONArray(note.chartDataJson)
            if (chartPoints.length() > 0 && note.chartType != "none") {
                yPos += 20f
                if (yPos > 600f) { // Need space for chart rendering
                    pdfDocument.finishPage(page)
                    val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(nextInfo)
                    canvas = page.canvas
                    yPos = 50f
                }
                canvas.drawText("Visual Data Chart (${note.chartType.uppercase()}) 📊", 30f, yPos, paintSubHeader)
                yPos += 25f

                // Draw simple visual representation of chart
                val chartPaint = Paint().apply {
                    color = Color.rgb(233, 30, 99) // Hot pink
                    style = Paint.Style.FILL
                }
                val labelPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 9f
                }

                if (note.chartType == "bar") {
                    val axisY = yPos + 100f
                    // Draw axis
                    canvas.drawLine(60f, yPos, 60f, axisY, paintBorder)
                    canvas.drawLine(60f, axisY, 400f, axisY, paintBorder)
                    
                    var startX = 80f
                    for (i in 0 until chartPoints.length()) {
                        val pt = chartPoints.getJSONObject(i)
                        val label = pt.optString("label", "pt")
                        val value = pt.optDouble("value", 0.0).toFloat()
                        
                        // Limit height to 80px
                        val height = (value.coerceIn(0f, 100f) / 100f) * 80f
                        canvas.drawRect(startX, axisY - height, startX + 25f, axisY, chartPaint)
                        canvas.drawText("$label ($value%)", startX - 5f, axisY + 12f, labelPaint)
                        startX += 45f
                    }
                    yPos += 120f
                } else {
                    // List chart values in a cute table
                    for (i in 0 until chartPoints.length()) {
                        val pt = chartPoints.getJSONObject(i)
                        val label = pt.optString("label", "pt")
                        val value = pt.optDouble("value", 0.0)
                        canvas.drawText("• $label: $value%", 50f, yPos, paintText)
                        yPos += 15f
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Draw doodle image if present
        if (note.imagePath != null) {
            try {
                val imgFile = File(note.imagePath)
                if (imgFile.exists()) {
                    yPos += 20f
                    if (yPos > 550f) { // Needs plenty of space for image painting
                        pdfDocument.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                        page = pdfDocument.startPage(nextInfo)
                        canvas = page.canvas
                        yPos = 50f
                    }
                    canvas.drawText("Attached Picture / Doodle Illustration 🎨", 30f, yPos, paintSubHeader)
                    yPos += 20f
                    
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // downsample to avoid OOM
                    }
                    val bitmap = BitmapFactory.decodeFile(note.imagePath, options)
                    if (bitmap != null) {
                        // Fit to standard scale
                        val targetWidth = 200f
                        val scale = targetWidth / bitmap.width
                        val targetHeight = bitmap.height * scale
                        
                        val destRect = android.graphics.RectF(50f, yPos, 50f + targetWidth, yPos + targetHeight)
                        canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        yPos += targetHeight + 15f
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        pdfDocument.finishPage(page)

        // Save PDF to downloads or cache
        try {
            val fileName = "StudySlay_Note_${note.id}_${System.currentTimeMillis()}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var pdfFile = File(downloadsDir, fileName)
            
            // Fallback to cache if we don't have direct external storage access (Android 10+ scoped storage handles standard Downloads, but in case)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                pdfFile = File(context.cacheDir, fileName)
            }
            
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            
            onComplete(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }

    fun exportMockTestToPdf(context: Context, test: MockTest, onComplete: (File) -> Unit) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = Color.rgb(244, 67, 54) // Bright Red-Orange Theme
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubHeader = Paint().apply {
            color = Color.rgb(76, 175, 80) // Cool Green
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("StudySlay Mock Test & Prep 📝🔥", 30f, 50f, paintHeader)
        canvas.drawLine(30f, 60f, 565f, 60f, paintHeader.apply { strokeWidth = 2f })

        // Test details
        paintText.textSize = 13f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Test: ${test.title}", 30f, 85f, paintText)
        
        paintText.textSize = 11f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val scoreText = if (test.isCompleted) "Score: ${test.achievedScore}/${test.maxMarks} • ${test.evaluationFeedback}" else "Duration: ${test.durationMinutes} mins • Max Marks: ${test.maxMarks}"
        canvas.drawText(scoreText, 30f, 105f, paintText)

        canvas.drawLine(30f, 115f, 565f, 115f, Paint().apply { color = Color.LTGRAY })

        var yPos = 140f
        paintText.textSize = 11f
        paintText.typeface = Typeface.DEFAULT

        try {
            val questions = JSONArray(test.questionsJson)
            for (i in 0 until questions.length()) {
                if (yPos > 750f) {
                    pdfDocument.finishPage(page)
                    val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(nextInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                val qObj = questions.getJSONObject(i)
                val type = qObj.optString("questionType")
                val qText = qObj.optString("questionText")
                val marks = qObj.optInt("marks")

                // Question Title
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Q${i + 1}. $qText", 30f, yPos, paintText)
                canvas.drawText("($marks Marks)", 480f, yPos, paintText)
                yPos += 18f

                paintText.typeface = Typeface.DEFAULT
                // Render details based on type
                when (type) {
                    "mcq" -> {
                        val options = JSONArray(qObj.optString("optionsJson", "[]"))
                        for (j in 0 until options.length()) {
                            if (yPos > 780f) {
                                pdfDocument.finishPage(page)
                                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                                page = pdfDocument.startPage(nextInfo)
                                canvas = page.canvas
                                yPos = 50f
                            }
                            val optLetter = ('A'.code + j).toChar()
                            canvas.drawText("   [  ] $optLetter. ${options.getString(j)}", 40f, yPos, paintText)
                            yPos += 15f
                        }
                    }
                    "true_false" -> {
                        canvas.drawText("   [  ] TRUE      [  ] FALSE", 40f, yPos, paintText)
                        yPos += 15f
                    }
                    "match_following" -> {
                        val left = JSONArray(qObj.optString("matchLeftJson", "[]"))
                        val right = JSONArray(qObj.optString("matchRightJson", "[]"))
                        
                        // Show randomized list
                        val maxLen = maxOf(left.length(), right.length())
                        for (j in 0 until maxLen) {
                            if (yPos > 780f) {
                                pdfDocument.finishPage(page)
                                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                                page = pdfDocument.startPage(nextInfo)
                                canvas = page.canvas
                                yPos = 50f
                            }
                            val lItem = if (j < left.length()) "${j + 1}. ${left.getString(j)}" else ""
                            val rItem = if (j < right.length()) "${('a'.code + j).toChar()}. ${right.getString(j)}" else ""
                            canvas.drawText("      $lItem   ------   $rItem", 45f, yPos, paintText)
                            yPos += 15f
                        }
                    }
                    "fill_blanks" -> {
                        canvas.drawText("   Answer: _____________________________________", 40f, yPos, paintText)
                        yPos += 15f
                    }
                    "subjective" -> {
                        // Drawing blank writing lines
                        canvas.drawText("   Guidelines: ${qObj.optString("subjectiveRubric")}", 40f, yPos, paintText.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) })
                        yPos += 15f
                        paintText.typeface = Typeface.DEFAULT
                        for (k in 0..3) {
                            if (yPos > 780f) {
                                pdfDocument.finishPage(page)
                                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                                page = pdfDocument.startPage(nextInfo)
                                canvas = page.canvas
                                yPos = 50f
                            }
                            canvas.drawLine(50f, yPos + 10f, 545f, yPos + 10f, Paint().apply { color = Color.LTGRAY })
                            yPos += 20f
                        }
                    }
                }
                
                // Print correct answers at the bottom of the page if test is completed
                if (test.isCompleted) {
                    val correctAns = qObj.optString("correctAnswer")
                    canvas.drawText("   ✨ Correct Answer: $correctAns", 40f, yPos, paintText.apply { 
                        color = Color.rgb(76, 175, 80)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    })
                    paintText.color = Color.BLACK
                    paintText.typeface = Typeface.DEFAULT
                    yPos += 18f
                }
                
                yPos += 15f // Space before next question
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pdfDocument.finishPage(page)

        try {
            val fileName = "StudySlay_MockTest_${test.id}_${System.currentTimeMillis()}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var pdfFile = File(downloadsDir, fileName)
            
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                pdfFile = File(context.cacheDir, fileName)
            }
            
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            
            onComplete(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }

    /**
     * Triggers a generic system send/share Intent to open or share the PDF.
     */
    fun sharePdfFile(context: Context, file: File) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Download / Open Study Pack via:"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
