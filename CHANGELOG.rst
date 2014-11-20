3.20:

* `#480 <https://github.com/xitrum-framework/xitrum/issues/480>`_
  Update Glokka from 2.0 to 2.2

3.19:

* `#452 <https://github.com/xitrum-framework/xitrum/issues/452>`_
  Postback: Split data-extra to data-params and data-form
* `#460 <https://github.com/xitrum-framework/xitrum/issues/460>`_
  Make respondView more convenient, so that one action can have multiple views
* `#463 <https://github.com/xitrum-framework/xitrum/issues/463>`_
  Do not ignore empty uploaded files
* `#466 <https://github.com/xitrum-framework/xitrum/issues/466>`_
  Add xitrum.util.Loader.bytesToFile method to write to file
* `#467 <https://github.com/xitrum-framework/xitrum/issues/467>`_
  Improve xitrum.util.SeriDeseri methods
* `#474 <https://github.com/xitrum-framework/xitrum/issues/474>`_
  Add isEmpty to SessionVar for convenience
* `#475 <https://github.com/xitrum-framework/xitrum/issues/475>`_
  Make writing before filters more convenient:
  If a filter responds something, the main action will not be called;
  you don't have to make the filter return true/false any more
* `#462 <https://github.com/xitrum-framework/xitrum/issues/462>`_
  Switch back to Twitter's Chill (from chill-scala) because it now supports Scala 2.11
* `#476 <https://github.com/xitrum-framework/xitrum/issues/476>`_
  Update Netty from 4.0.23 to 4.0.24;
  SSLv3.0 is disabled to avoid POODLE vulnerability
* `#461 <https://github.com/xitrum-framework/xitrum/issues/461>`_
  Update Akka from 2.3.5 to 2.3.7
* `#465 <https://github.com/xitrum-framework/xitrum/issues/465>`_
  Update Scaposer from 1.4 to 1.5;
  the original string is used when it hasn't been translated yet
* `#478 <https://github.com/xitrum-framework/xitrum/issues/478>`_
  Update JSON4S from 3.2.10 to 3.2.11
* `#469 <https://github.com/xitrum-framework/xitrum/issues/469>`_
  Update metrics-scala from 3.2.1_a2.3 to 3.3.0_a2.3
* `#470 <https://github.com/xitrum-framework/xitrum/issues/470>`_
  Update RhinoCoffeeScript from 1.7.1 to 1.8.0
* `#471 <https://github.com/xitrum-framework/xitrum/issues/471>`_
  Update Swagger UI from 2.0.22 to 2.0.24
* `#479 <https://github.com/xitrum-framework/xitrum/issues/479>`_
  Update D3.js from 3.4.11 to 3.4.13

3.18:

* `#446 <https://github.com/xitrum-framework/xitrum/issues/446>`_
  Fix bug: Action cache does not work when the response is not gzip
* `#307 <https://github.com/xitrum-framework/xitrum/issues/307>`_
  Add option edgeTriggeredEpoll to xitrum.conf to use Netty's epoll feature for
  maximal performance and low latency
* `#429 <https://github.com/xitrum-framework/xitrum/issues/429>`_
  Generate Swagger API doc listing so that codegen tool works
* `#448 <https://github.com/xitrum-framework/xitrum/issues/448>`_
  Update Swagger UI from 2.0.18 to 2.0.22
* `#443 <https://github.com/xitrum-framework/xitrum/issues/443>`_
  paramo returns None for empty param, params and paramso ignore empty params
* `#438 <https://github.com/xitrum-framework/xitrum/issues/438>`_
  In dev mode, only recollect routes when there's .class file change
* `#441 <https://github.com/xitrum-framework/xitrum/issues/441>`_
  Move routes.cache to tmp directory
* `#442 <https://github.com/xitrum-framework/xitrum/issues/442>`_
  Add respond404Page and respond500Page
* `#444 <https://github.com/xitrum-framework/xitrum/issues/444>`_
  Update Akka from 2.3.4 to 2.3.5
* `#445 <https://github.com/xitrum-framework/xitrum/issues/445>`_
  Update Netty from 4.0.21 to 4.0.23
* `#449 <https://github.com/xitrum-framework/xitrum/issues/449>`_
  Update metrics-scala from 3.2.0_a2.3 to 3.2.1_a2.3

3.17:

* `#434 <https://github.com/xitrum-framework/xitrum/issues/434>`_
  [Autoreload in dev mode] Replace classloaders with DCEVM
* `#436 <https://github.com/xitrum-framework/xitrum/issues/436>`_
  Check for CSRF when request method is PATCH
* `#439 <https://github.com/xitrum-framework/xitrum/issues/439>`_
  Fix type cast error at PoLoader
* `#432 <https://github.com/xitrum-framework/xitrum/issues/432>`_
  Update Swagger UI from 2.0.17 to 2.0.18
* `#433 <https://github.com/xitrum-framework/xitrum/issues/433>`_
  Update D3.js from 3.4.8 to 3.4.11
* `#435 <https://github.com/xitrum-framework/xitrum/issues/435>`_
  Update jQuery Validation from 0.12.0 to 0.13.0

3.16:

* `#428 <https://github.com/xitrum-framework/xitrum/issues/428>`_
  Fix: SeriDeseri.{fromJson, toJson} doesn't work in dev mode
* `#416 <https://github.com/xitrum-framework/xitrum/issues/416>`_
  [Autoreload in dev mode] Autoreload all directories in classpath
  (with this improvement, Xitrum can autoreload dependency modules in
  multimodule SBT projects)
* `#430 <https://github.com/xitrum-framework/xitrum/issues/430>`_
  Fix: JS error at metrics page of indiviual actions

3.15:

* `#415 <https://github.com/xitrum-framework/xitrum/issues/415>`_
  Fix: publicUrl/3 reverses paths of development mode and production mode
* `#420 <https://github.com/xitrum-framework/xitrum/issues/420>`_
  [Autoreload in dev mode] Allow disabling autoreload
* `#418 <https://github.com/xitrum-framework/xitrum/issues/418>`_
  [Autoreload in dev mode] Allow specifying classes that shouldn't be reloaded
* `#426 <https://github.com/xitrum-framework/xitrum/issues/426>`_
  Add xitrum.Config.xitrum.tmpDir
* `#413 <https://github.com/xitrum-framework/xitrum/issues/413>`_
  [Metrics viewer] Add reconnection
* `#425 <https://github.com/xitrum-framework/xitrum/issues/425>`_
  Update Netty from 4.0.20 to 4.0.21
* `#424 <https://github.com/xitrum-framework/xitrum/issues/424>`_
  Update Akka from 2.3.3 to 2.3.4

3.14:

* `#400 <https://github.com/xitrum-framework/xitrum/issues/400>`_
  Support both Scala 2.10.x and 2.11.x
* `#81 <https://github.com/xitrum-framework/xitrum/issues/81>`_
  Reload classes in development mode
* `#398 <https://github.com/xitrum-framework/xitrum/issues/398>`_
  Recollect routes in development mode
* `#399 <https://github.com/xitrum-framework/xitrum/issues/399>`_
  Add xitrum.Component
* `#390 <https://github.com/xitrum-framework/xitrum/issues/390>`_
  Add validation method "check" that returns true/false
* `#393 <https://github.com/xitrum-framework/xitrum/issues/393>`_
  For development mode, set Netty's ResourceLeakDetector level to ADVANCED
* `#395 <https://github.com/xitrum-framework/xitrum/issues/395>`_
  Fix memory leak at xitrum.util.SeriDeseri.fromUrlSafeBase64
* `#394 <https://github.com/xitrum-framework/xitrum/issues/394>`_
  Add automatic error handling (error 500) for ActorAction
* `#404 <https://github.com/xitrum-framework/xitrum/issues/404>`_
  At boot, add config directory to classpath, if it's not in classpath
* `#411 <https://github.com/xitrum-framework/xitrum/issues/411>`_
  Set Action#requestCookies to empty when there's problem decoding cookies
