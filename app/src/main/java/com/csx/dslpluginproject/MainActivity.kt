package com.csx.dslpluginproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.airbnb.lottie.LottieAnimationView
import org.libpag.PAGFile
import org.libpag.PAGImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPagView()

        initLottieView()
    }

    private fun initLottieView() {
        val view1 = findViewById<LottieAnimationView>(R.id.lottie_view1)
        view1.setAnimation("camera_ocr_check_report_lottie.json")
        view1.repeatCount = -1
        view1.playAnimation()



     val view2 = findViewById<LottieAnimationView>(R.id.lottie_view2)
        view2.setAnimation("speechRecordAnimation.json")
        view2.repeatCount = -1
        view2.playAnimation()





    }

    private fun initPagView() {
        val view1 = findViewById<PAGImageView>(R.id.pag_view1)
        view1.composition = PAGFile.Load(assets,"camera_ocr_check_report_lottie.pag")
        view1.setRepeatCount(-1)
        view1.play()


        val view2 = findViewById<PAGImageView>(R.id.pag_view2)
        view2.composition = PAGFile.Load(assets,"speechRecordAnimation.pag")
        view2.setRepeatCount(-1)
        view2.play()

//        val view3 = findViewById<PAGImageView>(R.id.pag_view3)
//        view3.composition = PAGFile.Load(assets,"camera_ocr_check_report_lottie.pag")
//        view3.setRepeatCount(-1)
//        view3.play()
    }


}