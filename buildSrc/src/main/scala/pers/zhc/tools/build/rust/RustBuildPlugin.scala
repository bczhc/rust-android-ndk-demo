package pers.zhc.tools.build.rust

import org.gradle.api.provider.Property
import org.gradle.api.{GradleException, Plugin, Project, Task}
import pers.zhc.tools.build.FileUtils
import pers.zhc.tools.build.rust.RustBuildPlugin.RustBuildPluginExtension

import java.io.{File, InputStream}
import scala.util.control.Breaks.{break, breakable}

class RustBuildPlugin extends Plugin[Project] {
  var appProjectDir: Option[File] = None
  var extension: Option[RustBuildPluginExtension] = None
  var project: Option[Project] = None

  override def apply(project: Project): Unit = {
    project.task("compileReleaseRust", { task: Task =>

      val extension = project.getExtensions.create("rustBuild", classOf[RustBuildPluginExtension])
      this.extension = Some(extension)

      task.doLast { _: Task =>
        checkRequiredExtension(extension)
        configureToolchain()
        compileReleaseRust()
      }
      ()
    })

    val appProject = project.getRootProject.findProject("app")
    require(appProject != null)
    appProjectDir = Some(appProject.getProjectDir)

    this.project = Some(project)
  }

  def printStream(is: InputStream): Unit = {
    val c = new Array[Byte](1)
    var readLen = 0
    breakable {
      while (true) {
        readLen = is.read(c)
        if (readLen == -1) {
          break()
        }
        Console.out.write(c, 0, readLen)
        Console.out.flush()
      }
    }
  }

  def executeProgram(runtime: Runtime, cmd: Array[String], dir: Option[File]): Int = {
    val process = runtime.exec(cmd, null, dir.orNull)
    val stdout = process.getInputStream
    val stderr = process.getErrorStream

    val t1 = new Thread {
      printStream(stdout)
    }
    val t2 = new Thread {
      printStream(stderr)
    }
    t1.start()
    t2.start()
    t1.join()
    t2.join()

    stdout.close()
    stderr.close()
    process.waitFor()
    process.exitValue()
  }

  def compileReleaseRust(): Unit = {
    assert(appProjectDir.isDefined)
    assert(extension.isDefined)

    val architecture = getTarget
    val rustTarget = architecture

    val rustProjectDir = getRustProjectDir
    val jniLibsDir = getJniLibsDir

    val command = Array(
      "cargo", "build",
      "--release", "--target", rustTarget
    )

    val runtime = Runtime.getRuntime
    val status = executeProgram(runtime, command, Some(rustProjectDir))
    if (status != 0) {
      throw new GradleException("Failed to run program: exits with non-zero return value")
    }

    val outputDir = new File(rustProjectDir, s"target/$architecture/release")
    assert(outputDir.exists())

    if (!jniLibsDir.exists()) {
      require(jniLibsDir.mkdir())
    }

    val listSoFiles: (File, File => Unit) => Unit = { (dir, func) =>
      dir.listFiles().foreach { f: File =>
        if (f.isFile && FileUtils.getFileExtensionName(f).getOrElse("") == "so") {
          func(f)
        }
      }
    }

    val outputSoDir = {
      val d = new File(jniLibsDir, getAndroidAbi.toString)
      if (!d.exists()) {
        require(d.mkdirs())
      }
      d
    }
    listSoFiles(outputDir, { file =>
      val dest = new File(outputSoDir, file.getName)
      FileUtils.copyFile(file, dest)
      assert(dest.exists())
    })
  }

  def checkRequiredExtension(extension: RustBuildPluginExtension): Unit = {
    require(extension.getAndroidApi.isPresent)
    require(extension.getTarget.isPresent)
  }

  def getRustProjectDir: File = Option(extension.get.getRustProjectDir.getOrNull()) match {
    case Some(value) =>
      new File(value)
    case None =>
      new File(appProjectDir.get, "src/main/jni/rust")
  }

  def getAndroidApi: Int = extension.get.getAndroidApi.get()

  def getArchitecture: String = extension.get.getTarget.get()

  def getTarget: String = s"$getArchitecture-linux-android"

  def getCargoConfigFile: File = new File(getRustProjectDir, ".cargo/config")

  def getAndroidAbi: AndroidAbi = AndroidAbi.from(getTarget)

  def getJniLibsDir: File = Option(extension.get.getOutputDir.getOrNull()) match {
    case Some(value) =>
      new File(value)
    case None =>
      new File(appProjectDir.get, "jniLibs")
  }

  def configureToolchain(): Unit = {
    require(extension.get.getAndroidNdkDir.isPresent)
    val androidNdkDir = extension.get.getAndroidNdkDir.get()
    val toolchain = new Toolchain(new File(androidNdkDir), getAndroidApi, getArchitecture)
    val configString = ToolchainUtils.generateCargoConfig(getTarget, toolchain)
    FileUtils.writeFile({
      val configFile = getCargoConfigFile
      val parent = getCargoConfigFile.getParentFile
      if (!parent.exists()) {
        require(parent.mkdir())
      }
      configFile
    }, configString)
  }
}

object RustBuildPlugin {
  trait RustBuildPluginExtension {
    def getOutputDir: Property[String]

    def getRustProjectDir: Property[String]

    def getAndroidNdkDir: Property[String]

    def getAndroidApi: Property[Int]

    // TODO: multi-target handling
    def getTarget: Property[String]
  }
}
