import java.io.File

val termuxPackagesDir = File("termux-packages-master")

fun main(args: List<String>) {
    runCommand("curl -sL https://github.com/termux/termux-packages/archive/master.tar.gz | tar -xz")
    setupEnvironment()
    $$"""
        # these need to be unset a second time again for ./build-package.sh
        unset NDK ANDROID_HOME
        ./build-package.sh -I -C -a "${{ matrix.target_arch }}" "${packages[@]}"
    """.trimIndent()

    buildPackages(listOf("bash"), args.firstOrNull() ?: "aarch64")
}

private fun setupEnvironment() {
    require(termuxPackagesDir.isDirectory)
    if (System.getenv("CI") == null) {
        println("Ignoring setupEnvironment to not mess up your system.")
        return
    }
    runCommand(
        $$"""
            ./scripts/setup-ubuntu.sh
            # need to unset these for setup-android-sdk.sh.
            unset NDK ANDROID_HOME
            ./scripts/setup-android-sdk.sh
            rm -f ${HOME}/lib/ndk-*.zip ${HOME}/lib/sdk-*.zip
            sudo apt install ninja-build
            ./scripts/free-space.sh
        """.trimIndent(),
        termuxPackagesDir
    )
}

private fun buildPackages(packages: List<String>, arch: String) {
    require(termuxPackagesDir.isDirectory)
    runCommand(
        """
            # these need to be unset a second time again for ./build-package.sh
            unset NDK ANDROID_HOME
            ./build-package.sh -a $arch ${packages.joinToString(" ")}
        """.trimIndent(),
        termuxPackagesDir
    )
}

private fun runCommand(
    command: String,
    directory: File? = null
) = ProcessBuilder()
    .command("sh", "-c", command)
    .directory(directory)
    .redirectErrorStream(true)
    .start().apply {
        inputStream.bufferedReader().forEachLine {
            println("${command.substringBefore(' ')}: $it")
        }
        waitFor().let {
            if (it != 0) error("$command got exit code: $it")
        }
    }