* `#408 <https://github.com/xitrum-framework/xitrum/issues/408>`_
  Metrics: Ignore the actions of metrics itself
* `#409 <https://github.com/xitrum-framework/xitrum/issues/409>`_
  Update Netty from 4.0.19 to 4.0.20
* `#410 <https://github.com/xitrum-framework/xitrum/issues/410>`_
  Allow the use of OpenSSL engine for faster HTTPS
* `#407 <https://github.com/xitrum-framework/xitrum/issues/407>`_
  Update Javassist from 3.18.1-GA to 3.18.2-GA

3.13:

* `#363 <https://github.com/xitrum-framework/xitrum/issues/363>`_
  Use SLF4S instead of using SLF4J directly
* `#385 <https://github.com/xitrum-framework/xitrum/issues/385>`_
  Support WebJars; rename resourceUrl to webJarsUrl
* `#383 <https://github.com/xitrum-framework/xitrum/issues/383>`_
  Update Scala from 2.11.0 to 2.11.1
* `#384 <https://github.com/xitrum-framework/xitrum/issues/384>`_
  Update Akka from 2.3.2 to 2.3.3
* `#387 <https://github.com/xitrum-framework/xitrum/issues/387>`_
  Update JSON4S from 3.2.9 to 3.2.10
* `#388 <https://github.com/xitrum-framework/xitrum/issues/388>`_
  Update metrics-scala from 3.1.1.1_a2.3 to 3.2.0_a2.3

3.12:

* `#372 <https://github.com/xitrum-framework/xitrum/issues/372>`_
  ParamAccess: Support all primitive types
* `#373 <https://github.com/xitrum-framework/xitrum/issues/373>`_
  Add respondWebSocketJson and respondSockJsJson
* `#374 <https://github.com/xitrum-framework/xitrum/issues/374>`_
  Merge UrlSafeBase64 to SeriDeseri; Also solve memory leak problem
* `#375 <https://github.com/xitrum-framework/xitrum/issues/375>`_
  Add toBase64 and fromBase64 to SeriDeseri
* `#376 <https://github.com/xitrum-framework/xitrum/issues/376>`_
  Embed Xitrum CSRF token to requests from Swagger UI
* `#379 <https://github.com/xitrum-framework/xitrum/issues/379>`_
  Add xitrum.version
* `#380 <https://github.com/xitrum-framework/xitrum/issues/380>`_
  Recreate routes.cache when Xitrum is updated
* `#362 <https://github.com/xitrum-framework/xitrum/issues/362>`_
  Update Schwatcher from 0.1.4 to 0.1.5
* `#381 <https://github.com/xitrum-framework/xitrum/issues/381>`_
  Update D3.js from 3.4.6 to 3.4.7
* `#382 <https://github.com/xitrum-framework/xitrum/issues/382>`_
  Update Swagger-UI from 2.0.16 to 2.0.17

3.11:

* `#357 <https://github.com/xitrum-framework/xitrum/issues/357>`_
  Update Scala from 2.10.4 to 2.11.0
* `#361 <https://github.com/xitrum-framework/xitrum/issues/361>`_
  Update Netty from 4.0.18 to 4.0.19
* `#358 <https://github.com/xitrum-framework/xitrum/issues/358>`_
  Update JSON4S from 3.2.8 to 3.2.9
* `#359 <https://github.com/xitrum-framework/xitrum/issues/359>`_
  Update metrics-scala from 3.0.5_a2.3 to 3.1.1.1_a2.3
* `#365 <https://github.com/xitrum-framework/xitrum/issues/365>`_
  Update Schwatcher from 0.1.1 to 0.1.4
* `#368 <https://github.com/xitrum-framework/xitrum/issues/368>`_
  Update jQuery from 1.11.0 to 1.11.1
* `#369 <https://github.com/xitrum-framework/xitrum/issues/369>`_
  jQuery Validate from 1.11.1 to 1.12.0
* `#370 <https://github.com/xitrum-framework/xitrum/issues/370>`_
  Update Swagger-UI from 2.0.14 to 2.0.16
* `#356 <https://github.com/xitrum-framework/xitrum/issues/356>`_
  Fix: Remove metrics route when metrics is disabled
* `#360 <https://github.com/xitrum-framework/xitrum/issues/360>`_
  Split "Imperatively" feature (using Scla continuations) to a separate project
* `#143 <https://github.com/xitrum-framework/xitrum/issues/143>`_
  Replace Manifest with TypeTag when Scala 2.11 is released
* `#364 <https://github.com/xitrum-framework/xitrum/issues/364>`_
  Replace Twitter Chill with chill-scala-2.11

3.7:

* `#354 <https://github.com/xitrum-framework/xitrum/issues/354>`_
  Fix HTTP keep alive and pipelining do not work

3.6:

* `#347 <https://github.com/xitrum-framework/xitrum/issues/347>`_
  Fix Terrible performance of FutureAction and ActorAction
* `#348 <https://github.com/xitrum-framework/xitrum/issues/348>`_
  Fix bug: Unable to change Content-Type for respondFile and respondResource
* `#80 <https://github.com/xitrum-framework/xitrum/issues/80>`_
  Integrate Metrics (for actions statistics etc.)
* `#337 <https://github.com/xitrum-framework/xitrum/issues/337>`_
  Update Scala from 2.10.3 to 2.10.4 (you need to update your own projects to use Scala 2.10.4)
* `#339 <https://github.com/xitrum-framework/xitrum/issues/339>`_
  Update Netty from 4.0.17 to 4.0.18
* `#342 <https://github.com/xitrum-framework/xitrum/issues/342>`_
  Update Glokka from 1.7 to 1.8 (update Akka from 2.3.0 to 2.3.2)
* `#332 <https://github.com/xitrum-framework/xitrum/issues/332>`_
  Update JSON4S from 3.2.7 to 3.2.8 and replace json4s-native with json4s-jackson
* `#341 <https://github.com/xitrum-framework/xitrum/issues/341>`_
  Update SLF4J from 1.7.6 to 1.7.7, Logback from 1.1.1 to 1.1.2
* `#343 <https://github.com/xitrum-framework/xitrum/issues/343>`_
  Update commons-lang3 from 3.3 to 3.3.2

3.5:

* `#329 <https://github.com/xitrum-framework/xitrum/issues/329>`_
  Netty's HttpRequestDecoder unable to parse request
* `#333 <https://github.com/xitrum-framework/xitrum/issues/333>`_
  Update Glokka from 1.6 to 1.7 (update Akka from 2.2.3 to 2.3.0)

3.4:

* `#322 <https://github.com/xitrum-framework/xitrum/issues/322>`_
  Handle "Expect: 100-continue" requests
* `#327 <https://github.com/xitrum-framework/xitrum/issues/327>`_
  Fix path exception when autoreloading i18n files on Windows
* `#328 <https://github.com/xitrum-framework/xitrum/issues/328>`_
  Load language lazily

3.3:

* `#275 <https://github.com/xitrum-framework/xitrum/issues/275>`_
  Add a way to change session cookie max age
* `#316 <https://github.com/xitrum-framework/xitrum/issues/316>`_
  SockJS: Change heartbeat from 5s to 25s
* `#317 <https://github.com/xitrum-framework/xitrum/issues/317>`_
  SockJS: Add heartbeat for WebSocket transport
* `#318 <https://github.com/xitrum-framework/xitrum/issues/318>`_
  Fix Netty memory leak at xitrum.util.UrlSafeBase64
* `#323 <https://github.com/xitrum-framework/xitrum/issues/323>`_
  Stop Xitrum at startup on port bind exception
* `#315 <https://github.com/xitrum-framework/xitrum/issues/315>`_
  Update Akka from 2.2.3 to 2.3.0
* `#319 <https://github.com/xitrum-framework/xitrum/issues/319>`_
  Update Twitter Chill from 0.3.5 to 0.3.6
* `#320 <https://github.com/xitrum-framework/xitrum/issues/320>`_
  Update Schwatcher from 0.0.9 to 0.1.0
