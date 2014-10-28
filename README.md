### EC2 Consistency Check

Small tool to check consistency of EC2 file system operations. Writes random files with random content in parallel and check it later.
Ideally content should match 100% of times.

## Command Line Arguments

```scala
  private val CLI = new scopt.OptionParser[Args]("EC2 File System Consistency Check") {
    opt[Int]('t', "threads") action { (x, c) => c.copy(nThreads = x)} text "Number of worker Threads"
    opt[Int]('n', "writes")  action { (x, c) => c.copy(n = x)}        text "Number of file writes to perform"
    opt[String]('d', "dir")  action { (x, c) => c.copy(dir = x)}      text "Path to directory used to create files"
    opt[Int]('l', "length")  action { (x, c) => c.copy(length = x)}   text "Content length to write in each file"
    opt[Int]('s', "sleep")   action { (x, c) => c.copy(sleep = x)}    text "Time to sleep after write operation in ms"
  }
```

## Building

In the root directory run:

    sbt assembly

The application fat jars will be placed in:
  - `target/scala-2.10/ec2-consistency-check-app.jar`


## Running

First you need to run `assembly` in sbt and then run java cmd

    java -cp ec2-consistency-check-app.jar com.github.ezhulenev.RunEC2ConsistencyCheck