package exampleproject.model.dto

import spray.json.DefaultJsonProtocol

case class Item(id: Long, categoryId: Long, name: String)
case class CreateItem(categoryId: Long, name: String)
case class FullItem(id: Long, name: String, category: String, stock: Long)

trait ItemJsonProtocol extends DefaultJsonProtocol{
    implicit val itemFormat = jsonFormat3(Item)
    implicit val createItemFormat = jsonFormat2(CreateItem)
    implicit val itemFullFormat = jsonFormat4(FullItem)
}