* `#325 <https://github.com/xitrum-framework/xitrum/issues/325>`_
  Update commons-lang3 from 3.2.1 to 3.3

3.2:

* `#312 <https://github.com/xitrum-framework/xitrum/issues/312>`_
  Fix bug at xitrum.js: when ajaxLoadingImg is not specified, the next element of the submit element is removed
* `#268 <https://github.com/xitrum-framework/xitrum/issues/268>`_
  Add xitrum.util.FileMonitor (requires Java 7)
* `#308 <https://github.com/xitrum-framework/xitrum/issues/308>`_
  Reload .po files updated at runtime
* `#311 <https://github.com/xitrum-framework/xitrum/issues/311>`_
  xitrum.util: Merge Json and SecureUrlSafeBase64 to SeriDeseri
* `#297 <https://github.com/xitrum-framework/xitrum/issues/297>`_
  Disable xitrum.local.LruCache in development mode
* `#306 <https://github.com/xitrum-framework/xitrum/issues/306>`_
  Update Netty from 4.0.16 to 4.0.17

3.1:

* `#292 <https://github.com/xitrum-framework/xitrum/issues/292>`_
  Fix chunked response bug
* `#295 <https://github.com/xitrum-framework/xitrum/issues/295>`_
  Fix bug at responding static file in the zero copy style
* `#288 <https://github.com/xitrum-framework/xitrum/issues/288>`_
  Add Scalive
* `#289 <https://github.com/xitrum-framework/xitrum/issues/289>`_
  Update SLF4J from 1.7.5 to 1.7.6, Logback from 1.0.13 to 1.1.1
* `#300 <https://github.com/xitrum-framework/xitrum/issues/300>`_
  Update Netty from 4.0.15 to 4.0.16
* `#301 <https://github.com/xitrum-framework/xitrum/issues/301>`_
  Update JSON4S from 3.2.6 to 3.2.7
* `#302 <https://github.com/xitrum-framework/xitrum/issues/302>`_
  Update RhinoCoffeeScript from 1.6.3 to 1.7.1
* `#303 <https://github.com/xitrum-framework/xitrum/issues/303>`_
  Update jQuery from 1.10.2 to 1.11.0
* `#304 <https://github.com/xitrum-framework/xitrum/issues/304>`_
  Update Swagger UI from 2.0.4 to 2.0.12

3.0:

* `#151 <https://github.com/xitrum-framework/xitrum/issues/151>`_
  Update Netty from 3.9.0 to 4.0.15
* `#284 <https://github.com/xitrum-framework/xitrum/issues/284>`_
  Fix: xitrum.local.LruCache#putIfAbsent can't overwrite stale cache
* `#265 <https://github.com/xitrum-framework/xitrum/issues/265>`_
  Add FutureAction, rename ActionActor to ActorAction, SockJsActor to SockJsAction, WebSocketActor to WebSocketAction
* `#261 <https://github.com/xitrum-framework/xitrum/issues/261>`_
  Add convenient methods to manipulate routes after they are collected at startup
* `#269 <https://github.com/xitrum-framework/xitrum/issues/269>`_
  Increase akka.logger-startup-timeout from 5s to 30s to avoid Akka's log initialization timeout error at Xitrum startup
* `#263 <https://github.com/xitrum-framework/xitrum/issues/263>`_
  Increase access log level from DEBUG to INFO
* `#259 <https://github.com/xitrum-framework/xitrum/issues/259>`_
  Log WebSocket messages at TRACE level
* `#272 <https://github.com/xitrum-framework/xitrum/issues/272>`_
  Add sockJsCookieNeeded in xitrum.conf so that this option can be set when deploying, depending on deployment environment
* `#74 <https://github.com/xitrum-framework/xitrum/issues/74>`_
  Flash socket policy server can use the same port with HTTP server
* `#274 <https://github.com/xitrum-framework/xitrum/issues/274>`_
  Update commons-lang3 from 3.1 to 3.2.1
* `#282 <https://github.com/xitrum-framework/xitrum/issues/282>`_
  Update Swagger UI from 2.0.3 to 2.0.4

2.15:

* `#77 <https://github.com/xitrum-framework/xitrum/issues/77>`_
  Remove HttpChunkAggregator to avoid memory problem with file upload
* `#258 <https://github.com/xitrum-framework/xitrum/issues/258>`_
  Add config for saving upload files to memory or to disk temporarily
* `#257 <https://github.com/xitrum-framework/xitrum/issues/257>`_
  Add config for directory path to save uploaded files temporarily
* `#256 <https://github.com/xitrum-framework/xitrum/issues/256>`_
  Replace syntax respondView(classOf[OtherAction]) with respondView[OtherAction]()
* `#255 <https://github.com/xitrum-framework/xitrum/issues/255>`_
  Update Netty from 3.8.0 to 3.9.0

2.14:

* `#252 <https://github.com/xitrum-framework/xitrum/issues/252>`_
  Fix cache TTL bug
* `#244 <https://github.com/xitrum-framework/xitrum/issues/244>`_
  Easier way to get request content as string and JSON
* `#245 <https://github.com/xitrum-framework/xitrum/issues/245>`_
  Rename "atJs" to "atJson"
* `#248 <https://github.com/xitrum-framework/xitrum/issues/248>`_
  Collect all routes
* `#249 <https://github.com/xitrum-framework/xitrum/issues/249>`_
  Improve inheritance rule of route annotations
* `#250 <https://github.com/xitrum-framework/xitrum/issues/250>`_
  CORS allow-origin should not be set for domain not specified in xitrum.conf
* `#253 <https://github.com/xitrum-framework/xitrum/issues/253>`_
  Update JSON4S from 3.2.5 to 3.2.6
* `#254 <https://github.com/xitrum-framework/xitrum/issues/254>`_
  Update Swagger UI from 2.0.2 to 2.0.3

2.13:

* `#239 <https://github.com/xitrum-framework/xitrum/issues/239>`_
  Readd feature: One action can have multiple routes
* `#236 <https://github.com/xitrum-framework/xitrum/issues/236>`_
  Remove Swagger related routes when it is disabled
* `#145 <https://github.com/xitrum-framework/xitrum/issues/145>`_
  Split Knockout.js to a separate module
* `#234 <https://github.com/xitrum-framework/xitrum/issues/234>`_
  xitrum.js: Fix bug XITRUM_BASE_URL does not exist
* `#237 <https://github.com/xitrum-framework/xitrum/issues/237>`_
  xitrum.js: Add withBaseUrl
* `#242 <https://github.com/xitrum-framework/xitrum/issues/242>`_
  Add atJs; atJs("key") returns the JSON form of at("key")
* `#238 <https://github.com/xitrum-framework/xitrum/issues/238>`_
  CSRF token can be set in header

2.12:

* `#230 <https://github.com/xitrum-framework/xitrum/issues/230>`_
  Fix bug Routes with trailing '/' are not matched
* `#218 <https://github.com/xitrum-framework/xitrum/issues/218>`_
  Make Hazelcast optional
* `#221 <https://github.com/xitrum-framework/xitrum/issues/221>`_
  xitrum.conf: Improve config of template engine, cache, and session store
* `#159 <https://github.com/xitrum-framework/xitrum/issues/159>`_
  Support dot in route
* `#206 <https://github.com/xitrum-framework/xitrum/issues/206>`_
  Support index.html fallback
* `#209 <https://github.com/xitrum-framework/xitrum/issues/209>`_
  Support automatic OPTIONS request handling for the whole site
* `#71 <https://github.com/xitrum-framework/xitrum/issues/71>`_
  Support automatic CORS request handling for the whole site
* `#204 <https://github.com/xitrum-framework/xitrum/issues/204>`_
  Log Xitrum additional routes separately from app routes
* `#233 <https://github.com/xitrum-framework/xitrum/issues/233>`_
  Add xitrum.Log package object for convenience use
* `#223 <https://github.com/xitrum-framework/xitrum/issues/223>`_
  Rename logger to log
