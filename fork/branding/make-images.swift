// Copyright 2026 ReStudio contributors. Use of this source code is governed by the Apache 2.0 license.
//
// Makes the ReStudio images from restudio.svg and restudio_16.svg. Runs on macOS only (AppKit, iconutil).
// Usage: swift fork/branding/make-images.swift
// Output: fork/branding/images/{linux,mac,win} (for the installers) and the splash and icon resources in
// community-resources/resources (for the IDE).
import AppKit

let branding = URL(fileURLWithPath: CommandLine.arguments[0]).deletingLastPathComponent().standardizedFileURL
let root = branding.deletingLastPathComponent().deletingLastPathComponent()
let images = branding.appendingPathComponent("images")
let resources = root.appendingPathComponent("community-resources/resources")
let fileManager = FileManager.default

let icon = NSImage(contentsOf: branding.appendingPathComponent("restudio.svg"))!
let smallIcon = NSImage(contentsOf: branding.appendingPathComponent("restudio_16.svg"))!
let background = NSColor(srgbRed: 0.106, green: 0.114, blue: 0.161, alpha: 1)
let accent = NSColor(srgbRed: 0.055, green: 0.647, blue: 0.643, alpha: 1)

func bitmap(_ width: Int, _ height: Int, _ draw: (NSRect) -> Void) -> NSBitmapImageRep {
  let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: width, pixelsHigh: height, bitsPerSample: 8,
                             samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                             bytesPerRow: 0, bitsPerPixel: 0)!
  NSGraphicsContext.saveGraphicsState()
  NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
  NSGraphicsContext.current!.imageInterpolation = .high
  draw(NSRect(x: 0, y: 0, width: width, height: height))
  NSGraphicsContext.restoreGraphicsState()
  return rep
}

func iconBitmap(_ size: Int) -> NSBitmapImageRep {
  bitmap(size, size) { rect in (size <= 24 ? smallIcon : icon).draw(in: rect) }
}

func png(_ rep: NSBitmapImageRep) -> Data { rep.representation(using: .png, properties: [:])! }

func write(_ data: Data, _ url: URL) {
  try! fileManager.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
  try! data.write(to: url)
  print(url.path)
}

/// A 24-bit BMP file without alpha, as NSIS needs it.
func bmp(_ rep: NSBitmapImageRep) -> Data {
  let width = rep.pixelsWide, height = rep.pixelsHigh
  let rowSize = (width * 3 + 3) & ~3
  var data = Data()
  func append<T: FixedWidthInteger>(_ value: T) { withUnsafeBytes(of: value.littleEndian) { data.append(contentsOf: $0) } }
  data.append(contentsOf: [0x42, 0x4D])
  append(UInt32(54 + rowSize * height)); append(UInt32(0)); append(UInt32(54))
  append(UInt32(40)); append(Int32(width)); append(Int32(height)); append(UInt16(1)); append(UInt16(24))
  append(UInt32(0)); append(UInt32(rowSize * height)); append(Int32(2834)); append(Int32(2834)); append(UInt32(0)); append(UInt32(0))
  for y in (0..<height).reversed() {
    var row = Data()
    for x in 0..<width {
      let color = rep.colorAt(x: x, y: y)!.usingColorSpace(.sRGB)!
      row.append(contentsOf: [UInt8(color.blueComponent * 255), UInt8(color.greenComponent * 255), UInt8(color.redComponent * 255)])
    }
    row.append(contentsOf: [UInt8](repeating: 0, count: rowSize - width * 3))
    data.append(row)
  }
  return data
}

/// A 32-bit DIB for an ICO entry: BGRA rows from the bottom, then an empty AND mask.
func dib(_ rep: NSBitmapImageRep) -> Data {
  let size = rep.pixelsWide
  let maskRowSize = ((size + 31) / 32) * 4
  var data = Data()
  func append<T: FixedWidthInteger>(_ value: T) { withUnsafeBytes(of: value.littleEndian) { data.append(contentsOf: $0) } }
  append(UInt32(40)); append(Int32(size)); append(Int32(size * 2)); append(UInt16(1)); append(UInt16(32))
  append(UInt32(0)); append(UInt32(size * size * 4 + maskRowSize * size)); append(Int32(0)); append(Int32(0)); append(UInt32(0)); append(UInt32(0))
  for y in (0..<size).reversed() {
    for x in 0..<size {
      let color = rep.colorAt(x: x, y: y)!.usingColorSpace(.sRGB)!
      let alpha = color.alphaComponent
      // DIB icons use straight (not premultiplied) alpha.
      data.append(contentsOf: [UInt8(color.blueComponent * 255), UInt8(color.greenComponent * 255),
                               UInt8(color.redComponent * 255), UInt8(alpha * 255)])
    }
  }
  data.append(contentsOf: [UInt8](repeating: 0, count: maskRowSize * size))
  return data
}

