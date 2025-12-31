
import com.mrnrod45.nstk.domain.file.FileSplitter
import com.mrnrod45.nstk.domain.file.DesktopFileSplitter

actual fun getFileSplitter(): FileSplitter = DesktopFileSplitter()

class DesktopPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual class Greeting {
    actual fun greet(): String {
        return "Hello, ${DesktopPlatform().name}!"
    }
}

actual fun getPlatformType(): PlatformType = PlatformType.DESKTOP
