/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import javax.inject.Inject
import models.Error._
import models.{InvalidJsonResponse, VatReturnDetail}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.VatReturnsService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

class SubmitVatReturnController @Inject()(vatReturnsService: VatReturnsService)
                                         (implicit ec: ExecutionContext) extends BaseController {

  def submitVatReturn(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    val requestAsJson: Option[VatReturnDetail] = request.body.asJson match {
      case Some(validJson) => validJson.asOpt[VatReturnDetail]
      case None =>
        Logger.warn("[SubmitVatReturnController][submitVatReturn] Issue parsing body as json")
        Logger.debug("[SubmitVatReturnController][submitVatReturn] The body provided in the request is not valid Json")
        None
    }

    requestAsJson match {
      case Some(vatReturnModel) => vatReturnsService.submitVatReturn(vrn, vatReturnModel).map {
        case Right(responseModel) => Ok(Json.toJson(responseModel))
        case Left(error) =>
          Logger.warn("[SubmitVatReturnsController][SubmitVatReturn] Error occurred while trying to submit return")
          Logger.debug(
            "[SubmitVatReturnsController][SubmitVatReturn] The following error occurred while trying to submit a return:"
              + error.error.toJson
          )
          Status(error.status)(Json.toJson(error.toJson))
      }
      case None =>
        Logger.debug("[SubmitVatReturnsController][SubmitVatReturn] An error occurred while trying to parse incoming Json")
        Future.successful(InternalServerError(Json.toJson(InvalidJsonResponse.toJson)))
    }
  }

}