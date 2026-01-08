import java.io.File

val termuxPackagesDir = File("termux-packages-master")

fun main(args: Array<String>) {
    runCommand("curl -sL https://github.com/termux/termux-packages/archive/master.tar.gz | tar -xz")
    buildPackages(listOf("bash"), args.first())
}

private fun buildPackages(packages: List<String>, arch: String) {
    require(termuxPackagesDir.isDirectory)
    runCommand(
        "./scripts/run-docker.sh ./build-package.sh -a $arch ${packages.joinToString(" ")}",
        "build-package",
        termuxPackagesDir
    )
}

private fun runCommand(
    command: String,
    label: String? = null,
    directory: File? = null
) = ProcessBuilder()
    .command("sh", "-c", command)
    .directory(directory)
    .redirectErrorStream(true)
    .start().apply {
        inputStream.bufferedReader().forEachLine {
            println("$label: $it")
        }
        waitFor().let {
            if (it != 0) error("$command got exit code: $it")
        }
    }