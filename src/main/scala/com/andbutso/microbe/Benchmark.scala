package com.andbutso.microbe

// TODO Full documentation
object Benchmark {
  // TODO For timed runs given an estimate on how long the benchmark will take
  final val DefaultRunDuration = 30000 // 30,000 milliseconds
  final val DefaultWarmUpRuns  = 4
  /**
   * The least fussy way to perform a benchmark is by just providing a description
   * and the chuck of code you want to benchmark. For example:
   *
   * {{{
   *   val numbers = (1 to 10000) toList
   *   SummarizedReport(
   *     Benchmark("toIndexedSeq") { numbers.toIndexedSeq },
   *     Benchmark("toSet") { numbers.toSet },
   *     Benchmark("toArray") { numbers.toArray }
   *   )
   *
   * }}}
   *
   * The benchmark will run the code for a desirable amount of time.
   *
   * @param name describing the code being benchmarked.
   * @param code the chuck of code you want to benchmark.
   */
  final def apply[V](name: String)(code: => V): Benchmark = {
    this(name, DefaultRunDuration, DefaultWarmUpRuns)(code)
  }

  def apply[V](
    name: String,
    iterations: Long,
    warmUpRuns: Int
  )(
    code: => V
  ): Benchmark = {
    val mainRun = IteratedRun(iterations, name, () => code)

    val warmUps = mkWarmUpRuns[IteratedRun[V], V](mainRun, warmUpRuns)

    run(name, warmUps :+ mainRun)
  }

  final def apply[V](
    name: String,
    durationInMilliseconds: Int,
    warmUpRuns: Int
  )(
    code: => V
  ): Benchmark = {
    val mainRun = TimedRun(durationInMilliseconds, name, () => code)

    val warmUps = mkWarmUpRuns[TimedRun[V], V](mainRun, warmUpRuns)

    run(name, warmUps :+ mainRun)
  }

  final private[this] def run(name: String, runs: Seq[Run[_]]) = {
    val results = runs map { run =>
      System.gc()

      run()
    }

    Benchmark(name, results)
  }

  final private[this] def mkWarmUpRuns[R <: Run[T], T](mainRun: R, numberOfRuns: Int) = {
    val warmUpName = (number: Int) => "Warmup #" + number + " for '" + mainRun.name + "'"
    val runs = 1 to numberOfRuns

    mainRun match {
      case iteratedRun: IteratedRun[_] =>
        runs map { n => iteratedRun.copy(name = warmUpName(n)) }
      case timedRun: TimedRun[_] =>
        runs map { n => timedRun.copy(name = warmUpName(n)) }
      case _ =>
        throw new IllegalArgumentException("Unsupported run type")
    }
  }
}

case class Benchmark(
  name: String,
  results: Seq[Result]
) extends Ordered[Benchmark] {
  val warmUps = results.init
  val mainRun = results.last

  final def compare(otherBenchmark: Benchmark) = {
    mainRun.compare(otherBenchmark.mainRun)
  }
}

trait Run[V] {
  def apply(): Result = {
    announceStart()

    val runtime = Runtime.getRuntime

    val startState = RuntimeState(runtime)
    val runValue   = apply(new RunValue)
    val endState   = RuntimeState(runtime)

    announceCompletion()

    Result(this, startState, endState, runValue.value)
  }

  def announceStart() {
    println("Starting " + name)
  }

  def announceCompletion() {
    println("Completed " + name)
  }

  def name: String
  def iterations: Long
  def code: () => V

  def apply(runValue: RunValue): RunValue
}

/**
 * TODO Document
 */
class RunValue {
  var value: Any = null
}

case class IteratedRun[V](
  iterations: Long,
  name: String,
  code: () => V
) extends Run[V] {
  val iterationRange = 1L to iterations

  final def apply(runValue: RunValue) = {
    iterationRange foreach { _ =>
      runValue.value = code()
    }

    runValue
  }
}

case class TimedRun[V](
  durationInMilliseconds: Long,
  name: String,
  code: () => V
) extends Run[V] {
  var iterations = 0L

  final def apply(runValue: RunValue) = {
    val timeToStop = System.currentTimeMillis + durationInMilliseconds

    while (System.currentTimeMillis < timeToStop) {
      iterations += 1
      runValue.value = code()
    }

    runValue
  }
}

case class RuntimeState(
  totalMemory: Long,
  freeMemory: Long,
  time: Double = System.nanoTime.toDouble
) {
  val consumedMemory = totalMemory - freeMemory

  val memoryConsumptionAsPercentOfTotalMemory = {
    consumedMemory.toFloat / totalMemory * 100
  }
}

object RuntimeState {
  def apply(runtime: Runtime): RuntimeState = {
    RuntimeState(runtime.totalMemory, runtime.freeMemory)
  }
}

