import edu.wpi.first.toolchain.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.platform.base.PlatformContainer
import org.gradle.nativeplatform.platform.NativePlatform
import groovy.transform.InheritConstructors
import groovy.transform.CompileStatic

@InheritConstructors
@CompileStatic
class TinkerboardGcc extends GccToolChain {
  @Override
  String getTypeName() {
    return "TinkerboardGcc"
  }
}

@CompileStatic
class TinkerboardPlugin implements Plugin<Project> {
  static final String toolchainVersion = "6.3.0"
  static final String toolchainTag = "20181229.2"

  static final String tinkerboardToolchainName = "tinkerboard"
  static final String tinkerboardPlatform = "linuxtinkerboard"

  String composeTool(String toolName) {
    String exeSuffix = OperatingSystem.current().isWindows() ? ".exe" : "";
    return "arm-tinkerboard-linux-gnueabihf-" + toolName + exeSuffix
  }

  void apply(Project proj) {
    proj.pluginManager.apply(TinkerboardRules)

    ToolchainDescriptor desc = new ToolchainDescriptor(tinkerboardToolchainName, "tinkerboardGcc", new ToolchainRegistrar<TinkerboardGcc>(TinkerboardGcc.class, proj))
    desc.setToolchainPlatforms(tinkerboardPlatform)
    desc.setOptional(true)
    desc.getDiscoverers().all { ToolchainDiscoverer disc ->
      disc.configureVersions(toolchainVersion, toolchainVersion)
    }

    proj.extensions.getByType(ToolchainExtension.class).add(desc)

    def installLoc = new File(ToolchainPlugin.pluginHome(), "tinkerboard")
    desc.getDiscoverers().add(new ToolchainDiscoverer("Project", installLoc, this.&composeTool))
    desc.getDiscoverers().addAll(ToolchainDiscoverer.forSystemPath(proj, this.&composeTool))

    desc.getInstallers().add(installerFor(OperatingSystem.LINUX, installLoc));
    desc.getInstallers().add(installerFor(OperatingSystem.WINDOWS, installLoc));
    desc.getInstallers().add(installerFor(OperatingSystem.MAC_OS, installLoc));
  }

  AbstractToolchainInstaller installerFor(OperatingSystem os, File installDir) {
    URL url = new URL("https://github.com/JacisNonsense/TinkerboardGCC/releases/download/" + toolchainTag + "/" + toolchainRemoteFile())
    return new DefaultToolchainInstaller(os, url, installDir, "Tinkerboard")
  }

  String toolchainRemoteFile() {
    String platformId = OperatingSystem.current().isWindows() ? "Windows" : OperatingSystem.current().isMacOsX() ? "Mac" : "Linux"
    String ext = OperatingSystem.current().isWindows() ? "zip" : "tar.gz"
    return "Tinkerboard-${platformId}-Toolchain-${toolchainVersion}.${ext}"
  }
}

@CompileStatic
class TinkerboardRules extends RuleSource {
  @Mutate
  void addPlatform(final PlatformContainer platforms) {
    def tinkerboard = platforms.maybeCreate(TinkerboardPlugin.tinkerboardPlatform, NativePlatform.class)
    tinkerboard.architecture("arm")
    tinkerboard.operatingSystem("linux")
  }
}