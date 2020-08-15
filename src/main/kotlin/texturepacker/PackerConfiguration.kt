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

        private const val MAX_DEPTH_SRC     = 7
        private const val DEFAULT_DEPTH_SRC = 1

    }

    /**
     * Парсер аргументов командной строки
     */
    private val clParser = ArgsParser(
            programInfo = PROGRAM_INFO,
            helpUsage = HELP_USAGE,
            helpPreamble = HELP_PREAMBLE,
            helpConclusion = HELP_CONCLUSION,
            descriptionIndent = DESCRIPTION_INDENT,
            applyParams = this::collectImagesFromParams
    )

    var atlasWidth      = DEFAULT_RESOLUTION
    var atlasHeight     = DEFAULT_RESOLUTION
    var atlasMargin     = DEFAULT_MARGIN
    var atlasName       = DEFAULT_ATLAS_NAME
    var descriptionName = DEFAULT_DESCRIPTION_NAME
    var srcDepth        = DEFAULT_DEPTH_SRC
    var forceSkip       = false
    var helpRequested   = false
    var srcImgList      = mutableListOf<File>()

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
            false,
            "Usage: --inputDir <PATH_TO_DIR> <DEPTH>\n  <PATH_TO_DIR> - path to directory\n  <DEPTH> - search depth.\n",
            2,
            this::collectImagesFromPath
    )


    /**
     * Опция с входными файлами
     */
    private val inputFilesOption = UnfixParamsOption(
            "-s",
            "--srcImages",
            "Images which will be packed",
            1,
            false,
            "Usage: --srcImages <PATH_TO_IMG_1> ... <PATH_TO_IMG_n>\n  <PATH_TO_IMG_i> - path to image #i\n  Images will be read before next option\n",
            this::collectImagesFromList
    )


    /**
     * Опция указывающая, надо ли игнорировать предупреждения
     */
    private val forceSkipOption = Key(
            "-f",
            "--forceSkip",
            "Force pack. Skip warnings. Not more.",
            0,
            false
    ) { forceSkip = true; true }


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
        clParser.addOption(inputFilesOption)
        clParser.addOption(atlasNameOption)
        clParser.addOption(descriptionNameOption)
        clParser.addOption(helpOption)
        clParser.addOption(forceSkipOption)
        helpOption.help = "      ${"--help".padEnd(DESCRIPTION_INDENT)} - Show help message"

    }


    /**
     *
     * Сбор изображений из списка файлов в параметрах программы (все, что не опция)
     *
     * @param params Параметры программы
     *
     * @return true - в них содержатся изображения, false - изображений нет.
     *
     */
    private fun collectImagesFromParams(params: Array<String>): Boolean {

        if(inputDirOption.applied || inputFilesOption.applied) {
            if(params.isNotEmpty()) {
                println("Error! Unexpected program params!\nSee texturepacker --help for more information")
                return false
            }
            return true
        }

        if(params.isEmpty()) {
            println("Error! No input files!")
            return false
        }

        val collectedImages = mutableListOf<File>()
        var currFile: File
        params.forEach { param ->
            currFile = File(param)
            if(currFile.isFile && supportedFormat(currFile.name.substringAfterLast('.')))
                collectedImages.add(currFile)
            else {
                println("Error! Invalid input files!")
                return false
            }
        }

        if(collectedImages.isNotEmpty()) {
            srcImgList.addAll(collectedImages)
            return true
        }

        println("Error! Input files doesn't contain images!")
        return false

    }

    /**
     *
     * Сбор изображений из списка файлов, полученных с опции [inputFilesOption]
     *
     * @param params Список файлов
     *
     * @return true - изображения есть или можно пропустить
     *
     */
    private fun collectImagesFromList(params: Array<String>): Boolean {

        if(params.isEmpty() && !forceSkip) return inputFilesOption.errMsg("Option without params!")

        val collectedImages = mutableListOf<File>()
        var currFile: File
        for(param in params) {
            currFile = File(param)
            if(currFile.isFile && supportedFormat(currFile.name.substringAfterLast('.')))
                collectedImages.add(currFile)
            else if(!forceSkip) return inputFilesOption.errMsg("${currFile.name} - its not image!")
        }

        if(collectedImages.isNotEmpty()) {
            srcImgList.addAll(collectedImages)
            return true
        }

        if(forceSkip) return true

        return inputFilesOption.errMsg("All params - is not images!")
    }

    /**
     *
     * Проверка директории и взятие из нее изображений (опция [inputDirOption])
     *
     * @param params По идее: params[0] - путь до директории, params[1] - глубина поиска
     *
     * @return true - изображения есть, false - изображений нет или можно пропустить.
     *
     */
    private fun collectImagesFromPath(params: Array<String>): Boolean {

        // Проверка числа параметров
        if(params.size != 2) {
            if(params.size == 1 && forceSkip) srcDepth = DEFAULT_DEPTH_SRC else return inputDirOption.errMsg("Invalid params!")
        } else {
            // Проверка "глубины" поиска исходных изображений
            srcDepth = params[1].toInt()
            if(srcDepth !in DEFAULT_DEPTH_SRC..MAX_DEPTH_SRC) {
                if(forceSkip)
                    srcDepth = DEFAULT_DEPTH_SRC
                else
                    return inputDirOption.errMsg("Depth must be in [$DEFAULT_DEPTH_SRC..$MAX_DEPTH_SRC]!")
            }
        }

        // Проверка пути
        val pathToDir = params[0]
        val inputDir  = File(pathToDir)
        if((!inputDir.exists() || !inputDir.isDirectory) && !forceSkip)
            return inputDirOption.errMsg("\'$pathToDir\' doesn't exist or not directory!")

        // Сбор файлов .png
        val collectedFiles = selectImages(inputDir.listFiles(), srcDepth)

        // Были ли файлы то?
        if(collectedFiles.isNotEmpty()) {
            srcImgList.addAll(collectedFiles)
            return true
        }

        // Если можно забить - забиваем.
        if(forceSkip) return true
        return inputDirOption.errMsg("$pathToDir doesn't contain .png files!")

    }


    /**
     *
     * Выбор из каталога всех файлов "поддерживаемых форматов" (.png)
     *
     * @param files Файлы, с которых начинается отбор
     * @param depth Максимальная глубина поиска. При depth = 1 переход во вложенные каталоги не производится
     *
     * @return Список файлов-изображений поддерживаемого формата (.png)
     *
     */
    private fun selectImages(files: Array<File>?, depth: Int = 1): MutableList<File> {

        val outImgList = mutableListOf<File>()
        if(files.isNullOrEmpty())
            return outImgList

        val dirsInPath = mutableListOf<File>()
        var currDepth = 0
        do {
            files.forEach { file ->
                if(file.isFile && supportedFormat(file.name.substringAfterLast('.')))
                    outImgList.add(file)
                if(file.isDirectory)
                    dirsInPath.add(file)
            }
            currDepth++
        } while(currDepth != depth)

        return outImgList

    }


    private fun supportedFormat(format: String): Boolean {
        return format == "png"
    }


    /**
     * Подсказка по использованию пакера
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

        if(clParser.parseArgs(args) != ParseResult.OK)
            return false

        return true

    }



}