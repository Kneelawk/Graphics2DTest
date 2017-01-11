package org.kneelawk.graphics2dtest

import java.awt.image.BufferedImage
import java.awt.GradientPaint
import java.awt.Color
import java.awt.RenderingHints
import scala.util.Random
import javax.imageio.ImageIO
import java.io.File
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Graphics2DTest {
  val WIDTH = 2000
  val HEIGHT = 2000
  val SHAPE = Array((0, 0), (20, 0), (20, 20), (10, 30), (0, 20))

  def rotate(value: Float, amount: Float, min: Float, max: Float): Float = {
    val size = max - min
    if (size > 0) {
      var quot = (value + amount - min) / size
      quot -= quot.floor
      return quot * size + min
    } else if (size == 0) {
      return min
    } else {
      throw new RuntimeException("Invalid rotate range")
    }
  }

  def cap(value: Float, amount: Float, min: Float, max: Float): Float = {
    val nval = value + amount
    if (nval < min) return min
    else if (nval > max) return max
    return nval
  }

  def main(args: Array[String]) {
    val rand = new Random
    def rfloat(min: Float, max: Float) = rand.nextFloat() * (max - min) + min

    val outDir = new File("output")
    if (!outDir.exists()) outDir.mkdirs()

    var offset = 0;
    while (new File(outDir, f"out$offset%04d.png").exists()) offset += 1
    println(s"Offset: $offset")

    val generators = for (j <- 0 until 20) yield Future {
      val image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
      val g = image.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      val bg1Hue = rand.nextFloat()
      val bg1Sat = 1f
      val bg1Bri = rfloat(0.6f, 1f)
      val bg1 = Color.getHSBColor(bg1Hue, bg1Sat, bg1Bri)

      val bg2Hue = rotate(bg1Hue, if (rand.nextBoolean()) rfloat(0.15f, 0.25f) else rfloat(-0.25f, -0.15f), 0f, 1f)
      val bg2Sat = bg1Sat
      val bg2Bri = cap(bg1Bri, rfloat(-0.2f, 0.2f), 0f, 1f)
      val bg2 = Color.getHSBColor(bg2Hue, bg2Sat, bg2Bri)

      g.setPaint(new GradientPaint(WIDTH / 2, 0, bg1, WIDTH / 2, HEIGHT, bg2))
      g.fillRect(0, 0, WIDTH, HEIGHT)

      val locs = (0 until (WIDTH * HEIGHT / 1000) map (i => (rfloat(-20, WIDTH).toInt, rfloat(-30, HEIGHT).toInt))).sortWith(_._2 < _._2)

      for (loc <- locs) {
        val sbg1Hue = rotate(bg1Hue, if (rand.nextBoolean()) rfloat(0.02f, 0.08f) else rfloat(-0.08f, -0.02f), 0f, 1f)
        val sbg1Sat = bg1Sat
        val sbg1Bri = rotate(bg1Bri, rfloat(-0.2f, 0.2f), 0f, 1f)
        val sbg1 = Color.getHSBColor(sbg1Hue, sbg1Sat, sbg1Bri)

        val sbg2Hue = rotate(bg2Hue, if (rand.nextBoolean()) rfloat(0.02f, 0.08f) else rfloat(-0.08f, -0.02f), 0f, 1f)
        val sbg2Sat = bg2Sat
        val sbg2Bri = rotate(bg2Bri, rfloat(-0.2f, 0.2f), 0f, 1f)
        val sbg2 = Color.getHSBColor(sbg2Hue, sbg2Sat, sbg2Bri)

        g.setPaint(new GradientPaint(WIDTH / 2, 0, sbg1, WIDTH / 2, HEIGHT, sbg2))

        val x = loc._1
        val y = loc._2

        g.fillPolygon(SHAPE.map(_._1 + x), SHAPE.map(_._2 + y), SHAPE.length)
        g.drawLine(x + 10, y + 28, x + 10, HEIGHT)
      }

      ImageIO.write(image, "png", new File(outDir, f"out${j + offset}%04d.png"))
      println(s"Finished $j")
    }

    println("Threads started.")

    Await.result(Future.sequence(generators), Duration.Inf)

    println("All done.")
  }
}