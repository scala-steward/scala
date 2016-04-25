/**
  * Created by cmartin on 24/04/16.
  */
import xml.XML.loadFile
import scala.xml.Node

val fileName = "resolverOperacionesNoEjecutadasBandejaCliente"

val xmlFilePath = "resources/" + fileName + ".xml"


def isModel(node: Node): Boolean = {
  val name: String = (node \ "@name").text
  name.endsWith("Model") || name.endsWith("View")
}

def isType(node: Node): Boolean = {
  val tag: String = (node \ "@tag").text
  tag == "type"
}

def getNodeName(node: Node): String = {
  (node \ "@name").text
}

def getNodeValue(node: Node): String = {
  (node \ "@value").text
}

def convertType(t: String): String =
  t match {
    case "Cadena de Caracteres" => "String"
    case "Importe Monetario" => "BigDecimal"
    case _ => t
}

def repeatChar(c: Char, n: Int): String = c.toString * n

def pad(n: Int): String = " " * n

def makeLine(n: Int, line: String): String = pad(n) + line

// read xml file from disk
val xmlFile = loadFile(xmlFilePath)
println("toString length: " + xmlFile.toString().length / 1024)

val classNodes = (xmlFile \\ "Class").filter(isModel(_))

var nodeCount = classNodes.size
println(f"classElements has $nodeCount elements\n")

println(makeLine(0, "definitions:"))
for (cn <- classNodes) {
  val className = getNodeName(cn)
  println(makeLine(2, className + ':'))
  println(makeLine(4, "type: object"))
  println(makeLine(4, "properties:"))
  val attributeNodes = cn \\ "Attribute"
  for (an <- attributeNodes) {
    val attributeName = getNodeName(an)
    val typeNodes = (an \\ "TaggedValue").filter(isType(_))
    val typeName = convertType(getNodeValue(typeNodes.head))

    println(makeLine(6, attributeName + ':'))
    println(makeLine(8, "type: " + typeName))
  }
}