case class Result(
  run: Run[_],
  startState: RuntimeState,
  endState: RuntimeState,
  value: Any
) extends Ordered[Result] {
  val iterations = run.iterations
  val elapsed    = endState.time - startState.time

  val millisecondsElapsed      = elapsed / 1000000
  val iterationsPerMillisecond = iterations / millisecondsElapsed
  val millisecondsPerIteration = millisecondsElapsed / iterations
  val nanosecondsPerIteration  = elapsed / iterations

  val memoryConsumption         = endState.consumedMemory - startState.consumedMemory
  val memoryConsumptionIncrease = endState.consumedMemory.toFloat / startState.consumedMemory

  final def compare(that: Result) = {
    that.iterationsPerMillisecond.compare(iterationsPerMillisecond)
  }
}

object text {
  import Console._

  def underlined(string: String) = {
    UNDERLINED + string + RESET
  }

  def bold(string: String) = {
    BOLD + string + RESET
  }
}

case class StdoutReport(result: Result) {
  val valuePreview = {
    val value = result.value.toString

    if (value.size > 50) {
      value.take(50) + "..."
    } else {
      value
    }
  }

  def apply() {
    emit(text.bold("Report for '" + result.run.name + "'"))
    emit("result", valuePreview)
    emit("iterations", result.iterations)
    emit(text.underlined("Time:"))
    emit(f(result.millisecondsElapsed), "milliseconds")
    if (result.nanosecondsPerIteration < 1000000) { // Don't bother if it's more than a millisecond
      emit(f(result.nanosecondsPerIteration), "ns per iteration")
    }
    emit(f(result.millisecondsPerIteration, 5), "ms per iteration")
    emit(f(result.iterationsPerMillisecond, 5), "iterations per ms")
    emit(text.underlined("Memory:"))
    emit(StorageUnit(result.memoryConsumption).toHuman, "increase during run")
    emit(StorageUnit(result.memoryConsumption / result.iterations).toHuman, "~ generated per iteration")
    emit(f(result.memoryConsumptionIncrease) + "x increase")
    emit(f(result.startState.memoryConsumptionAsPercentOfTotalMemory) + "% of total at start")
    emit(f(result.endState.memoryConsumptionAsPercentOfTotalMemory) + "% of total at end")
    emit("")
  }

  private[this] def emit(output: Any*) {
    val line = output map { _.toString } mkString(" ")
    println(line)
  }

  private[this] def f(value: AnyVal, precision: Int = 2) = {
    value.formatted("%." + precision + "f")
  }
}

/**
 * TODO Provide docs showing how to define a custom Report and how to configure your
 * benchmark to use your custom report.
 */
trait Report[V] extends (Seq[Benchmark] => V)

class SummarizedReport extends Report[Unit] {
  def apply(benchmarks: Seq[Benchmark]) {
    val sorted  = benchmarks.sorted
    val fastest = sorted.head
    for (index <- 0 until sorted.size) {
      val number    = "#" + (index + 1)
      val benchmark = sorted(index)

      val label = if (benchmark != fastest) {
        val timesFaster = benchmark.mainRun.millisecondsPerIteration /
                          fastest.mainRun.millisecondsPerIteration

        number + ": (" + timesFaster.formatted("%.2f") + " times slower)"
      } else { number + ":" }

      if (benchmarks.size > 1) println(label)

      StdoutReport(benchmark.results.last)()
    }
  }
}

case class StorageUnit(bytes: Long) {
  def toHuman = {
    val prefix      = "KMGT"
    var prefixIndex = -1
    var display     = bytes.toDouble
    while (display.abs > 1126.0) {
      prefixIndex += 1
      display /= 1024.0
    }
    if (prefixIndex < 0) {
      "%d B".format(bytes)
    } else {
      "%.1f %cB".format(display, prefix.charAt(prefixIndex))
    }
  }
}

class Builder {
  var name = ""
  /**
   * The number of warm up runs that will be performed before benchmark measurements are taken.
   *
   */
  var warmupUps = 4

  /**
   *
   */
  var report: Report = new SummarizedReport

  val benchmarks = collection.mutable.ArrayBuffer[Benchmark]()

  def apply[R](name: String)(code: => R) = {
    benchmarks += Benchmark(name)(code)
    benchmarks.toIndexedSeq
  }
}

object Bench {
  def apply[V](definition: => Builder => V) = {
    val builder = new Builder
    definition(builder)

    val report = new SummarizedReport
    report(builder.benchmarks)
  }
}

object Main extends App {
  val number = 10000

  Bench { bench =>
    bench.name = "Adding parallel and serial collections"

    bench("1 to 100000 doubled in parallel") {
      (1 to number).par map { _ * 2 }
    }

    bench("1 to 100000 doubled serially") {
      (1 to number) map { _ * 2 }
    }
  }
}