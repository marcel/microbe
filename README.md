Microbe - Quick but reliable micro-benchmarking
------------------------------------------------

Writing micro-benchmarks on the JVM is notoriously tricky. HotSpot
optimizations on naively written benchmarks may mislead you into forming
conclusions about the performance characteristics of two alternate
implementations.

[Links about microbenchmarks on the JVM]
https://wikis.oracle.com/display/HotSpotInternals/MicroBenchmarks
http://www.ibm.com/developerworks/java/library/j-jtp02225/
http://www.ibm.com/developerworks/java/library/j-jtp12214/

[List of best practices]
* Warm up the JVM to trigger compilation before the benchmark measurement is
 performed.
* Don't throw away computed results. Capture results and hold on to them so the compiler doesn't to optimize them
 out because they are never used.
* Loop unrolling could mean 1,000,000 iterations could be optimized into a
 single, coalesced iteration.
* others...

[Include something like:
  Goals of Microbe:
  * Everything is configurable, but you don't have to configure anything
  * You shouldn't have to worry about HotSpot invalidating your benchmark
  * Make it as trivial as possible to benchmark code
  * While you shouldn't have to configure anything in the simple case, you should be
    able to configure everything.
  * Take care of ensuring benchmarks are as realistic as possible despite aggressive runtime
    compilation.
]

= Configuration Defaults
Each section of code to benchmark will be executed for 30 seconds, with 4 warm up runs.

= The simplest benchmark
Benchmark { codeToBenchmark }

= The most sophisticated benchmark

Benchmark { bench =>
  bench.name             = "Sequence linear scans"
  bench.iterations       = Seq(1000, 10000, 10000)
  bench.warmUps          = 4
  bench.report           = new CampfireReporter
  bench.printDiagnostics = true
  bench.silenceWarnings  = true // default is false
  bench.quick = true // This prints a warning to STDERR
  bench.warmUps = 0 // This prints a warning to STDERR w/ a recommendation if warmUps is less than 3
  bench.duration = 1000 // This prints a warning to STDERR w/ a recommendation if it's less than 10 seconds

  bench("one") {
    println("ran 1")
  }

  bench("two") {
    println("ran 2")
  }
}

= From Java

// Code example: new SummarizedReport(new Benchmark

= .sbtrc

Add this to your .sbtrc file so you can create a Microbe benchmark in any sbt session:
TODO