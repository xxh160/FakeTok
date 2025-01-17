package com.example.faketok

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.example.faketok.util.Constant
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates


enum class ArgName {
    ARG_NICKNAME, ARG_DESCRIPTION, ARG_LIKECOUNT, ARG_URI, ARG_IMAGE, ARG_AVATAR
}

// frame layout 中放置视频播放空间和图片控件，frame 本身充当监听器（未完成）
class VideoFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    // 控件
    private lateinit var avatarView: ImageView
    private lateinit var imageView: ImageView
    private lateinit var nicknameView: TextView
    private lateinit var desView: TextView
    private lateinit var likeCountView: TextView
    private lateinit var progress: ProgressBar
    private lateinit var videoView: StandardGSYVideoPlayer

    // bundle 传参
    private lateinit var nickname: String
    private lateinit var description: String
    private var likeCount by Delegates.notNull<Long>()
    private lateinit var uri: Uri
    private lateinit var image: Uri
    private lateinit var avatar: Uri

    // video 状态, 用户指令
    private var isActivated: Boolean = false

    // 手势控制器
    private var gesture: GestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return super.onDoubleTap(e)
            }
        })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            nickname = it.getString(ArgName.ARG_NICKNAME.toString())!!
            description = it.getString(ArgName.ARG_DESCRIPTION.toString())!!
            likeCount = it.getString(ArgName.ARG_LIKECOUNT.toString())!!.toLong()
            uri = Uri.parse(it.getString(ArgName.ARG_URI.toString())!!)
            image = Uri.parse(it.getString(ArgName.ARG_IMAGE.toString())!!)
            avatar = Uri.parse(it.getString(ArgName.ARG_AVATAR.toString())!!)
        }

        Log.d(Constant.APP, "VideoFragment, onCreate finishes")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View? = inflater.inflate(R.layout.fragment_video, container, false)
        this.initImage(view)
        this.initTexts(view)
        this.setVideo(view)
        this.initProgress(view)
        return view
    }

    private fun initProgress(view: View?) {
        progress = view?.findViewById(R.id.progress)!!
        // 默认状态不可见
        progress.visibility = View.GONE
        Log.d(Constant.APP, "VideoFragment, initProgress, ${progress.visibility}")
    }

    private fun setVideo(view: View?) {
        videoView = view?.findViewById(R.id.player)!!

        val gsyVideoOption = GSYVideoOptionBuilder()
        gsyVideoOption.setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onPrepared(url: String?, vararg objects: Any?) {
                super.onPrepared(url, *objects)
                if (!isActivated) videoView.onVideoPause()
                // 准备好了 封面不可见 progress 不可见 视频可见
                imageView.visibility = View.GONE
                progress.visibility = View.GONE
                videoView.visibility = View.VISIBLE
                Log.d(
                    Constant.APP,
                    "VideoFragment, video ready, $isActivated, ${progress.visibility == View.GONE}"
                )
            }

            override fun onClickBlank(url: String?, vararg objects: Any?) {
                Log.d(Constant.APP, "VideoFragment, onClickBlank, $isActivated")
                if (!isActivated) videoView.onVideoResume(false)
                else videoView.onVideoPause()
                isActivated = !isActivated
            }
        }).build(videoView)

        videoView.apply {
            contentDescription = "$nickname, $description, $likeCount, $uri, $image"
            // 初始设为不可见 在点击 image 之后可见
            visibility = View.GONE
            // 设置 uri
            setUp(
                uri.toString(),
                true,
                "cur"
            )
            // 标题不可见
            titleTextView?.visibility = View.GONE
            // 返回按钮不可见
            backButton?.visibility = View.GONE
            // 循环
            isLooping = true

            Log.d(Constant.APP, "VideoFragment, $uri")
        }
    }

    // avatar 封面
    private fun initImage(view: View?) {
        // 封面
        imageView = view?.findViewById(R.id.image)!!
        context?.let {
            Glide.with(it).load(image).transition(withCrossFade()).into(imageView)
        }
        imageView.visibility = View.VISIBLE
        // 一次性监听器
        imageView.setOnClickListener {
            // 开始加载 圈圈可见
            progress.visibility = View.VISIBLE
            isActivated = true
            videoView.startPlayLogic()
            Log.d(
                Constant.APP,
                "VideoFragment, ImageView click, $isActivated, ${progress.visibility}"
            )
        }
        // avatar
        avatarView = view.findViewById(R.id.avatar)
        context?.let {
            Glide.with(it).load(avatar).transition(withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(avatarView)
        }
    }

    private fun initTexts(view: View?) {
        nicknameView = view?.findViewById(R.id.nickname)!!
        nicknameView.text = "@$nickname"
        desView = view.findViewById(R.id.description)!!
        desView.text = description
        likeCountView = view.findViewById(R.id.like_count)!!
        val cur = likeCount / 1000
        likeCountView.text = "${cur}k"
    }

    override fun onPause() {
        super.onPause()
        isActivated = false
        videoView.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        isActivated = true
        videoView.onVideoResume(false)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            info: VideoInfo
        ) =
            VideoFragment().apply {
                arguments = Bundle().apply {
                    arrayOf(
                        Pair(ArgName.ARG_DESCRIPTION, info.description),
                        Pair(ArgName.ARG_LIKECOUNT, info.likecount),
                        Pair(ArgName.ARG_NICKNAME, info.nickname),
                        Pair(ArgName.ARG_URI, info.feedurl),
                        Pair(ArgName.ARG_IMAGE, info.thumbnails),
                        Pair(ArgName.ARG_AVATAR, info.avatar)
                    ).forEach {
                        putString(it.first.toString(), it.second.toString())
                    }
                }
            }
    }
}


