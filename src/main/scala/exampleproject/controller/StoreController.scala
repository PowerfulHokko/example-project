package exampleproject.controller

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import exampleproject.model.dto._
import exampleproject.service.StoreService
import spray.json._

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object StoreController extends App with ItemJsonProtocol with ItemStockJsonProtocol {
    implicit val system: ActorSystem = ActorSystem(this.getClass.getSimpleName.replace("$",""))
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val exc: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))
    private val log = system.log

    /*
      ✓  GET /api/items
      ✓  GET /api/items/id   -> NOTE: Check implementation
        GET /api/items/stock
        GET /api/items/stock? instock=bool & id=##
        GET /api/items/category
        GET /api/items/category? id=##

      ✓  POST /api/items
        POST /api/items/stock
        POST /api/items/category

        PUT /api/items?id=##
        PUT /api/items/stock?id=##
        PUT /api/items/category?id=##

        DELETE /api/items?id=##
        DELETE /api/items/stock?id=##
        DELETE /api/items/category?id=##

     */

    //todo: endpoints
    val storeEndPoints = {
        pathPrefix("api" / "items" ){
            (pathEndOrSingleSlash & extractRequest) { req: HttpRequest =>
                get{
                    log.info(s"GET to ${req.getUri()}")
                    val items: Future[Seq[FullItem]] = StoreService.getAllItems
                    val entityFuture = items.map{ item: Seq[FullItem] =>
                        HttpEntity(ContentTypes.`application/json`, item.toJson.prettyPrint)
                    }
                    complete(entityFuture)
                } ~ post {
                    val entText: Future[String] = req.entity.toStrict(1 second).map(_.data.utf8String)
                    val text: String = Await.result[String](entText, 1 second)
                    val item: CreateItem = text.parseJson.convertTo[exampleproject.model.dto.CreateItem]
                    val createdItem : Future[Item] = StoreService.createItem(item)

                    val entity = createdItem.map((item: Item) => {
                        HttpEntity(ContentTypes.`application/json`, item.toJson.prettyPrint)
                    })
                    complete(StatusCodes.Created, entity)
                }
            } ~ path(LongNumber){ id =>
                get{
                    val optionalItem: Future[Option[FullItem]] = StoreService.getItem(id)
                    val oItem = Await.result[Option[FullItem]](optionalItem, 1 second)

                    //TODO: seems incorrect
                    if(oItem.isDefined){
                        complete(
                            StatusCodes.OK,
                            HttpEntity(ContentTypes.`application/json`, oItem.get.toJson.prettyPrint)
                        )
                    } else {
                        complete(StatusCodes.NotFound)
                    }
                }
            }
        } ~
        pathPrefix("api" / "items" / "stock"){
            parameter('itemId.as[Long], 'qty.as[Long]){ (itemId, qty) =>
                put{
                    val updatedStockWithItem : Future[Try[FullItem]] = StoreService.updateItemStock(itemId, qty)
                    val oItem = Await.result[Try[FullItem]](updatedStockWithItem, 1 second)
                    oItem match {
                        case Success(value) => {
                            complete(value.toJson.prettyPrint)
                        }
                        case Failure(exception) => {
                            complete(
                                StatusCodes.BadRequest,
                                HttpEntity(exception.getMessage)
                            )
                        }
                    }
                }
            } ~
            (pathEndOrSingleSlash) {
                get{
                    val totalStock: Future[Seq[exampleproject.model.dto.ItemStock]] = StoreService.getTotalStock
                    val entityFuture = totalStock.map{ itemStock: Seq[ItemStock] =>
                        HttpEntity(ContentTypes.`application/json`, itemStock.toJson.prettyPrint)
                    }
                    complete(entityFuture)
                }
            }
        }
    }

    Http().bindAndHandle(storeEndPoints, "localhost", 8080)

}
