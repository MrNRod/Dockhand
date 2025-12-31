
import com.mrnrod45.nstk.domain.file.FileSplitter
import com.mrnrod45.nstk.domain.file.AndroidFileSplitter

actual fun getFileSplitter(): FileSplitter = AndroidFileSplitter()

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual class Greeting {
    actual fun greet(): String {
        return "Hello, ${AndroidPlatform().name}!"
    }
}

actual fun getPlatformType(): PlatformType = PlatformType.ANDROID