* `#195 <https://github.com/xitrum-framework/xitrum/issues/195>`_
  Rename xitrumCSS to xitrumCss
* `#216 <https://github.com/xitrum-framework/xitrum/issues/216>`_
  Remove package xitrum.mq
* `#211 <https://github.com/xitrum-framework/xitrum/issues/211>`_
  Remove xitrum/routes.js
* `#220 <https://github.com/xitrum-framework/xitrum/issues/220>`_
  Optimize xitrum.util.Loader.bytesFromInputStream
* `#227 <https://github.com/xitrum-framework/xitrum/issues/227>`_
  Optimize xitrum.util.Json by avoid creating formats at every call
* `#196 <https://github.com/xitrum-framework/xitrum/issues/196>`_
  Swagger: Rename Optional<ValueType><ParamType> to Opt<ValueType><ParamType>, ex: OptStringQuery
* `#198 <https://github.com/xitrum-framework/xitrum/issues/198>`_
  Swagger: Add inheritance
* `#199 <https://github.com/xitrum-framework/xitrum/issues/199>`_
  Swagger: Add notes
* `#232 <https://github.com/xitrum-framework/xitrum/issues/232>`_
  Update Netty from 3.7.0 to 3.8.0
* `#214 <https://github.com/xitrum-framework/xitrum/issues/214>`_
  Update Glokka from 1.2 to 1.3 (and akka-slf4j to 2.2.3)
* `#231 <https://github.com/xitrum-framework/xitrum/issues/231>`_
  Update Twitter Chill from 0.3.4 to 0.3.5
* `#200 <https://github.com/xitrum-framework/xitrum/issues/200>`_
  Update Scaposer from 1.2 to 1.3
* `#222 <https://github.com/xitrum-framework/xitrum/issues/222>`_
  Update Knockout.js from 2.3.0 to 3.0.0

2.11: This release contains a noisy debug println, please use 2.12 instead

2.10:

* `#180 <https://github.com/xitrum-framework/xitrum/issues/180>`_
  Swagger: Add option to xitrum.conf to disable Swagger Doc
* `#181 <https://github.com/xitrum-framework/xitrum/issues/181>`_
  Swagger: Improve annotations
* `#182 <https://github.com/xitrum-framework/xitrum/issues/182>`_
  Swagger: Cache result on 1st access
* `#185 <https://github.com/xitrum-framework/xitrum/issues/185>`_
  Swagger: Include Swagger UI
* `#183 <https://github.com/xitrum-framework/xitrum/issues/183>`_
  Fix: Dead actor sends Terminate message to itself
* `#194 <https://github.com/xitrum-framework/xitrum/issues/194>`_
  Fix: Wrong version (2.10.0) of scala-compiler, scala-reflect, and scalap is used
* `#51 <https://github.com/xitrum-framework/xitrum/issues/51>`_
  Add bin/runner.bat for Windows
* `#93 <https://github.com/xitrum-framework/xitrum/issues/93>`_
  Readd indices for xitrum.mq.MessageQueue
* `#179 <https://github.com/xitrum-framework/xitrum/issues/179>`_
  Add route aliasing
* `#189 <https://github.com/xitrum-framework/xitrum/issues/189>`_
  Add option to xitrum.conf to configure request maxInitialLineLength
* `#193 <https://github.com/xitrum-framework/xitrum/issues/193>`_
  Add request.staticFilePathRegex to xitrum.conf
* `#172 <https://github.com/xitrum-framework/xitrum/issues/172>`_
  Replace Java annotations with Scala annotations
* `#191 <https://github.com/xitrum-framework/xitrum/issues/191>`_
  Placeholder in URL can't be empty
* `#132 <https://github.com/xitrum-framework/xitrum/issues/132>`_
  Log network card interface
* `#192 <https://github.com/xitrum-framework/xitrum/issues/192>`_
  Update Twitter Chill from 0.3.2 to 0.3.4

2.9:

* `#169 <https://github.com/xitrum-framework/xitrum/issues/169>`_
  Add Swagger Doc support
* `#173 <https://github.com/xitrum-framework/xitrum/issues/173>`_
  Speed up routing by caching latest matched routes
* `#174 <https://github.com/xitrum-framework/xitrum/issues/174>`_
  Redirect Akka log to SLF4J
* `#175 <https://github.com/xitrum-framework/xitrum/issues/175>`_
  Optimize static file serving by readding /public/ prefix
* `#176 <https://github.com/xitrum-framework/xitrum/issues/176>`_
  Change SkipCSRFCheck to SkipCsrfCheck; same for antiCSRFMeta etc.
* `#177 <https://github.com/xitrum-framework/xitrum/issues/177>`_
  Make routes.cache loading more robust with class name change
* `#168 <https://github.com/xitrum-framework/xitrum/issues/168>`_
  Better support for custom handler
* `#167 <https://github.com/xitrum-framework/xitrum/issues/167>`_
  Update Netty from 3.6.6.Final to 3.7.0.Final
* `#171 <https://github.com/xitrum-framework/xitrum/issues/171>`_
  Update Hazelcast from 3.0.1 to 3.0.2
* `#170 <https://github.com/xitrum-framework/xitrum/issues/170>`_
  Update Glokka from 1.1 to 1.2
* `#178 <https://github.com/xitrum-framework/xitrum/issues/178>`_
  Update Twitter Chill from 0.3.1 to 0.3.2

2.8:

* `#164 <https://github.com/xitrum-framework/xitrum/issues/164>`_
  Fix: publicUrl and resourceUrl return URL containing double slash if reverseProxy.baseUrl in xitrum.conf is set
* `#157 <https://github.com/xitrum-framework/xitrum/issues/157>`_
  Support HTTP method PATCH
* `#161 <https://github.com/xitrum-framework/xitrum/issues/161>`_
  Update SBT from 0.12.4 to 0.13.0
* `#162 <https://github.com/xitrum-framework/xitrum/issues/162>`_
  Update Akka from 2.2.0 to 2.2.1
* `#158 <https://github.com/xitrum-framework/xitrum/issues/158>`_
  Update Hazelcast from 2.6 to 3.0.1
* `#163 <https://github.com/xitrum-framework/xitrum/issues/163>`_
  Update Twitter Chill from 0.3.0 to 0.3.1

2.7:

* `#152 <https://github.com/xitrum-framework/xitrum/issues/152>`_
  Fix: Static files in public directory are not served on Windows
* `#155 <https://github.com/xitrum-framework/xitrum/issues/155>`_
  Fix: Workaround for thread safety problem of Scala reflection API
* `#146 <https://github.com/xitrum-framework/xitrum/issues/146>`_
  Add config option to disable auto gzip
* `#140 <https://github.com/xitrum-framework/xitrum/issues/140>`_
  Update Scala from 2.10.1 to 2.10.2
* `#148 <https://github.com/xitrum-framework/xitrum/issues/148>`_
  Update Akka from 2.1.4 to 2.2.0
* `#142 <https://github.com/xitrum-framework/xitrum/issues/142>`_
  Update Hazelcast from 2.5.1 to 2.6
* `#153 <https://github.com/xitrum-framework/xitrum/issues/153>`_
  Update Twitter Chill from 0.2.3 to 0.3.0
* `#154 <https://github.com/xitrum-framework/xitrum/issues/154>`_
  Update JSON4S from 3.2.4 to 3.2.5
* `#147 <https://github.com/xitrum-framework/xitrum/issues/147>`_
  Update RhinoCoffeeScript to 1.6.3
* `#149 <https://github.com/xitrum-framework/xitrum/issues/149>`_
  Update jQuery from 1.10.0 to 1.10.2
* `#150 <https://github.com/xitrum-framework/xitrum/issues/150>`_
  Update Knockout.js from 2.2.1 to 2.3.0

2.6:

* `#135 <https://github.com/xitrum-framework/xitrum/issues/135>`_
  Fix: Hangs up on cached action
* `#119 <https://github.com/xitrum-framework/xitrum/issues/119>`_
  Close connection after sendUnsupportedWebSocketVersionResponse
