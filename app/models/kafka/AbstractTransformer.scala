package models.kafka

import scala.concurrent.duration.Duration

import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.{ Transformer, TransformerSupplier }
import org.apache.kafka.streams.processor.ProcessorContext
import org.slf4j.LoggerFactory

/**
 * Preserves the context in a field, provides scheduling.
 */
abstract class AbstractTransformer[K, V, NK, NV](interval: Duration = Duration.Zero)
    extends Transformer[K, V, KeyValue[NK, NV]] {

  protected val log = LoggerFactory.getLogger(getClass)
  
  protected var context: ProcessorContext = null

  final def init(ctx: ProcessorContext) = {
    this.context = ctx
    if (interval > Duration.Zero)
      ctx.schedule(interval)
    postInit(ctx)
    log.info(s"[${this.toString}] transformer initialized")
  }
  
  def postInit(ctx: ProcessorContext) = {}
  
  def punctuate(ts: Long) = null

  def close() = {}
}

/**
 * Transformer factory.
 */
abstract class AbstractTransformerSupplier[K, V, NK, NV] extends TransformerSupplier[K, V, KeyValue[NK, NV]]