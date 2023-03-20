package exampleproject.service

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import exampleproject.database.StoreDB
import exampleproject.model.db.{Item, ItemCategory, ItemStock}
import exampleproject.model.dto
import exampleproject.model.dto.FullItem

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object StoreService {

    implicit val system: ActorSystem = ActorSystem(this.getClass.getSimpleName.replace("$",""))
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val exc: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))
    import StoreDB._
    val storeDB: ActorRef = system.actorOf(Props(new StoreDB))

    implicit val timeout = Timeout(2 second)
    def getAllItems : Future[Seq[FullItem]] = {
        (storeDB ? GetAllItems)
            .mapTo[Future[Seq[((Item, Option[ItemCategory]), Option[ItemStock])]]]
            .flatten
            .map((seq: Seq[((Item, Option[ItemCategory]), Option[ItemStock])]) => {
                seq.map((itemT: ((Item, Option[ItemCategory]), Option[ItemStock])) => {
                    FullItem(
                        itemT._1._1.id,
                        itemT._1._1.name,
                        itemT._1._2.getOrElse(ItemCategory(0,"UNDEFINED")).name,
                        itemT._2.getOrElse(ItemStock(0,0,0)).stock
                    )
                })
            })
    }

    def getItem(id: Long): Future[Option[FullItem]] = {
        (storeDB ? FindItem(id)).mapTo[Future[Option[((Item, Option[ItemCategory]), Option[ItemStock])]]]
            .flatten
            .map((option: Option[((Item, Option[ItemCategory]), Option[ItemStock])]) => {
                option.map((itemTuple: ((Item, Option[ItemCategory]), Option[ItemStock])) => {
                    FullItem(
                        itemTuple._1._1.id,
                        itemTuple._1._1.name,
                        itemTuple._1._2.getOrElse(ItemCategory(0,"UNDEFINED")).name,
                        itemTuple._2.getOrElse(ItemStock(0,0,0)).stock
                    )
                })
            })
    }


    def createItem(item: exampleproject.model.dto.CreateItem): Future[dto.Item] = {
        (storeDB ? CreateItem(item))
            .mapTo[Future[exampleproject.model.db.Item]]
            .flatten
            .map(i => exampleproject.model.dto.Item(i.id, i.categoryId, i.name))
    }

    def getTotalStock: Future[Seq[dto.ItemStock]] = {
        (storeDB ? GetAllStock)
            .mapTo[Future[Seq[exampleproject.model.db.ItemStock]]]
            .flatten
            .map((seq: Seq[ItemStock]) => {
                seq.map(i => {
                    exampleproject.model.dto.ItemStock(i.id, i.idItem, i.stock)
                })
            })
    }

    def updateItemStock(itemId: Long, qty: Long): Future[Try[FullItem]] = {
        //getItemStock and check
        //then update if valid
        (storeDB ? UpdateItemStock(itemId, qty))
            .mapTo[Future[Try[((Item, Option[ItemCategory]), Option[ItemStock])]]]
            .flatten
            .map((tr: Try[((Item, Option[ItemCategory]), Option[ItemStock])]) => {
                tr match {
                    case Failure(exception) => Failure(exception)
                    case Success(value) => {
                        val res = FullItem(
                            value._1._1.id,
                            value._1._1.name,
                            value._1._2.getOrElse(ItemCategory(0,"UNDEFINED")).name,
                            value._2.getOrElse(ItemStock(0,0,0)).stock
                        )
                        Success(res)
                    }
                }
            })
    }


}
