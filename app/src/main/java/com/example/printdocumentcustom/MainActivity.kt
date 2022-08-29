package com.example.printdocumentcustom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import java.io.FileOutputStream
import java.io.IOException
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.content.Context
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.graphics.pdf.PdfDocument
import java.lang.Exception
import android.graphics.Color
import android.graphics.Paint
import android.print.PrintManager
import android.view.View
import android.widget.Button
import android.widget.EditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        val button: Button = findViewById(R.id.print_btn)
        button.setOnClickListener {
            doPrint()
        }

    }


    private fun doPrint() {
        val printManager = this
            .getSystemService(PRINT_SERVICE) as PrintManager

        val jobName = this.getString(R.string.app_name) + "Document"
        printManager.print(jobName, MyPrintDocumentAdapter(this), null)
    }


    inner class MyPrintDocumentAdapter(private var context: Context) : PrintDocumentAdapter() {
        private var pageHeight: Int = 0
        private var pageWidth: Int = 0
        private var myPdfDocument: PdfDocument? = null
        private var totalpages = 1
        val editText: EditText = findViewById<EditText?>(R.id.editText)
        val content :String = editText.text.toString()



        private fun drawPage(page: PdfDocument.Page, pageNumber: Int) {
            var pagenum = pageNumber
            val canvas = page.canvas
            pagenum++

            val titleBaseLine = 100
            val leftMargin = 54
            val paint = Paint()
            paint.color = Color.RED
            paint.textSize = 40f
            paint.textSize = 14f
            canvas.drawText(content
                ,
                leftMargin.toFloat(), (titleBaseLine + 35).toFloat(), paint
            )
            if (pagenum % 2 == 0)
                paint.color = Color.RED
            else
                paint.color = Color.GREEN




        }

        private fun pageInRange(pageRanges: Array<PageRange>, page: Int): Boolean {
            for (i in pageRanges.indices) {
                if (page >= pageRanges[i].start && page <= pageRanges[i].end)
                    return true
            }
            return false
        }


        override fun onLayout(
            oldAttributes: PrintAttributes,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: PrintDocumentAdapter.LayoutResultCallback,
            metadata: Bundle


        ) {
            myPdfDocument = PrintedPdfDocument(context, newAttributes)
            val height = newAttributes.mediaSize?.heightMils
            val width = newAttributes.mediaSize?.heightMils
            height?.let {
                pageHeight = it / 1000 * 72
            }

            width?.let {
                pageWidth = it / 1000 * 72
            }
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }
            if (totalpages > 0) {
                val builder =
                    PrintDocumentInfo.Builder("print_output.pdf").setContentType(
                        PrintDocumentInfo.CONTENT_TYPE_DOCUMENT
                    )
                        .setPageCount(totalpages)
                val info = builder.build()
                callback.onLayoutFinished(info, true)
            } else {
                callback.onLayoutFailed("Page count is zero")

            }


        }

        override fun onWrite(
            pageRanges: Array<PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: PrintDocumentAdapter.WriteResultCallback
        ) {
            for (i in 0 until totalpages) {
                if (pageInRange(pageRanges, i)) {
                    val newPage = PageInfo.Builder(pageWidth, pageHeight, i).create()
                    val page = myPdfDocument?.startPage(newPage)

                    if (cancellationSignal.isCanceled) {
                        callback.onWriteCancelled()
                        myPdfDocument?.close()
                        myPdfDocument = null
                        return
                    }
                    page?.let {
                        drawPage(it, i)
                    }
                    myPdfDocument?.finishPage(page)


                }


            }
            try {
                myPdfDocument?.writeTo(FileOutputStream(destination.fileDescriptor))
            } catch (e: Exception) {
                callback.onWriteFailed(e.toString())
                return
            } finally {
                myPdfDocument?.close()
                myPdfDocument = null
            }
            callback.onWriteFinished(pageRanges)

        }
    }
}
