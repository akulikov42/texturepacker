package texturepacker

fun main(args: Array<String>) {

    val packerConf = PackerConfiguration()
    if(!packerConf.setConfFromCL(args))
        return

    val packer = TexturePacker(packerConf)

    packer.pack()

    return

}