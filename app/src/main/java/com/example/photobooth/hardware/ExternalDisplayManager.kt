package com.example.photobooth.hardware

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.widget.ImageView

class ExternalDisplayPresentation(context: Context, display: Display, private val slideImageRes: Int) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageView = ImageView(context).apply {
            setImageResource(slideImageRes)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(imageView)
    }
}
