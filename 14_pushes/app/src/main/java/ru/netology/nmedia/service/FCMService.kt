package ru.netology.nmedia.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import kotlin.random.Random


class FCMService : FirebaseMessagingService() {
    private val action = "action"
    private val content = "content"
    private val channelId = "remote"
    private val gson = Gson()

    private lateinit var  repository: PostRepository



    override fun onCreate() {
        super.onCreate()
         repository  = PostRepositoryImpl(AppDb.getInstance(context = application).postDao())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.e("exc", "onMessageReceived")

        message.data[action]?.let { action ->
            gson.fromJson(message.data[content], DataFromServer::class.java)?.let { data ->

                try {
                    Log.e("exc", "on EventBus")
                    EventBus.getDefault().post(data)

                    if (data.notification) {
                        val dialogIntent = Intent(this, AppActivity::class.java)
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(dialogIntent)

                    }

                    when (Action.valueOf(action.toUpperCase())) {
                        Action.LIKE -> {
                            handleMessage(data, R.string.notification_user_liked)
                            repository.likeById(data.postId)
                        }
                        Action.SHARE -> {
                            handleMessage(data, R.string.notification_user_shared)
                            repository.shareById(data.postId)
                        }
                        Action.NEW -> {
                            handleMessage(data, R.string.notification_user_new)
                            repository.save(
                                Post(
                                    id = 0, author = data.userName,
                                    content = data.postAuthor, published = "by remote",
                                    likedByMe = false, likes = 0, shares = 444
                                )
                            )
                        }

                        Action.REMOVE -> {
                            handleMessage(data, R.string.notification_user_deleted)
                            repository.removeById(data.postId)
                            
                        }

                    }

                } catch (e: IllegalArgumentException) {
                    Log.e("exc", e.printStackTrace().toString())
                }

            }
        }
    }



    override fun onNewToken(token: String) {
        Log.e("exc", "token $token")
    }

    private fun handleMessage(content: DataFromServer, resource: Int) {
//        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
//            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
//        }

        val icon = BitmapFactory.decodeResource(resources, R.drawable.msg_icon_background)
        val notification = NotificationCompat.Builder(this, channelId)
          //  .setContentIntent(resultPendingIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setLargeIcon(icon)
            .setContentTitle(
                getString(
                    resource,
                    content.userName,
                    content.postAuthor,
                )
            ).setContentText(content.postAuthor)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content.postAuthor)
            )
            .setPriority(Notification.PRIORITY_MAX)
            .build()



        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)
    }
}

enum class Action {
    LIKE, SHARE, NEW, REMOVE
}

data class DataFromServer(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String,
    val notification: Boolean,
)

