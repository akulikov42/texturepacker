package texturepacker

fun main(args: Array<String>) {

    val packerConf = PackerConfiguration()

    if(!packerConf.setConfFromCL(args))
        print("")
    else
        println("ENJOY!!!")

    if(packerConf.helpRequested)
        println(packerConf.helpMsg())

}