* `#139 <https://github.com/xitrum-framework/xitrum/issues/139>`_
  Add UnserializableSessionStore
* `#136 <https://github.com/xitrum-framework/xitrum/issues/136>`_
  Update Netty to 3.6.6.Final
* `#133 <https://github.com/xitrum-framework/xitrum/issues/133>`_
  Update Akka to 2.1.4
* `#137 <https://github.com/xitrum-framework/xitrum/issues/137>`_
  Update Twitter Chill to 0.2.3
* `#138 <https://github.com/xitrum-framework/xitrum/issues/138>`_
  Update jQuery to 1.10.0

2.5:

* `#126 <https://github.com/xitrum-framework/xitrum/issues/126>`_
  Basic authentication causes NullPointerException
* `#127 <https://github.com/xitrum-framework/xitrum/issues/127>`_
  Update Twitter Chill to 0.2.2

2.4:

* `#115 <https://github.com/xitrum-framework/xitrum/issues/115>`_
  Replace Javassist with ASM to reduce the number of dependencies
* `#121 <https://github.com/xitrum-framework/xitrum/issues/121>`_
  Update Twitter Chill to 0.2.1
* `#123 <https://github.com/xitrum-framework/xitrum/issues/123>`_
  Update Hazelcast to 2.5.1

2.3:

* `#120 <https://github.com/xitrum-framework/xitrum/issues/120>`_
  Add javacOptions -source 1.6 to avoid problem when Xitrum is built with
  Java 7 but the projects that use Xitrum are run with Java 6

2.2:

* `#112 <https://github.com/xitrum-framework/xitrum/issues/112>`_
  Add redirectToThis to redirect to the current action
* `#113 <https://github.com/xitrum-framework/xitrum/issues/113>`_
  Rename urlForPublic to publicUrl, urlForResource to resourceUrl
* `#117 <https://github.com/xitrum-framework/xitrum/issues/117>`_
  Apps can be configured to use no template engine
* `#118 <https://github.com/xitrum-framework/xitrum/issues/118>`_
  Route collecting: support getting cache annotation from superclasses

2.1:

* `#110 <https://github.com/xitrum-framework/xitrum/issues/110>`_
  Can't run in production mode because SockJsClassAndOptions
  in routes can't be serialized
* `#111 <https://github.com/xitrum-framework/xitrum/issues/111>`_
  Unify the "execute" method for Action, ActionActor,
  WebSocketActor, and SockJSActor

2.0:
`#104 <https://github.com/xitrum-framework/xitrum/issues/104`_
Annotate your Akka actor to make it accessible from web

* Break actions in controller out to separate classes, each is an Action or
  an ActionActor; your action can be an actor
* Rewrite part of SockJS using ActionActor
* Add connection abort handling for SockJS
* Support "/" in SockJS path prefix
* Support WebSocket binary frame
* Allow starting server with custom Netty ChannelPipelineFactory;
  for an example, see xitrum.handler.DefaultHttpChannelPipelineFactory
* Speed up CoffeeScript compiling by using
  https://github.com/xitrum-framework/RhinoCoffeeScript
* Use Akka log instead of using SLF4J directly
* Ignore trailing slash in URL: treat "articles" and "articles/" the same;
  note that trailing slash is not recommended since browsers do not cache page with such URL
* Update Netty to `3.6.5 <http://netty.io/news/2013/04/09/3-6-5-Final.html>_,
  jQuery Validate to `1.11.1 <http://bassistance.de/2013/03/22/release-validation-plugin-1-11-1/>_,
  Sclasner to 1.6, and xitrum-scalate to 1.1

1.22:

* `#106 <https://github.com/xitrum-framework/xitrum/issues/106>`_
  Update JSON4S to 3.2.4
* `#107 <https://github.com/xitrum-framework/xitrum/issues/107>`_
  Update Netty to 3.6.4

1.21:

* `#103 <https://github.com/xitrum-framework/xitrum/issues/103>`_
  Move Scalate template engine out to a separate project
* `#105 <https://github.com/xitrum-framework/xitrum/issues/105>`_
  Move xitrum-sbt-plugin out to a separate project
* `#100 <https://github.com/xitrum-framework/xitrum/issues/100>`_
  Update JSON4S to 3.2.3
* `#102 <https://github.com/xitrum-framework/xitrum/issues/102>`_
  Update slf4j-api to 1.7.5

1.20:

* `#88 <https://github.com/xitrum-framework/xitrum/issues/88>`_
  Replace JBoss Marshalling with Twitter's Chill
* `#99 <https://github.com/xitrum-framework/xitrum/issues/99>`_
  Use ReflectASM (included by Twitter Chill) to initiate controllers faster
* `#96 <https://github.com/xitrum-framework/xitrum/issues/96>`_
  Rename xitrum.util.Base64 to UrlSafeBase64, SecureBase64 to SecureUrlSafeBase64
* `#97 <https://github.com/xitrum-framework/xitrum/issues/97>`_
  Update SLF4J from 1.7.2 to 1.7.3, Logback from 1.0.9 to 1.0.10
  You should update Logback in your project from 1.0.9 to 1.0.10
* `#98 <https://github.com/xitrum-framework/xitrum/issues/98>`_
  Update Akka from 2.1.1 to 2.1.2

1.19:

* `#91 <https://github.com/xitrum-framework/xitrum/issues/91>`_
  Update Akka from 2.1.0 to 2.1.1
* `#94 <https://github.com/xitrum-framework/xitrum/issues/94>`_
  Improve Secure#unseal

1.18:

* `#87 <https://github.com/xitrum-framework/xitrum/issues/87>`_
  Update Netty from 3.6.2 to 3.6.3
* `#90 <https://github.com/xitrum-framework/xitrum/issues/90>`_
  Update jQuery Validate from 1.10.0 to 1.11.0

1.17:

* Avoid error of instantiating abstract controller while collecting routes

1.16:

* `#86 <https://github.com/xitrum-framework/xitrum/issues/86>`_
  Add forwardTo
* SockJS handler can now access session, request headers etc.
  ``def onOpen(session: immutable Map[String, Any])`` -> ``def onOpen(controller: Controller)``