/// An ICO file. The 256 px image is a PNG, the smaller images are DIBs.
func ico(_ sizes: [Int]) -> Data {
  let pngs = sizes.map { $0 >= 256 ? png(iconBitmap($0)) : dib(iconBitmap($0)) }
  var data = Data()
  func append<T: FixedWidthInteger>(_ value: T) { withUnsafeBytes(of: value.littleEndian) { data.append(contentsOf: $0) } }
  append(UInt16(0)); append(UInt16(1)); append(UInt16(sizes.count))
  var offset = 6 + 16 * sizes.count
  for (size, image) in zip(sizes, pngs) {
    data.append(UInt8(size >= 256 ? 0 : size)); data.append(UInt8(size >= 256 ? 0 : size))
    data.append(0); data.append(0); append(UInt16(1)); append(UInt16(32))
    append(UInt32(image.count)); append(UInt32(offset))
    offset += image.count
  }
  pngs.forEach { data.append($0) }
  return data
}

func text(_ string: String, size: CGFloat, weight: NSFont.Weight, color: NSColor, at point: NSPoint) {
  let attributes: [NSAttributedString.Key: Any] = [.font: NSFont.systemFont(ofSize: size, weight: weight), .foregroundColor: color]
  NSAttributedString(string: string, attributes: attributes).draw(at: point)
}

/// The splash screen. The IDE draws the progress bar at its bottom.
func splash(_ scale: Int) -> NSBitmapImageRep {
  bitmap(640 * scale, 400 * scale) { rect in
    let s = CGFloat(scale)
    background.setFill(); rect.fill()
    accent.setFill(); NSRect(x: 0, y: 0, width: rect.width, height: 4 * s).fill()
    icon.draw(in: NSRect(x: 48 * s, y: 216 * s, width: 112 * s, height: 112 * s))
    text("ReStudio", size: 64 * s, weight: .bold, color: .white, at: NSPoint(x: 184 * s, y: 236 * s))
    text("An open-source IDE for many languages", size: 20 * s, weight: .regular,
         color: NSColor(white: 0.75, alpha: 1), at: NSPoint(x: 48 * s, y: 150 * s))
    text("Built on the IntelliJ Platform open-source code. Not a JetBrains product.", size: 13 * s, weight: .regular,
         color: NSColor(white: 0.55, alpha: 1), at: NSPoint(x: 48 * s, y: 40 * s))
  }
}

func banner(_ width: Int, _ height: Int, iconSize: Int, vertical: Bool) -> NSBitmapImageRep {
  bitmap(width, height) { rect in
    (vertical ? background : NSColor.white).setFill(); rect.fill()
    let x = (width - iconSize) / 2
    let y = vertical ? height - iconSize - 40 : (height - iconSize) / 2
    icon.draw(in: NSRect(x: vertical ? x : width - iconSize - 8, y: y, width: iconSize, height: iconSize))
    if vertical {
      text("ReStudio", size: 24, weight: .bold, color: .white, at: NSPoint(x: 20, y: CGFloat(y - 48)))
    }
  }
}

// IDE resources.
write(png(splash(1)), resources.appendingPathComponent("restudio_logo.png"))
write(png(splash(2)), resources.appendingPathComponent("restudio_logo@2x.png"))
for name in ["restudio.svg", "restudio_16.svg"] {
  let target = resources.appendingPathComponent(name)
  try? fileManager.removeItem(at: target)
  try! fileManager.copyItem(at: branding.appendingPathComponent(name), to: target)
  print(target.path)
}

// Linux.
write(png(iconBitmap(128)), images.appendingPathComponent("linux/product_128.png"))

// Windows.
write(ico([16, 20, 24, 32, 40, 48, 64, 256]), images.appendingPathComponent("win/product.ico"))
write(ico([16, 24, 32, 48]), images.appendingPathComponent("win/install.ico"))
write(ico([16, 24, 32, 48]), images.appendingPathComponent("win/uninstall.ico"))
write(bmp(banner(150, 57, iconSize: 41, vertical: false)), images.appendingPathComponent("win/headerlogo.bmp"))
write(bmp(banner(164, 314, iconSize: 84, vertical: true)), images.appendingPathComponent("win/logo.bmp"))

// macOS.
let dmgBackground = bitmap(455, 296) { rect in
  NSColor(white: 0.97, alpha: 1).setFill(); rect.fill()
  text("Drag ReStudio into Applications", size: 15, weight: .medium, color: NSColor(white: 0.35, alpha: 1), at: NSPoint(x: 116, y: 40))
}
write(dmgBackground.representation(using: .tiff, properties: [:])!, images.appendingPathComponent("mac/dmg_background.tiff"))

let iconset = fileManager.temporaryDirectory.appendingPathComponent("restudio.iconset")
try? fileManager.removeItem(at: iconset)
try! fileManager.createDirectory(at: iconset, withIntermediateDirectories: true)
for size in [16, 32, 128, 256, 512] {
  try! png(iconBitmap(size)).write(to: iconset.appendingPathComponent("icon_\(size)x\(size).png"))
  try! png(iconBitmap(size * 2)).write(to: iconset.appendingPathComponent("icon_\(size)x\(size)@2x.png"))
}
let icns = images.appendingPathComponent("mac/product.icns")
let iconutil = Process()
iconutil.executableURL = URL(fileURLWithPath: "/usr/bin/iconutil")
iconutil.arguments = ["-c", "icns", iconset.path, "-o", icns.path]
try! iconutil.run()
iconutil.waitUntilExit()
precondition(iconutil.terminationStatus == 0, "iconutil failed")
print(icns.path)
