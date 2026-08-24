// swift-tools-version:5.9
import PackageDescription

// Links directly against the Kotlin/Native debug framework built by
// `./gradlew :core:linkDebugFrameworkMacosArm64` — a plain .framework rather
// than an .xcframework, since this only targets the local Apple Silicon build
// (no need for XCFramework multi-arch merging for local dev/testing).
let frameworkSearchPath = "../core/build/bin/macosArm64/debugFramework"

let package = Package(
    name: "NstkMac",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(
            name: "NstkMac",
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
                    "-Xlinker", "Info.plist"
                ])
            ]
        )
    ]
)