* Update mime.types from https://github.com/klacke/yaws/blob/master/priv/mime.types
  (text/cache-manifest is added http://www.html5rocks.com/en/tutorials/appcache/beginner/)
* Update jQuery from 1.8.3 to 1.9.1
* Update Knockout.js from 2.2.0 to 2.2.1, its mapping plugin from 2.3.5 to 2.4.1
* Update SBT from 0.12.1 to 0.12.2
  http://www.scala-sbt.org/0.12.2/docs/Community/Changes.html

1.15:

* `Improve SockJS handler interface <https://groups.google.com/group/xitrum-framework/browse_thread/thread/d60dbfb72556aa8c>`_
  ``def onOpen()`` -> ``def onOpen(session: immutable Map[String, Any])``
* `Add more Unicode quoting for SockJS <https://groups.google.com/group/sockjs/msg/ff08ee1a29ac683e>`_
* Make SockJS clusterwise, using Akka Remoting and Hazelcast
  - Add config/application.conf which loads conf/akka.conf and conf/xitrum.conf
  - Add Config.application and rename Config.config to Config.xitrum
  - Add Config.actorSystem named "xitrum"
  - Add xitrum.util.ActorCluster
    `Akka Clustering is currently lacks "single actor instance" feature <http://groups.google.com/group/akka-user/browse_thread/thread/23d6b2851648c1b0>`_
* `Update Netty from 3.6.1 to 3.6.2 <https://netty.io/Blog/Netty+362Final+released>`_
* `Update Hazelcast from 2.4.1 to 2.5 <http://www.hazelcast.com/docs/2.5/manual/multi_html/ch18s04.html>`_
* Update jboss-marshalling from 1.3.16.GA to 1.3.17.GA

See these examples to know how to update your project from 1.14 to 1.15:

* `xitrum-new <https://github.com/xitrum-framework/xitrum-new/commit/98b1af9a006491935f217d46fedda79bd522a3c9>`_
* `xitrum-demos <https://github.com/xitrum-framework/xitrum-demos/commit/e57872a1e7d6d74854b012e45879bf1500029217>`_
* `And xsbt-scalate-generate <https://github.com/xitrum-framework/xitrum-new/commit/ce9d3c777fec2f0e4cacdb5171476791a572f7bc>`_

1.14:

* `Add config for template engine and Scalate template path <http://xitrum-framework.github.io/guide/howto.html#create-your-own-template-engine>`_
* `Add comparison of controllers and actions <https://groups.google.com/group/xitrum-framework/browse_thread/thread/a3469fea17f84ce4>`_
  ``if (currentController == MyController) ...``
  ``if (currentAction == MyController.index) ...``
* `Update Netty from 3.6.0 to 3.6.1 <https://netty.io/Blog/Netty+361Final+out+-+More+SSL+fixes>`_
* `Update Scalate from 1.6.0 to 1.6.1 <http://scalate.fusesource.org/blog/releases/release-1.6.1.html>`_
* Update jboss-marshalling from 1.3.15.GA to 1.3.16.GA

Updating your project from Xitrum 1.13 to 1.14 is
`simple <https://github.com/xitrum-framework/xitrum-new/commit/fea3334ae3c7bedca1a6051d6abc851fb617d4ba>`_.

1.13:

* `Update Scala from 1.9.2 to 2.10.0 <https://groups.google.com/group/akka-user/browse_thread/thread/77e1f134b5134c70>`_
* `Update Akka from 2.0.4 to 2.1.0 <http://doc.akka.io/docs/akka/2.1.0/project/migration-guide-2.0.x-2.1.x.html>`_
* Change Scalate template directory from ``src/main/view/scalate`` to ``src/main/scalate``
* `Add network interface config to config/xitrum.conf <https://github.com/xitrum-framework/xitrum-new/blob/master/config/xitrum.conf>`_
* Add request and response log at TRACE level for easier debugging
* Add log for 500 error in production mode

1.12:

* `Replace Jerkson with JSON4S (Jerkson has been abandoned) <https://github.com/json4s/json4s>`_;
  Note that there are also `other libs <http://wiki.fasterxml.com/JacksonModuleScala>`_
  like Jacks and jackson-module-scala
* `Change <https://github.com/typesafehub/config>`_
  ``config/xitrum.json`` to ``config/xitrum.conf``
* Add methods to render Scalate templates directly from strings (non-file)
* `Add Unicode quoting for SockJS <https://groups.google.com/group/sockjs/msg/9da24b0dde8916e4>`_
* `Update Netty from 3.5.11.Final to 3.6.0.Final <https://netty.io/Blog/Netty+360Final+released+-+Keep+on+moving>`_
* `Update Scalate from 1.5.3 to 1.6.0 <http://scalate.fusesource.org/blog/releases/release-1.6.0.html>`_
* Update Knockout.mapping from 2.3.3 to 2.3.5

1.11:

* Add renderFragment(fragment: String) which renders a fragment of the current controller
* Improve exception handling by catching only Exception, Error and control flow
  Throwable like scala.runtime.NonLocalReturnControl will not be catched.
  An Error is a subclass of Throwable that indicates serious problems that a
  reasonable application should not try to catch.
  http://docs.oracle.com/javase/6/docs/api/java/lang/Error.html
* Rename hazelcast_cluster_or_super_client.xml to hazelcast_cluster_or_lite_member.xml
  From Hazelcast 2.0, SuperClient is renamed to LiteMember to avoid confusion:
  http://www.hazelcast.com/docs/2.4/manual/multi_html/ch18s04.html
* Update Hazelcast from 2.4 to 2.4.1
  This version fixes Out of Memory Error every few days:
  http://groups.google.com/group/hazelcast/browse_thread/thread/31f69d0eb89440b5/1d9ce430deffb575

1.10:

* `Improve <http://xitrum-framework.github.io/guide/scopes.html#cookie>`_
  cookie API to requestCookies and responseCookies.
  Only cookies in responseCookies `will be sent to browsers <http://groups.google.com/group/xitrum-framework/browse_thread/thread/dbb7a8e638120b09>`_.
* `Remove <http://groups.google.com/group/xitrum-framework/browse_thread/thread/310c61f501e0bba8>`_
  ``resetSession`` method. To reset session, call ``session.clear()``.
* `Support Scalate Mustache template <http://xitrum-framework.github.io/guide/controller_action_view.html#scalate>`_
* `Fix bug with sending the last chunk in chunked mode for SockJS <http://groups.google.com/group/sockjs/msg/d66e2978249b5f26>`_
* Fix URL to jquery.validate-1.10.0/localization/messages_<lang>.js
  (was "jquery.validate-1.9.0/..." instead)
* Update SBT from 0.12.0 to 0.12.1
* `Update Netty from 3.5.10.Final to 3.5.11.Final <https://netty.io/Blog/Netty+3511Final+is+out>`_
* `Update Javassist from 3.16.1-GA to 3.17.1-GA (works with Java 6; 3.17.0-GA requires Java 7) <https://issues.jboss.org/browse/JASSIST/fixforversion/12320652>`_
* `Update Sclasner from 1.1 to 1.2 <http://groups.google.com/group/xitrum-framework/browse_thread/thread/f1ede2c56bf27e75>`_
* Update jQuery from 1.8.2 to 1.8.3

1.9.10:

* Revert Javassist back to 3.16.1-GA because 3.17.0-GA
  `requires Java 7 <http://groups.google.com/group/xitrum-framework/browse_thread/thread/fe3c1be6857ff1a3>`_

1.9.9:

* Only decode request body only when the request method is POST, PUT, or PATCH
  http://groups.google.com/group/xitrum-framework/browse_thread/thread/f343f7bc92edb39c
* SockJS:
  - Minor bug fixes and improvements
  - Allow setting options websocket = false and cookie_needed = true
    http://groups.google.com/group/sockjs/browse_thread/thread/392cd07c4a75400b
* `Update Netty from 3.5.9.Final to 3.5.10.Final <https://netty.io/Blog/Netty+3510Final+-+Get+it+while+it+is+hot>`_
* `Update Akka from 2.0.3 to 2.0.4 <http://groups.google.com/group/akka-user/browse_thread/thread/4da3849a0a5e4163>`_
* `Update Javassist from 3.16.1-GA to 3.17.0-GA <https://issues.jboss.org/browse/JASSIST/fixforversion/12319159>`_
* `Update Knockout.js from 2.1.0 to 2.2.0, Knockout.mapping from 2.3.2 to 2.3.3 <http://blog.stevensanderson.com/2012/10/29/knockout-2-2-0-released/>`_
* `Update SockJS JS library from 0.3.3 to 0.3.4 <http://groups.google.com/group/sockjs/browse_thread/thread/e4b2c1871601f8ae>`_

1.9.8:

* Add support for
  `SockJS <https://github.com/sockjs/sockjs-client>`_
  `0.3.3 <https://github.com/sockjs/sockjs-protocol>`_;
  SockJS now works on a single server, next version will add cluster mode
* Add `respondEventSource(data: String, event: String = "message") <http://dev.w3.org/html5/eventsource/>`_
* Add clientMustRevalidateStaticFiles option to config/xitrum.json
  You can force browsers to always send request to server to revalidate cache before using
* Add Akka 2.0.3 as a dependency, for use in SockJS
* Add `JBoss Marshalling <http://www.jboss.org/jbossmarshalling>`_ as dependency,
  for faster/smaller session cookie storing/restoring.
  It features the advanced River serialization protocol which is far more
  space- and computation-efficient. It can be found in use within the excellent
  Infinispan project as well as finding heavy use in JBoss.
* Add `Scalate Markdown <http://scalate.fusesource.org/documentation/jade.html>`_
  as `dependency <http://groups.google.com/group/xitrum-framework/browse_thread/thread/262176aa8e875940>`_
* `Add Appache Commons Lang as dependency, to use its StringEscapeUtils in jsEscape <http://commons.apache.org/lang/api-release/org/apache/commons/lang3/StringEscapeUtils.html>`_
  * Fix bug at remoteIp when reverseProxy is enabled in config/xitrum.json
* Remove double quotes around the result of jsEscape
* Remove xitrum.comet.CometController
  Rename xitrum.comet.Comet to xitrum.sockjs.MessageQueue
* Try GZIP compressing session cookie bigger than 4KB (limit of most browsers)
  Display error log when session cookie is still bigger than 4KB after compressing
* Rename routes.sclasner to routes.cache
* `Update Netty from 3.5.8.Final to 3.5.9.Final <https://netty.io/Blog/Netty+359Final+is+out>`_
* Update Hazelcast from 2.3.1 to 2.4
* `Update Scaposer from 1.1 to 1.2 <https://github.com/xitrum-framework/scaposer/pull/3>`_

1.9.7:

* Fix problem when HTTPS is used and static file is bigger than
  "smallStaticFileSizeInKB" in config/xitrum.js:
  https://github.com/xitrum-framework/xitrum/issues/64
* Fix iOS6 Safari POST caching problem by automatically setting "Cache-Control"
  header to "no-cache" for POST response:
  http://www.mnot.net/blog/2012/09/24/caching_POST
  http://stackoverflow.com/questions/12506897/is-safari-on-ios-6-caching-ajax-results
* Support HEAD (automatically handled by Xitrum as GET), OPTIONS, and PATCH
* In your controller, to prevent client-side caching, call setNoClientCache();
  It will set "Cache-Control" header to:
  "no-store, no-cache, must-revalidate, max-age=0"
* Other new methods:
  isTablet: returns true if the request is from tablet
  setClientCacheAggressively()
  respondHtml("<html>...</html>")
  respondJsonText("[1, 2, 3]")
  respondJsonP(List(1, 2, 3), "myFunction")
  respondJsonPText("[1, 2, 3]", "myFunction")
* Responding methods (respondXXX, redirectTo) now returns
  org.jboss.netty.channel.ChannelFuture
  http://static.netty.io/3.5/api/org/jboss/netty/channel/ChannelFuture.html
  You can use it to perform actions when the response has actually been sent
* Update Netty from 3.5.7.Final to 3.5.8.Final:
  https://netty.io/Blog/Netty+358Final+release+-+A+%22must%22+upgrade
* Update slf4j-api from 1.6.6 to 1.7.1
* Update jQuery from 1.7.2 to 1.8.2
* Update jQuery Validate from 1.9.0 to 1.10.0:
  http://bassistance.de/2012/09/07/release-validation-plugin-1-10-0/
* Update Knockout.js from 2.0.0 to 2.1.0
* Update Knockout.mapping from 2.0.3 to 2.3.2

1.9.6:

* Support WebSocket for iPhone Safari when running on port 80 (HTTP) or 443 (HTTPS);
  previous Xitrum versions work OK for iPhone Safari when non-standard ports are used
* Improve i18n feature, e.g. add autosetLanguage method:
  http://xitrum-framework.github.io/guide/i18n.html

1.9.5:

* You should upgrade to Xitrum 1.9.5 as soon as possible because there's a bug
  with file upload in Netty 3.5.5.Final:
  https://github.com/netty/netty/issues/569
* Update Netty from 3.5.5.Final to 3.5.7.Final:
  https://netty.io/Blog/Netty+357Final+released
  https://netty.io/Blog/Netty+356Final+released
* Update Hazelcast from 2.2 to 2.3.1:
  http://www.hazelcast.com/docs/2.3/manual/multi_html/ch18s04.html

1.9.4:

* Fix bug that causes non-empty 304 Not Modified response to be sent.
  This buggy response will be sent when respondFile is used in your controllers.
  You should upgrade to Xitrum 1.9.4 as soon as possible because Chrome cannot
  handle this response properly (but Firefox, Safari, and even IE can).

1.9.3:

* Update Netty from 3.5.3.Final to 3.5.5.Final:
  https://netty.io/Blog/Netty+355Final+released
  https://netty.io/Blog/Netty+354Final+out+now
* From Netty 3.5.5.Final, to delete cookie when the browser closes windows,
  set max age to Integer.MIN_VALUE, not -1 as before
* Xitrum now can serve flash socket policy file:
  http://www.adobe.com/devnet/flashplayer/articles/socket_policy_files.html
  http://www.lightsphere.com/dev/articles/flash_socket_policy.html
* config/xitrum.json is slightly improved:
  https://github.com/xitrum-framework/xitrum-new/blob/master/config/xitrum.json
* "Cache-Control" header will be automatically set to "no-cache"
  for chunked response, e.g. when response.setChunked(true) is called
  Note that "Pragma" will not be sent because this header is for request, not response:
  http://palizine.plynt.com/issues/2008Jul/cache-control-attributes/
* Add:
    respondBinary(channelBuffer: ChannelBuffer)
    respondWebSocket(channelBuffer: ChannelBuffer)
* Avoid duplicate routes when deleting and recreating routes.sclasner
* Remove </meta>, </input>, and </link> at:
  <meta name="csrf-token" content="d1d50807-5a0a-4d42-830a-a01a3628f2c8"></meta>
  <input name="csrf-token" type="hidden" value="d1d50807-5a0a-4d42-830a-a01a3628f2c8"></input>
  <link type="text/css" media="all" rel="stylesheet" href="/resources/public/xitrum/xitrum.css?DMtin-KdUgKxwWIyHp3E4A"></link>
  You should use
    != antiCSRFMeta
    != xitrumCSS
    != antiCSRFInput
  instead of:
    = antiCSRFMeta
    = xitrumCSS
    = antiCSRFInput

1.9.2:

* Add global basic authentication to protect the whole site.
  This is usually needed when putting an unfinished site to the Internet.
  See https://github.com/xitrum-framework/xitrum-new/blob/master/config/xitrum.json
* Improve access log to include remote IP
* Support "Range" request to static files
  Xitrum can now be used to serve interleaved MP4 movies
  (tested on iOS, Safari, Firefox, and Chrome)
  For simplicity only these specs are supported:
  bytes=123-456
  bytes=123-
* Update SBT from 0.11.3-2 to 0.12.0
* Update Hazelcast from 2.1.2 to 2.2:
  http://hazelcast.com/docs/2.2/manual/multi_html/ch18s04.html
  hazelcast_java_client.json is changed to hazelcast_java_client.properties
  See https://github.com/xitrum-framework/xitrum-new/blob/master/config/hazelcast_java_client.properties
* Update Scaposer from 1.0 to 1.1:
  https://github.com/xitrum-framework/scaposer/pull/2

1.9.1:

* Support "Range" request to static files
  Xitrum can now be used to serve interleaved MP4 movies
  (tested on iOS and Firefox)
  For simplicity only this spec is supported:
  bytes=123-456
* Update Netty from 3.5.0.Final to 3.5.3.Final:
  https://netty.io/Blog/Announcing+the+new+web+site+and+Netty+351Final
  https://netty.io/Blog/Netty+352Final+is+out
  https://netty.io/Blog/Say+Hello+to+Netty+353Final+
* Update Rhino from 1.7R3 to 1.7R4:
  https://developer.mozilla.org/en/New_in_Rhino_1.7R4
* Update SBT from 0.11.2 to 0.11.3-2

1.9:

* Use Netty 3.5.0.Final instead of 4.0.0.Alpha1-SNAPSHOT for file upload
  To upgrade, in your project in most cases just replace
  io.netty.xxx with org.jboss.netty.xxx
  Ex:
    Old code: import io.netty.util.CharsetUtil.UTF_8
    New code: import org.jboss.netty.util.CharsetUtil.UTF_8
* basicAuthenticate now works as documented:
  http://xitrum-framework.github.io/guide/howto.html#basic-authentication
* Add I18n#tf, tcf, tnf, tcnf for formatted string;
  Standard placeholders %d, %s etc. work, but if the formatted string contains
  many of them, their order should not be changed
* sbt publish-local can be run easily by anyone, not only core developers
* Update Hazelcast from 2.0.2 to 2.1.2
* Update SLF4J from 1.6.4 to 1.6.6

1.8.7:

* Add build for Scala 2.9.2
* To get URL to WebSocket action:
  ControllerObject.action.webSocketAbsoluteUrl
* Update Hazelcast from 2.0.1 to 2.0.2
* Fix #63
  https://github.com/xitrum-framework/xitrum/issues/63

1.8.6:

* Fix WebSocket bug introduced in Xitrum 1.8.4, now WebSocket frame receiving works again
* Cleaner API for WebSocket:
  http://xitrum-framework.github.io/guide/async.html#websocket
* To easily put JS fragments to Scalate views, jsAtBottom is split to jsDefaults and jsForView.
  jsDefaults containing jQuery, Knockout.js etc. should be put at layout's <head>.
  jsForView containing JS fragments added by jsAddToView should be put at layout's bottom.
* Add JS utility: xitrum.appendAndScroll, see example:
  https://github.com/xitrum-framework/xitrum-demos/blob/master/src/main/view/scalate/quickstart/controller/CometChat/index.jade

1.8.5:

* Regex can be used in routes to specify requirements:
  def show = GET("/articles/:id<[0-9]+>") { ... }
* Update Hazelcast from 2.0 to 2.0.1
* Update Javassist from 3.15.0-GA to 3.16.1-GA

1.8.4:

* Update Hazelcast from 1.9.4.8 to 2.0
* Remove ExecutionHandler.
  If your action performs a blocking operation that
  takes long time or accesses a resource which is not CPU-bound business logic
  such as DB access, you should do it in the async style (better) or use a separate
  thread pool to avoid unwanted hiccup during I/O because an I/O thread cannot
  perform I/O until your action returns the control to the I/O thread.
* For each connection, requests will be processed one by one.
  From Mongrel2: http://mongrel2.org/static/book-finalch6.html
  Where problems come in is with pipe-lined requests, meaning a browser sends a
  bunch of requests in a big blast, then hangs out for all the responses. This
  was such a horrible stupid idea that pretty much everone gets it wrong and
  doesn't support it fully, if at all. The reason is it's much too easy to blast
  a server with a ton of request, wait a bit so they hit proxied backends, and
  then close the socket. The web server and the backends are now screwed having
  to handle these requests which will go nowhere.

1.8.3:

* Fix `#60 <https://github.com/xitrum-framework/xitrum/issues/60>`_

1.8.2:

* Filters now have "only" and "except"
  http://xitrum-framework.github.io/guide/filter.html
* Optimize routing by using methods instead of vals
  http://xitrum-framework.github.io/guide/controller_action_view.html

1.8:

* Add Scalate back, with precompilation
* Remove annotations and put related actions into controller
* Remove server-side auto-validation for postback requests and
  rewrite validators so that they can be used for any kind of requests;
  You can still use postback APIs on browser side, postback requests are easier
  to debug with Firebug or Chrome, because parameter names are no longer encrypted
* Improve data-after, now you can write
    data-after="$('#chatInput').val('')"
  instead of
    data-after="function () { $('#chatInput').val('') }"
* Add Knockout.js
* Add Scala delimited continuation
  See:
    http://www.earldouglas.com/continuation-based-web-workflows-part-two/
    http://stackoverflow.com/questions/6062003/event-listeners-with-scala-continuations
    http://jim-mcbeath.blogspot.com/2010/08/delimited-continuations.html
* Update jQuery from 1.6.4 to 1.7.1
* Fix urlForPublic bug, resulted URL now has the leading "/"
* Improve Quickstart: https://github.com/xitrum-framework/xitrum-quickstart

1.7:

* WebSocket, see:
  http://xitrum-framework.github.io/guide/async.html#websocket
  http://netty.io/blog/2011/11/17/
* Make postback tag attributes HTML5 standards-compliant:
  You must change:
    postback to data-postback
    after    to data-after
    confirum to data-confirm
* Expose APIs for data encryption so that application developers may use
  xitrum.util.{Secure, SecureBase64, SeriDeseri}
  See http://xitrum-framework.github.io/guide/howto.html#encrypt-data
* Update Hazelcast from 1.9.4.4 to 1.9.4.5

1.6:

* Redesign filters to be typesafe
* Add after and around filters
* Add Loader.json and use JSON for config files
  (.json files should be used instead of .properties files)

1.5.3:

* Close connection for HTTP 1.0 clients. This allows Xitrum to be run behind
  Nginx without having to set proxy_buffering to off.
  Nginx talks HTTP/1.1 to the browser and HTTP/1.0 to the backend server, and
  it needs the backend server to close connection after finishing sending
  response to it. See http://wiki.nginx.org/HttpProxyModule.
* Fix the bug that causes connection to be closed immediately when sending file
  from action using renderFile to HTTP 1.0 clients.

1.5.2:

* Add xitrum.Config.root (like Rails.root) and fix #47
* Better API for basic authentication
* renderFile now can work with absolute path on Windows
* Exit if there's error on startup
* Update SLF4J from 1.6.2 to 1.6.4 (and Logback from 0.9.30 from to 1.0.0)
* Update Hazelcast from 1.9.4.3 to 1.9.4.4

1.5.1:

* Update Jerkson from 0.4.2 to 0.5.0

1.5:

* Static public files now do not have to have /public prefix, this is convenient
  but dynamic content perfomance decreases a little
* Applications can handle 404 and 500 errors by their own instead of using
  the default 404.html and 500.html
* Change validation syntax to allow validators to change HTML element:
  <input type="text" name={validate("username", Required)} /> now becomes
  {<input type="text" name="username" /> :: Required}

  <input type="text" name={validate("param", MaxLength(32), MyValidator)} /> now becomes
  {<input type="text" name="param" /> :: MaxLength(32) :: MyValidator}

  <input type="text" name={validate("no_need_to_validate")} /> now becomes
  {<input type="text" name="no_need_to_validate" /> :: Validated}
* Implement more validators: Email, EqualTo, Min, Max, Range, RangeLength
* Update jQuery Validation from 1.8.1 to 1.9.0:
  https://github.com/jzaefferer/jquery-validation/blob/master/changelog.txt
* Textual responses now include charset in Content-Type header:
  http://code.google.com/speed/page-speed/docs/rendering.html#SpecifyCharsetEarly
* Fix bug header not found: Content-Length for 404 and 500 content

1.4:

* Fix bug at setting Expires header for static content, it is now one year
  later instead of 17 days later
* Set Expires header for resources in classpath
* HTTPS (see config/xitrum.properties)
  KeyStore Explorer is a good tool to create self-signed keystore:
  http://www.lazgosoftware.com/kse/index.html

1.3:

* Update
    Hazelcast: 1.9.4.2 -> 1.9.4.3
    Jerkson:   0.4.1   -> 0.4.2
    SBT:       0.10.1  -> 0.11.0
* Improve performance, based on Google's best practices:
  http://code.google.com/speed/page-speed/docs/rules_intro.html
  Simple benchmark (please use httperf, ab is broken) on
  MacBook Pro 2 GHz Core i7, 8 GB memory:
    Static file:                 ~11000 req/s
    Resource file in classpath:  ~11000 req/s
    Dynamic HTML without layout: ~7000  req/s
    Dynamic HTML with layout:    ~7000  req/s
* Only gzip when client specifies "gzip" in Accept-Encoding request header

1.2:

* Conditional GET using ETag, see:
  http://stevesouders.com/hpws/rules.php
* Fix for radio: SecureBase64.encrypt always returns same output for same input
  <input type="radio" name={validate("light")} value="red" />
  <input type="radio" name={validate("light")} value="yellow" />
  <input type="radio" name={validate("light")} value="green" />

1.1:

* i18n using Scaposer
* Faster route collecting on startup using Sclasner
