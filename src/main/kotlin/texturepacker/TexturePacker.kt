package texturepacker

import java.awt.image.*
import java.io.File
import java.io.FileWriter
import javax.imageio.*
import javax.xml.stream.XMLOutputFactory

class TexturePacker(private val conf: PackerConfiguration) {

    lateinit var atlasImage: BufferedImage
    lateinit var atlasFile: File
    lateinit var descriptionWriter: FileWriter
    lateinit var descriptionFile: File

    // debug
    fun debugOut() {

        println("=== IMAGES ===")
        for(img in conf.srcImgList)
            println(img.absoluteFile.name)
        println("==============")

        println("=== PIXEL ===")
        val raster = ImageIO.read(conf.srcImgList[3]).raster
        val sampleModel = raster.sampleModel
        val result = IntArray(10)
        sampleModel.getPixel(200, 250, result, raster.dataBuffer)

        result.forEach { println(it) }
        println("=============")

    }

    fun pack() {
        createAtlas()
        ImageNode.margin = conf.atlasMargin
        val images = Array<BufferedImage>(conf.srcImgList.size) { idx -> ImageIO.read(conf.srcImgList[idx]) }
        val imageNode = ImageNode(0, 0, conf.atlasWidth, conf.atlasHeight)
        imageNode.place(images, Array(images.size) { idx -> conf.srcImgList[idx].name.substringBeforeLast(".") })

        val xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(descriptionWriter)
        xmlWriter.writeStartDocument()
        xmlWriter.writeStartElement("frames")
        xmlWriter.writeStartElement("description")
        xmlWriter.writeAttribute("cntFrames", images.size.toString())
        xmlWriter.writeAttribute("x_res", conf.atlasWidth.toString())
        xmlWriter.writeAttribute("y_res", conf.atlasHeight.toString())
        xmlWriter.writeEndElement()

        imageNode.write(atlasImage, xmlWriter)
        xmlWriter.writeEndElement()
        xmlWriter.writeEndDocument()
        xmlWriter.close()

        saveAtlas()

    }

    private fun createAtlas() {

        atlasFile = File(conf.atlasName)
        atlasFile.createNewFile()
        atlasImage = BufferedImage(conf.atlasWidth, conf.atlasHeight, BufferedImage.TYPE_INT_ARGB)

        descriptionFile = File(conf.descriptionName)
        descriptionFile.createNewFile()
        descriptionWriter = FileWriter(descriptionFile)

    }

    private fun saveAtlas() {
        descriptionWriter.close()
        ImageIO.write(atlasImage, "png", atlasFile)
    }



}