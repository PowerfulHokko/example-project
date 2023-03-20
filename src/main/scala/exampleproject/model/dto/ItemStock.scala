package exampleproject.model.dto

import spray.json.DefaultJsonProtocol

case class ItemStock(id: Long, idItem: Long, stock: Long)
case class CreateItemStock(idItem: Long, stock: Long)

trait ItemStockJsonProtocol extends DefaultJsonProtocol{
    implicit val itemStockProtocol = jsonFormat3(ItemStock)
    implicit val createItemStockProtocol = jsonFormat2(CreateItemStock)
}