scalacOptions ++= Seq("-unchecked", "-deprecation")

javacOptions ++= Seq(
  "-Xlint:unchecked", "-server", "-XX:+AggressiveOpts", 
  "-XX:-CITime", "-XX:+UseCompressedOops", "-Xmx2G", "-XX:MaxPermSize=256m",
  "-XX:CICompilerCount=1", "-Xbatch", "-verbose:gc"
)

parallelExecution := true

name := "microbe"

organization := "com.andbutso"

version := "1.0"

scalaVersion := "2.9.2"

initialCommands := "import com.andbutso.microbe._"
