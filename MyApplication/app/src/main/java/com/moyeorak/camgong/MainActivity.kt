package com.moyeorak.camgong

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_calendar.*
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

//    private val tabTextList = arrayListOf("Calendar", "HOME", "STATS")
    private val tabIconList = arrayListOf(R.drawable.pie_chart, R.drawable.house, R.drawable.calendar2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()

//        tabs.setScrollPosition(1,0f,true)
        view_pager.setCurrentItem(1)
    }

    private fun init() {
        view_pager.adapter = PageAdapter(this)
        TabLayoutMediator(tabs, view_pager) {
                tab, position ->
            tab.setIcon(tabIconList[position])
//            tab.text = tabTextList[position]
        }.attach()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> CustomDialog(this)
                .setTitle("종료하시겠습니까")
                .setMessage("지켜보고있다")
                .setPositiveButton("OK") { finish()
                }.setNegativeButton("CANCEL") {null
                }.show()
        }
        return true
    }
}