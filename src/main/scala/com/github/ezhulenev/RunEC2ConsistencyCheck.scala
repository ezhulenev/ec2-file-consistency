package com.github.ezhulenev

import java.io._
import java.nio.file.{Path, Paths}
import java.util
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

import com.google.common.io.ByteStreams

import scala.util.Random


object RunEC2ConsistencyCheck extends App {

  protected case class Args(nThreads: Int = 10, n: Int = 1000, length: Int = 1000, sleep: Int = 0, dir: String = System.getProperty("java.io.tmpdir"))

  private val CLI = new scopt.OptionParser[Args]("EC2 File System Consistency Check") {
    opt[Int]('t', "threads") action { (x, c) => c.copy(nThreads = x)} text "Number of worker Threads"
    opt[Int]('n', "writes")  action { (x, c) => c.copy(n = x)}        text "Number of file writes to perform"
    opt[String]('d', "dir")  action { (x, c) => c.copy(dir = x)}      text "Path to directory used to create files"
    opt[Int]('l', "length")  action { (x, c) => c.copy(length = x)}   text "Content length to write in each file"
    opt[Int]('s', "sleep")   action { (x, c) => c.copy(sleep = x)}    text "Time to sleep after write operation in ms"
  }

  val writes = new AtomicLong(0)
  val successRead = new AtomicLong(0)
  val failedRead = new AtomicLong(0)

  // Try parse input arguments and run App
  CLI.parse(args, Args()) map { case Args(nThreads, n, length, sleep, dir) =>
    println(s"Run EC2 Consistency check. File writes n: $n. Threads: $nThreads. Temp directory: $dir. Content length: $length. Sleep after write: $sleep")

    val random = new Random()

    val writePool = Executors.newFixedThreadPool(nThreads)
    val readPool = Executors.newFixedThreadPool(nThreads)

    for (i <- 1 to n) {
      val filename = random.nextString(10)
      val bytes = new Array[Byte](length)
      random.nextBytes(bytes)
      val latch = new CountDownLatch(1)

      // Submit tasks to pool
      writePool.execute(new WriteContent(Paths.get(dir), filename, bytes, latch, sleep))
      readPool.execute(new ReadContent(Paths.get(dir), filename, bytes, latch))
    }

    // Shutdown pools
    writePool.shutdown()
    readPool.shutdown()
    writePool.awaitTermination(1, TimeUnit.MINUTES)
    readPool.awaitTermination(1, TimeUnit.MINUTES)

    println(s"Test finished. Finished writes: ${writes.get}. " +
      s"Writes: ${successRead.get()}. " +
      s"Failed reads: ${failedRead.get()}. " +
      s"Fail rate: ${failedRead.get().toDouble / writes.get}")

    System.exit(0)
  }


  class WriteContent(dir: Path, filename: String, bytes: Array[Byte], latch: CountDownLatch, sleep: Int) extends Runnable {
    override def run(): Unit = {
      val file = dir.resolve(filename).toFile
      val is = new ByteArrayInputStream(bytes)
      val fos = new BufferedOutputStream(new FileOutputStream(file))
      try {
        ByteStreams.copy(new ByteArrayInputStream(bytes), fos)
        writes.incrementAndGet()
      } finally {
        is.close()
        fos.close()
        if (sleep > 0) Thread.sleep(sleep)
        latch.countDown()
      }
    }
  }

  class ReadContent(dir: Path, filename: String, bytes: Array[Byte], latch: CountDownLatch) extends Runnable {
    override def run(): Unit = {
      latch.await()

      val file = dir.resolve(filename).toFile
      val is = new BufferedInputStream(new FileInputStream(file))
      val baos = new ByteArrayOutputStream()
      val os = new BufferedOutputStream(baos)

      try {
        ByteStreams.copy(is, os)
      } finally {
        is.close()
        os.close()
      }

      val fileContent = baos.toByteArray
      if (!util.Arrays.equals(bytes, fileContent)) {
        println(s"WARN: File content doesn't match with expected. Read size: ${fileContent.size}. Expected size: ${bytes.size}")
        failedRead.incrementAndGet()
      } else {
        //println(s"Content 100% match")
        successRead.incrementAndGet()
      }
    }
  }

}