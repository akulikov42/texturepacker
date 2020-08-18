package texturepacker

import argsparser.*
import java.io.File

class PackerConfiguration {

    private companion object {

        private const val MAX_RESOLUTION            = 16384
        private const val MIN_RESOLUTION            = 64
        private const val DEFAULT_RESOLUTION        = 2048
        private const val MAX_MARGIN                = 100
        private const val DEFAULT_MARGIN            = 5
        private const val DEFAULT_ATLAS_NAME        = "atlas.png"
        private const val DEFAULT_DESCRIPTION_NAME  = "description.xml"

        private const val PROGRAM_INFO     = "\nTexture Packer v0.01"
        private const val HELP_PREAMBLE    = "Pack separate images to texture atlas and create XML description.\n" +
                                             "Supported image format: .png"
        private const val HELP_USAGE       = "Usage: texturepacker [OPTIONS] [SRC_IMAGES]"
        private const val HELP_CONCLUSION  = "Enjoy!"
        private const val DESCRIPTION_INDENT = 30

        private const val MAX_SEARCH_DEPTH     = 7
        private const val DEFAULT_SEARCH_DEPTH = 1

    }

    /**
     * Парсер аргументов командной строки
     */
    private val clParser = ArgsParser(
            programInfo = PROGRAM_INFO,
            helpUsage = HELP_USAGE,
            helpPreamble = HELP_PREAMBLE,
            helpConclusion = HELP_CONCLUSION,
            descriptionIndent = DESCRIPTION_INDENT
    ) { arr -> arr.isEmpty() }

    var atlasWidth      = DEFAULT_RESOLUTION
    var atlasHeight     = DEFAULT_RESOLUTION
    var atlasMargin     = DEFAULT_MARGIN
    var atlasName       = DEFAULT_ATLAS_NAME
    var descriptionName = DEFAULT_DESCRIPTION_NAME
    var srcImgList      = mutableListOf<File>()

    private var helpRequested   = false
    private var searchDepth = DEFAULT_SEARCH_DEPTH


    /**
     * Опция с шириной атласа
     */
    private val widthOption = FixParamsOption(
            "-w",
            "--width",
            "Set atlas width",
            0,
            false,
            "Usage: --height [$MIN_RESOLUTION..$MAX_RESOLUTION]"
    ) { str ->
        atlasWidth = if (str.toInt() in MIN_RESOLUTION..MAX_RESOLUTION) str.toInt() else DEFAULT_RESOLUTION
        true
    }


    /**
     * Опция с высотой атласа
     */
    private val heightOption = FixParamsOption(
            "-h",
            "--height",
            "Set atlas height",
            0,
            false,
            "Usage: --height [$MIN_RESOLUTION..$MAX_RESOLUTION]"
    ) { str ->
        atlasHeight = if(str.toInt() in MIN_RESOLUTION..MAX_RESOLUTION) str.toInt() else DEFAULT_RESOLUTION
        true
    }


    /**
     * Опция с отступами
     */
    private val marginOption = FixParamsOption(
            "-m",
            "--margin",
            "Set margin between src images",
            0,
            false,
            "Usage: --margin [0..$MAX_MARGIN]"
    ) { str ->
        atlasMargin = if(str.toInt() in 0..MAX_MARGIN) str.toInt() else DEFAULT_MARGIN
        true
    }


    /**
     * Опция с входной директорией (содержащей входные файлы)
     */
    private val inputDirOption = FixParamsOption(
            "-i",
            "--inputDir",
            "Directory which contain sources (.png)",
            1,
            true,
            "Usage: --inputDir <PATH_TO_DIR>\n  <PATH_TO_DIR> - path to directory\n",
            this::collectImagesFromPath
    )


    /**
     * Опция с глубиной поиска
     */
    private val searchDepthOption = FixParamsOption(
            "-d",
            "--depth",
            "Depth for search source files",
            0,
            false,
            "Usage: --depth [$DEFAULT_SEARCH_DEPTH..$MAX_SEARCH_DEPTH]"
    ) { str ->
        searchDepth = if(str.toInt() !in DEFAULT_SEARCH_DEPTH..MAX_SEARCH_DEPTH) DEFAULT_SEARCH_DEPTH else str.toInt()
        true
    }


