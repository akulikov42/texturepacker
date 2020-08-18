package texturepacker

import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.FileWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import kotlin.math.sqrt


class ImageNode(x: Int, y: Int, w: Int, h: Int) {

    companion object {
        var margin = 0
    }

    private data class ImageInfo(val src: BufferedImage, val name: String)

    var img         : BufferedImage?    = null
    var name        : String            = ""
    var rect        : Rectangle         = Rectangle(x, y, w, h)
    var firstChild  : ImageNode?        = null
    var secondChild : ImageNode?        = null

    private fun insert(inputImg: BufferedImage, inputName: String): Boolean {
        if(img == null) {
            if(inputImg.width + margin * 2 > rect.width || inputImg.height + margin * 2 > rect.height)
                return false
            val oldWidth  = rect.width
            val oldHeight = rect.height
            img  = inputImg
            name = inputName
            rect.width  = inputImg.width  + margin * 2
            rect.height = inputImg.height + margin * 2
            if(rect.height > rect.width) {
                firstChild  = ImageNode(rect.x + rect.width, rect.y, oldWidth - rect.width, rect.height)
                secondChild = ImageNode(rect.x, rect.y + rect.height, oldWidth, oldHeight - rect.height)
            } else {
                firstChild  = ImageNode(rect.x, rect.y + rect.height, rect.width, oldHeight - rect.height)
                secondChild = ImageNode(rect.x + rect.width, rect.y, oldWidth - rect.width, oldHeight)
            }
            return true
        }

        val deltaHeightFirst  = inputImg.height - firstChild!!.rect.height
        val deltaWidthFirst   = inputImg.width  - firstChild!!.rect.width
        val deltaHeightSecond = inputImg.height - secondChild!!.rect.height
        val deltaWidthSecond  = inputImg.width  - secondChild!!.rect.width

        if(diag(deltaHeightFirst, deltaWidthFirst) < diag(deltaHeightSecond, deltaWidthSecond)) {
            return if(firstChild!!.insert(inputImg, inputName))
                true
            else secondChild!!.insert(inputImg, inputName)
        } else {
            return if(secondChild!!.insert(inputImg, inputName))
                true
            else firstChild!!.insert(inputImg, inputName)
        }

    }

    private fun sortImages(images: Array<BufferedImage>, names: Array<String>) {
        val imgs = Array(images.size) { idx -> ImageInfo(images[idx], names[idx]) }
        imgs.sortBy{ diag(it.src.width, it.src.height) }
        for(idx in imgs.indices) {
            images[idx] = imgs[idx].src
            names [idx] = imgs[idx].name
        }
    }

    private fun diag(a: Double, b: Double): Double = sqrt(a * a + b * b)
    private fun diag(a: Int, b: Int): Double = sqrt((a * a + b * b).toDouble())

    fun place(images: Array<BufferedImage>, names: Array<String>): Boolean {
        sortImages(images, names)
        for(idx in images.indices) {
            if(!insert(images[idx], names[idx]))
                return false
        }
        return true
    }

    fun write(destinationImage: BufferedImage, xmlWriter: XMLStreamWriter) {

        if(img == null)
            return

        xmlWriter.writeStartElement("frame")
        xmlWriter.writeAttribute("name", name)
        xmlWriter.writeAttribute("x", (rect.x + margin).toString())
        xmlWriter.writeAttribute("y", (rect.y + margin).toString())
        xmlWriter.writeAttribute("w", (rect.width  - margin * 2).toString())
        xmlWriter.writeAttribute("h", (rect.height - margin * 2).toString())
        xmlWriter.writeEndElement()

        destinationImage.graphics.drawImage(
                img,
                rect.x + margin,
                rect.y + margin,
                img!!.width,
                img!!.height,
                null
        )

        firstChild!!.write(destinationImage, xmlWriter)
        secondChild!!.write(destinationImage, xmlWriter)

    }


}