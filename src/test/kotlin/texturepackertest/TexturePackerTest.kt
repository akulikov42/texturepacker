package texturepackertest

import org.junit.Test
import texturepacker.*

class TexturePackerTest {
    val packerConf = PackerConfiguration()
    @Test
    fun test() {
        val args = arrayOf("--help")
        packerConf.setConfFromCL(args)
        assert(true)
    }
}