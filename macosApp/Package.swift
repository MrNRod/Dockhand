// swift-tools-version:5.9
import PackageDescription
import Foundation

// Links directly against the Kotlin/Native debug framework built by
// `./gradlew :core:linkDebugFrameworkMacosArm64` — a plain .framework rather
// than an .xcframework, since this only targets the local Apple Silicon build
// (no need for XCFramework multi-arch merging for local dev/testing).
//
// Absolute paths, computed from this manifest's own location on disk, rather
// than relative ones: Xcode's build system doesn't resolve `../`-relative
// unsafeFlags paths the same way a plain `swift build` from Terminal does,
// which is why opening Package.swift directly in Xcode failed with
// "No such module 'core'" even though the CLI build worked fine.
let packageDir = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
let frameworkSearchPath = packageDir
    .appendingPathComponent("../core/build/bin/macosArm64/debugFramework")
    .standardized.path
let infoPlistPath = packageDir.appendingPathComponent("Info.plist").path

let package = Package(
    name: "NstkMac",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(
            name: "NstkMac",
            resources: [
                .copy("Resources/AppIcon.png")
            ],
            swiftSettings: [
                .unsafeFlags(["-Fsystem", frameworkSearchPath])
            ],
            linkerSettings: [
                .unsafeFlags([
                    "-F", frameworkSearchPath,
                    "-framework", "core",
                    "-Xlinker", "-rpath",
                    "-Xlinker", frameworkSearchPath,
                    // Embeds Info.plist into the binary's __TEXT,__info_plist section —
                    // without this, `swift run`'s unbundled executable has no bundle
                    // identity at all, so macOS doesn't give it a Dock icon or menu bar.
                    "-Xlinker", "-sectcreate",
                    "-Xlinker", "__TEXT",
                    "-Xlinker", "__info_plist",
                    "-Xlinker", infoPlistPath
                ])
            ]
        )
    ]
)
