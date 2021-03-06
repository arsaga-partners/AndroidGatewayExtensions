package jp.co.arsaga.extensions.gateway

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper


fun Activity.startLaunchActivity(transitionAnimation: ((Activity) -> Unit)? = null) {
    startLaunchActivity( {
        isCurrentLaunchActivity(it) == false
    }, transitionAnimation)
}

fun Activity.startLaunchActivity(
    isPossibleTransition: (launchIntent: Intent?) -> Boolean,
    transitionAnimation: ((Activity) -> Unit)? = null
) {
    packageManager.getLaunchIntentForPackage(packageName)
        ?.takeIf { isPossibleTransition(it) }
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ?.let {
            Handler(Looper.getMainLooper()).post {
                startActivity(it)
                transitionAnimation?.invoke(this)
            }
        }
}

fun Activity.isCurrentLaunchActivity(
    launchIntent: Intent?
): Boolean? = launchIntent?.component?.className
    ?.let { javaClass.name == it }
