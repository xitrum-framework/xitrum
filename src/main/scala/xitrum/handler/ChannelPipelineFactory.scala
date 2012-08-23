package xitrum.handler

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}
import org.jboss.netty.handler.execution.{ExecutionHandler, OrderedMemoryAwareThreadPoolExecutor}

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._

object ChannelPipelineFactory {
  def removeUnusedDefaultHttpHandlersForWebSocket(pipeline: ChannelPipeline) {
    pipeline.remove(classOf[NoPipelining])
    pipeline.remove(classOf[BasicAuth])
    pipeline.remove(classOf[BaseUrlRemover])
    pipeline.remove(classOf[PublicFileServer])
    pipeline.remove(classOf[PublicResourceServer])
    pipeline.remove(classOf[Request2Env])
    pipeline.remove(classOf[UriParser])
    pipeline.remove(classOf[BodyParser])
    pipeline.remove(classOf[MethodOverrider])
    pipeline.remove(classOf[Dispatcher])

    pipeline.remove(classOf[XSendFile])
    pipeline.remove(classOf[XSendResource])
    pipeline.remove(classOf[Env2Response])
    pipeline.remove(classOf[ResponseCacher])
  }
}

// See doc/sphinx/handler.rst
class ChannelPipelineFactory(https: Boolean) extends CPF {
  // Sharable handlers

  private val noPipelining         = new NoPipelining
  private val basicAuth            = new BasicAuth
  private val baseUrlRemover       = new BaseUrlRemover
  private val publicFileServer     = new PublicFileServer
  private val publicResourceServer = new PublicResourceServer
  private val request2Env          = new Request2Env
  private val uriParser            = new UriParser
  private val bodyParser           = new BodyParser
  private val methodOverrider      = new MethodOverrider
  private val dispatcher           = new Dispatcher

  private val xSendFile            = new XSendFile
  private val xSendResource        = new XSendResource
  private val env2Response         = new Env2Response
  private val responseCacher       = new ResponseCacher

  def getPipeline: ChannelPipeline = {
    val handlers1 = httpHandlers
    val handlers2 = if (https) ServerSsl.handler() +: handlers1 else handlers1

    // WebSocket handshaker in Netty dynamically changes the pipeline like this:
    // pipeline.remove(classOf[HttpChunkAggregator])
    // pipeline.replace(classOf[HttpRequestDecoder], "wsdecoder", new WebSocket08FrameDecoder(true, this.allowExtensions))
    // pipeline.replace(classOf[HttpResponseEncoder], "wsencoder", new WebSocket08FrameEncoder(false))
    Channels.pipeline(handlers2:_*)
  }

  /**
   * You can override this method to customize the default pipeline.
   *
   * Upstream direction: first handler -> last handler
   * Downstream direction: last handler -> first handler
   */
  def httpHandlers = List(
    // Up
    new HttpRequestDecoder,
    new HttpChunkAggregator(Config.config.request.maxSizeInMB * 1024 * 1024),
    noPipelining,
    basicAuth,
    baseUrlRemover,  // HttpRequest is attached to the channel here
    publicFileServer,
    publicResourceServer,
    request2Env,
    uriParser,
    bodyParser,
    methodOverrider,
    dispatcher,

    // Down
    new HttpResponseEncoder,
    xSendFile,
    xSendResource,
    env2Response,
    responseCacher
  )
}
