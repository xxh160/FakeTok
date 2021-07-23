package com.example.faketok

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

private val api = "https://beiyou.bytedance.com/api/invoke/video/invoke/video"

class FullScreenVideoActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var pager: ViewPager2
    private lateinit var videos: MutableList<VideoInfo>

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        videos = Collections.synchronizedList(ArrayList())

        val adapter = PagerAdapter(this, videos)

        pager = findViewById<ViewPager2?>(R.id.pager).apply {
            setAdapter(adapter)
        }

        // 上下滑动
        pager.orientation = ViewPager2.ORIENTATION_VERTICAL

        findViewById<Button>(R.id.gotoCaptureActivity).setOnClickListener {
            startActivity(Intent(this, CaptureActivity::class.java))
        }

        findViewById<Button>(R.id.gotoMyVideos).setOnClickListener {
            startActivity(Intent(this, MyVideosActivity::class.java))
        }

        launch {
            val client = HttpClient() {
                install(JsonFeature)
            }
            val response = client.request<HttpResponse> {
                url(api)
            }.receive<List<VideoInfo>>()

            client.close()
            videos.addAll(response)

            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }
}

private class PagerAdapter(ac: AppCompatActivity, val items: List<VideoInfo>) :
    FragmentStateAdapter(ac) {

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment =
        VideoFragment.newInstance(items[position])
}
