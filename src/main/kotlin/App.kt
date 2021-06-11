import javafx.application.Application
import javafx.application.Platform.runLater
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.canvas.Canvas
import javafx.scene.control.ChoiceBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

private class Backend(private var platform: Platform, private var canvas: Canvas) {
    private var backend: Thread? = null

    private fun start(name: String) {
        val rom = javaClass.getResource("roms/${name}.ch8")?.readBytes() ?: throw Exception("No game resource $name")

        backend = thread {
            val chip8 = Chip8(platform)
            chip8.load(rom)
            while (!Thread.interrupted()) {
                display(chip8)
                chip8.step()
            }
        }
    }

    fun restart(name: String) {
        stop()
        start(name)
    }

    fun stop() {
        backend?.interrupt()
        backend?.join()
    }

    private fun display(chip8: Chip8) {
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

class App : Application(), Platform {
    companion object {
        val ROMS: List<String> = listOf("invaders", "maze", "tetris", "airplane", "tank", "test")
    }

    private val canvas = Canvas(640.0, 320.0)
    private val backend = Backend(this, canvas)
    private val keys: Array<Boolean> = Array(16) { false }
    private val lock = ReentrantLock()
    private val cond = lock.newCondition()

    override fun start(primaryStage: Stage) {
        val choice = ChoiceBox<String>()
        choice.items.addAll(ROMS)
        choice.value = ROMS[0]
        choice.setOnAction { play(choice.value) }
        val vbox = VBox(choice, canvas)
        val scene = Scene(vbox)
        scene.setOnKeyPressed { setKey(it.text, true) }
        scene.setOnKeyReleased { setKey(it.text, false) }
        primaryStage.title = "Chip-8"
        primaryStage.scene = scene
        primaryStage.show()
        play(choice.value)
    }

    override fun stop() {
        backend.stop()
    }

    private fun play(name: String) {
        println("Loading game $name")
        backend.restart(name)
    }

    private fun setKey(text: String, value: Boolean) {
        val index = when (text) {
            "1" -> 0x1
            "2" -> 0x2
            "3" -> 0x3
            "4" -> 0xc
            "q" -> 0x4
            "w" -> 0x5
            "e" -> 0x6
            "r" -> 0xd
            "a" -> 0x7
            "s" -> 0x8
            "d" -> 0x9
            "f" -> 0xe
            "z" -> 0xa
            "x" -> 0x0
            "c" -> 0xb
            "v" -> 0xf
            else -> {
                return
            }
        }
        lock.withLock {
            keys[index] = value
            cond.signal()
        }
    }

    override fun yield() = TimeUnit.MICROSECONDS.sleep(1)

    override fun time(): Long = System.currentTimeMillis()

    override fun waitKey(): UByte {
        while (true) {
            lock.withLock {
                val i = keys.indexOf(true)
                if (i != -1) {
                    return i.toUByte()
                }
                cond.await()
            }
        }
    }

    override fun peekKey(key: UByte): Boolean = keys[key.toInt()]
}
