package exampleproject.database

import akka.actor.{Actor, ActorLogging}
import exampleproject.model.db.{Item, ItemCategory, ItemStock}
import slick.sql.FixedSqlAction

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object StoreDB {
    case object GetAllItems
    case class FindItem(id: Long)
    case class CreateItem(item: exampleproject.model.dto.CreateItem)
    case class UpdateItem(item: exampleproject.model.dto.Item)
    case class DeleteItem(id: Int)

    case object GetAllStock
    case class GetItemStock(id: Long)
    case class CreateItemStock(itemStock: exampleproject.model.dto.CreateItemStock)
    case class UpdateItemStock(id: Long, amount: Long)
    case class DeleteItemStock(id: Long)

    case object GetAllItemCategory
    case class GetItemCategory(id: Int)
    case class CreateItemCategory(itemCategory: exampleproject.model.dto.CreateItemCategory)
    case class UpdateItemCategory(itemCategory: exampleproject.model.dto.ItemCategory)
    case class DeleteItemCategory(id: Int)

}

class StoreDB extends Actor with ActorLogging{
    import Connection._
    import StoreDB._
    import Tables._
    import slick.jdbc.PostgresProfile.api._

    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(5)
    )

    override def receive: Receive = {
        case  GetAllItems => {
            val query = Tables.itemTable
                .joinLeft(Tables.itemCategoryTable).on( (a,b) => a.category_id === b.id)
                .joinLeft(Tables.itemStockTable).on( (a,b) => a._1.id === b.item_id)

            val result: Future[Seq[((Item, Option[ItemCategory]), Option[ItemStock])]] = Connection.db.run(query.result)
            sender() !  result
        }

        case  FindItem(itemId: Long) => {
            val query = Tables.itemTable
                .filter(_.id === itemId)
                .joinLeft(Tables.itemCategoryTable).on( (a,b) => a.category_id === b.id)
                .joinLeft(Tables.itemStockTable).on( (a,b) => a._1.id === b.item_id)

            val result: Future[Option[((Item, Option[ItemCategory]), Option[ItemStock])]] = Connection.db.run(query.result.headOption)
            sender() ! result
        }

        case  CreateItem(item: exampleproject.model.dto.CreateItem) => {
            val dbItemToCreate: Item = exampleproject.model.db.Item(0, item.categoryId, item.name)
            val insertQueryItem = Tables.itemTable returning Tables.itemTable.map(_.id) into((item, id) => item.copy(id = id))
            val query: FixedSqlAction[Item, NoStream, Effect.Write] = insertQueryItem += dbItemToCreate
            sender() ! Connection.db.run(query)
        }

        case  GetAllStock => {
            val query = Tables.itemStockTable.result
            sender() ! Connection.db.run(query)
        }

        case  CreateItemStock(itemStock: exampleproject.model.dto.CreateItemStock) =>

        case  UpdateItemStock(id: Long, amount: Long) => {
            val getItemStockQuery = Tables.itemStockTable.filter(_.item_id === id)
            val getItemStockOptional: Future[Option[ItemStock]] = Connection.db.run(getItemStockQuery.result.headOption)
            val itemStockOptional: Option[ItemStock] = Await.result[Option[ItemStock]](getItemStockOptional, 1 second)

            if(itemStockOptional.isEmpty){
                sender() ! Future(Failure(new NoSuchElementException(s"No item with id $id in stock")))
            } else {
                if(itemStockOptional.filter(_.stock + amount < 0).isDefined){
                    sender() ! Future(Failure(new IllegalStateException("Stock qty should not go below zero")))
                } else {
                    val item = itemStockOptional.get;
                    val updateQuery = Tables.itemStockTable.filter(_.item_id === id).update(
                        ItemStock(item.id, item.idItem, item.stock + amount)
                    )

                    val updateRun: Future[Int] = Connection.db.run(updateQuery)

                    val query = Tables.itemTable
                        .filter(_.id === id)
                        .joinLeft(Tables.itemCategoryTable).on( (a,b) => a.category_id === b.id)
                        .joinLeft(Tables.itemStockTable).on( (a,b) => a._1.id === b.item_id)

                    val queryResult: Future[Option[((Item, Option[ItemCategory]), Option[ItemStock])]] = Connection.db.run(query.result.headOption)
                    val result: Future[Try[((Item, Option[ItemCategory]), Option[ItemStock])]] = queryResult.map(t => {
                        t match {
                            case None => Failure(new NoSuchElementException(s"Item $id not found in database"))
                            case Some(value) => Success(value)
                        }
                    })

                    sender() ! result
                }
            }
        }

    }

    //database methods
    import slick.jdbc.PostgresProfile.api._

    object Tables{
        import slick.jdbc.PostgresProfile
        import slick.jdbc.PostgresProfile.api._
        import slick.relational._

        private val schema = Some("examplestore")

        private val itemModel = exampleproject.model.db.Item.tupled
        private val itemStockModel = exampleproject.model.db.ItemStock.tupled
        private val itemCategoryModel = exampleproject.model.db.ItemCategory.tupled

        //Entrypoints
        lazy val itemTable = TableQuery[ItemTable]
        lazy val itemStockTable = TableQuery[ItemStockTable]
        lazy val itemCategoryTable = TableQuery[ItemCategoryTable]

        //todo: Tables
        class ItemTable(tag: Tag) extends Table[exampleproject.model.db.Item](tag, schema, "item"){
            def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
            def category_id = column[Long]("id_category", O.Default(1))
            def name = column[String]("name", O.Unique)

            //mapping f to the case class
            override def * =
                (id, category_id, name) <> (itemModel, exampleproject.model.db.Item.unapply)
        }

        class ItemStockTable(tag: Tag) extends Table[exampleproject.model.db.ItemStock](tag, schema, "item_stock"){
            def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
            def item_id = column[Long]("item_id", O.Unique)
            def stock = column[Long]("stock", O.Default(0))

            override def * =
                (id, item_id, stock) <> (itemStockModel, exampleproject.model.db.ItemStock.unapply)
        }

        class ItemCategoryTable(tag: Tag) extends Table[exampleproject.model.db.ItemCategory](tag, schema, "item_category"){
            def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
            def name = column[String]("name", O.Unique)

            override def * =
                (id, name) <> (itemCategoryModel,exampleproject.model.db.ItemCategory.unapply)
        }

    }

    object Connection{
        import slick.jdbc.PostgresProfile.api._
        val db = Database.forConfig("postgres")
    }

}