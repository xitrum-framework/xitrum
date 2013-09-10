package xitrum.handler

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}
import org.jboss.netty.handler.execution.{ExecutionHandler, OrderedMemoryAwareThreadPoolExecutor}
import org.jboss.netty.handler.stream.ChunkedWriteHandler

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._

/**
 * Sharable handlers are put here so that they can be easily picked up by apps
 * that want to use custom pipeline. Those apps may only want a subset of
 * default handlers.
 *
 * When an app uses its own dispatcher (not Xitrum's routing/dispatcher) and
 * only needs Xitrum's fast static file serving, it may use only these handlers:
 *
 * Upstream:
 *   HttpRequestDecoder
 *   noPipelining
 *   requestAttacher
 *   publicFileServer
 *   its own dispatcher
 *
 * Downstream:
 *   HttpResponseEncoder
 *   ChunkedWriteHandler
 *   xSendFile
 */
object DefaultHttpChannelPipelineFactory {
  // Upstream sharable handlers

  lazy val noPipelining         = new NoPipelining
  lazy val baseUrlRemover       = new BaseUrlRemover
  lazy val basicAuth            = new BasicAuth
  lazy val requestAttacher      = new RequestAttacher
  lazy val publicFileServer     = new PublicFileServer
  lazy val publicResourceServer = new PublicResourceServer
  lazy val request2Env          = new Request2Env
  lazy val uriParser            = new UriParser
  lazy val bodyParser           = new BodyParser
  lazy val methodOverrider      = new MethodOverrider
  lazy val dispatcher           = new Dispatcher

  // Downstream sharable handlers

  lazy val fixiOS6SafariPOST    = new FixiOS6SafariPOST
  lazy val xSendFile            = new XSendFile
  lazy val xSendResource        = new XSendResource
  lazy val env2Response         = new Env2Response
  lazy val responseCacher       = new ResponseCacher

  def removeUnusedHandlersForWebSocket(pipeline: ChannelPipeline) {
    // WebSocket handshaker in Netty dynamically changes the pipeline like this:
    // pipeline.remove(classOf[HttpChunkAggregator])
    // pipeline.replace(classOf[HttpRequestDecoder],  "wsdecoder", new WebSocket08FrameDecoder(true, this.allowExtensions))
    // pipeline.replace(classOf[HttpResponseEncoder], "wsencoder", new WebSocket08FrameEncoder(false))

    // Upstream

    pipeline.remove(classOf[NoPipelining])
    pipeline.remove(classOf[BaseUrlRemover])
    if (Config.xitrum.basicAuth.isDefined)
    pipeline.remove(classOf[BasicAuth])
    pipeline.remove(classOf[PublicFileServer])
    pipeline.remove(classOf[PublicResourceServer])
    pipeline.remove(classOf[Request2Env])
    pipeline.remove(classOf[UriParser])
    pipeline.remove(classOf[BodyParser])
    pipeline.remove(classOf[MethodOverrider])
    pipeline.remove(classOf[Dispatcher])

    // Downstream

    pipeline.remove(classOf[ChunkedWriteHandler])
    pipeline.remove(classOf[FixiOS6SafariPOST])
    pipeline.remove(classOf[XSendFile])
    pipeline.remove(classOf[XSendResource])
    pipeline.remove(classOf[Env2Response])
    pipeline.remove(classOf[ResponseCacher])
  }
}

class DefaultHttpChannelPipelineFactory extends CPF {
  import DefaultHttpChannelPipelineFactory._

  /**
   * You can override this method to customize the default pipeline.
   *
   * Upstream direction: first handler -> last handler
   * Downstream direction: last handler -> first handler
   */
  def getPipeline(): ChannelPipeline = {
    // This method is run for every request, thus should be fast

    val ret = Channels.pipeline()

    // Upstream

    ret.addLast("HttpRequestDecoder",   new HttpRequestDecoder)
    ret.addLast("HttpChunkAggregator",  new HttpChunkAggregator(Config.xitrum.request.maxSizeInMB * 1024 * 1024))
    ret.addLast("noPipelining",         noPipelining)
    ret.addLast("baseUrlRemover",       baseUrlRemover)
    if (Config.xitrum.basicAuth.isDefined)
    ret.addLast("basicAuth",            basicAuth)
    ret.addLast("requestAttacher",      requestAttacher)       // <- Must be before publicFileServer and publicResourceServer
    ret.addLast("publicFileServer",     publicFileServer)      // because HttpRequest must be attached to the channel for
    ret.addLast("publicResourceServer", publicResourceServer)  // use at xSendFile and xSendResource (and fixiOS6SafariPOST)
    ret.addLast("request2Env",          request2Env)
    ret.addLast("uriParser",            uriParser)
    ret.addLast("bodyParser",           bodyParser)
    ret.addLast("methodOverrider",      methodOverrider)
    ret.addLast("dispatcher",           dispatcher)

    // Downstream

    ret.addLast("HttpResponseEncoder", new HttpResponseEncoder)
    ret.addLast("ChunkedWriteHandler", new ChunkedWriteHandler)  // For writing ChunkedFile, at XSendFile
    ret.addLast("fixiOS6SafariPOST",   fixiOS6SafariPOST)
    ret.addLast("xSendFile",           xSendFile)
    ret.addLast("xSendResource",       xSendResource)
    ret.addLast("env2Response",        env2Response)
    ret.addLast("responseCacher",      responseCacher)

    ret
  }
}

/** This is a wrapper. It prepends SSL handler to the non-SSL pipeline. */
class SslChannelPipelineFactory(nonSsl: CPF) extends CPF {
  def getPipeline(): ChannelPipeline = {
    val ret = nonSsl.getPipeline()
    ret.addFirst("SSL", ServerSsl.handler())
    ret
  }
}
