package exampleproject.model.dto

import spray.json.DefaultJsonProtocol

case class ItemCategory(id: Long, name: String)
case class CreateItemCategory(name: String)

trait ItemCategoryJsonProtocol extends DefaultJsonProtocol{
    implicit val itemJsonProtocol = jsonFormat2(ItemCategory)
    implicit val createItemJsonProtocol = jsonFormat1(CreateItemCategory)
}
