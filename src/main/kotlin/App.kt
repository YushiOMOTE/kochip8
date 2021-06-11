import javafx.application.Application
import javafx.application.Platform.runLater
import javafx.scene.Group
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlin.concurrent.thread

class AppPlatform : Platform {
    override fun time(): Long = System.currentTimeMillis()

    override fun waitKey(): UByte = 0u

    override fun peekKey(key: UByte): Boolean = false
}

class App : Application() {
    private val canvas = Canvas(640.0, 320.0)
    private val chip8 = Chip8(AppPlatform())
    private var backend: Thread? = null

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Hello World"
        primaryStage.scene = Scene(Group(canvas))
        primaryStage.show()

        val rom = javaClass.getResource("test.ch8").readBytes()
        chip8.load(rom)

        backend = thread {
            while (!Thread.interrupted()) {
                display()
                chip8.step()
            }
        }
    }

    override fun stop() {
        backend?.interrupt()
    }

    private fun display() {
        val updates = chip8.display.updates()
        if (updates.isEmpty()) {
            return
        }
        runLater {
            val ctx = canvas.graphicsContext2D
            updates.forEach {
                ctx.fill = if (it.value) Color.WHITE else Color.BLACK
                ctx.fillRect(it.x * 10.0, it.y * 10.0, 10.0, 10.0)
            }
        }
    }
}