    /**
     * Опция с именем атласа
     */
    private val atlasNameOption = FixParamsOption(
            "-a",
            "--atlasName",
            "Filename for output atlas",
            2,
            false,
            "Usage: --atlasName <ATLAS_NAME.png>\nWith .png or without - no matter.\n"
    ) { str -> atlasName = str; true }


    /**
     * Опция с именем описания атласа
     */
    private val descriptionNameOption = FixParamsOption(
            "-d",
            "--descriptionName",
            "Filename for output description",
            2,
            false,
            "Usage: --descriptionName <DESCRIPTION_NAME.xml>\nWith .xml or without - no matter.\n"
    ) { str-> descriptionName = str; true }


    /**
     * Опция помощи
     */
    private val helpOption = Key(
            "",
            "--help",
            "Show help message",
            Integer.MIN_VALUE,
            false
    ) { helpRequested = true; false; }


    /**
     * Установка ожидаемых опций вызова
     */
    init {
        clParser.addOption(widthOption)
        clParser.addOption(heightOption)
        clParser.addOption(marginOption)
        clParser.addOption(inputDirOption)
        clParser.addOption(atlasNameOption)
        clParser.addOption(descriptionNameOption)
        clParser.addOption(searchDepthOption)
        clParser.addOption(helpOption)
        helpOption.help = "      ${"--help".padEnd(DESCRIPTION_INDENT)} - Show help message"

    }


    /**
     *
     * Проверка директории и взятие из нее изображений (опция [inputDirOption])
     *
     * @param pathToDir Путь до директории с исходниками
     *
     * @return true - изображения есть, false - изображений нет.
     *
     */
    private fun collectImagesFromPath(pathToDir: String): Boolean {

        // Проверка пути
        val inputDir  = File(pathToDir)
        if(!inputDir.exists() || !inputDir.isDirectory)
            return inputDirOption.errMsg("\'$pathToDir\' doesn't exist or not directory!")

        // Сбор файлов .png
        collectImages(inputDir, srcImgList, searchDepth)
        if(srcImgList.isNotEmpty()) return true

        // Если изображений нет, то выводим соответствующее сообщение
        return inputDirOption.errMsg("$pathToDir doesn't contain .png files! Search depth: $searchDepth")

    }


    /**
     *
     * Выбор из каталога всех файлов "поддерживаемых форматов" (.png)
     *
     * @param dir Директория для поиска
     * @param depth Максимальная глубина поиска. При depth = 1 переход во вложенные каталоги не производится
     * @param outImgList Список с собранным файлами изображений
     *
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun collectImages(dir: File?, outImgList: MutableList<File>, depth: Int = 1) {

        // Проверяем условия выхода из рекурсии
        if(depth == 0 || dir == null || !dir.isDirectory)
            return

        // Собираем изображения из текущей директории и собираем список вложенных директорий
        val dirsInPath = mutableListOf<File>()
        dir.listFiles().forEach {  currFile ->
            if(currFile.isFile && supportedImage(currFile))
                outImgList.add(currFile)
            if(currFile.isDirectory)
                dirsInPath.add(currFile)
        }

        // Для каждой директории вызываем функцию сбора изображений
        dirsInPath.forEach { currDir ->
            collectImages(currDir, outImgList, depth - 1)
        }

    }

    /**
     * Проверка формата
     *
     * p.s. Потом надо его заменить на проверку заголовка файла, а не только имени
     *
     */
    private fun supportedImage(file: File): Boolean {
       val format = file.name.substringAfterLast('.')
       if(format == "png")
           return true
       return false

    }

    /**
     *
     * Подсказка по использованию пакера
     *
     */
    fun helpMsg() = clParser.buildHelp()


    /**
     *
     * Установка конфигурации из аргументов командной строки.
     *
     * @param args Аргументы командной строки.
     *
     * @return true - если успешно.
     *
     */
    fun setConfFromCL(args: Array<String>): Boolean {
        return when(clParser.parseArgs(args)) {
            ParseResult.MISSING_REQUIRED_OPTIONS -> {
                if(helpRequested) {
                    println(helpMsg())
                    return false
                }
                inputDirOption.errMsg("Input directory should be defined!")
                false
            }
            ParseResult.OK -> {
                true
            }
            else -> {
                println(helpMsg())
                false
            }
        }
    }
}