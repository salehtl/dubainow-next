// Combine screenshots side by side: swift tools/contact.swift out.jpg a.jpg b.jpg …
import AppKit
let args = CommandLine.arguments
let out = args[1]
let images = args.dropFirst(2).compactMap { NSImage(contentsOfFile: $0) }
let h: CGFloat = 1000
let widths = images.map { $0.size.width * h / $0.size.height }
let total = widths.reduce(0, +) + CGFloat(max(images.count - 1, 0)) * 16
let canvas = NSImage(size: NSSize(width: total, height: h))
canvas.lockFocus()
NSColor.white.setFill(); NSRect(x: 0, y: 0, width: total, height: h).fill()
var x: CGFloat = 0
for (img, w) in zip(images, widths) { img.draw(in: NSRect(x: x, y: 0, width: w, height: h)); x += w + 16 }
canvas.unlockFocus()
let rep = NSBitmapImageRep(data: canvas.tiffRepresentation!)!
try! rep.representation(using: .jpeg, properties: [.compressionFactor: 0.8])!.write(to: URL(fileURLWithPath: out))
