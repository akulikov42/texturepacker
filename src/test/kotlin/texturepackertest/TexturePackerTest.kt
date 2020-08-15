package texturepackertest

import org.junit.Test
import texturepacker.*

class TexturePackerTest {
    val packerConf = PackerConfiguration()
    @Test
    fun test() {
        val args = arrayOf("--help")
        packerConf.setConfFromCL(args)
        if(packerConf.helpRequested)
            println(packerConf.helpMsg())
        assert(true)
    }
}