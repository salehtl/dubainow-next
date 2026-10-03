import AppKit
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

let W = 1440.0, H = 900.0, S = 2.0
let pw = Int(W * S), ph = Int(H * S)
let cs = CGColorSpace(name: CGColorSpace.sRGB)!
let ctx = CGContext(data: nil, width: pw, height: ph, bitsPerComponent: 8, bytesPerRow: 0, space: cs, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
// flip to top-left origin, logical units
ctx.translateBy(x: 0, y: CGFloat(ph)); ctx.scaleBy(x: CGFloat(S), y: -CGFloat(S))

func rgb(_ hex: UInt32, _ a: CGFloat = 1) -> CGColor {
  CGColor(srgbRed: CGFloat((hex >> 16) & 0xff)/255, green: CGFloat((hex >> 8) & 0xff)/255, blue: CGFloat(hex & 0xff)/255, alpha: a)
}
// 1. base gradient
let base = CGGradient(colorsSpace: cs, colors: [rgb(0x0B1240), rgb(0x1B2C9E), rgb(0x2340E6), rgb(0xB83C9E), rgb(0xFF5E3A), rgb(0xFFB547)] as CFArray, locations: [0.0, 0.25, 0.46, 0.66, 0.82, 1.0])!
ctx.drawLinearGradient(base, start: CGPoint(x: 0, y: 0), end: CGPoint(x: W, y: H), options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
// 2. soft glow blobs
func blob(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ c: UInt32, _ a: CGFloat) {
  let g = CGGradient(colorsSpace: cs, colors: [rgb(c, a), rgb(c, 0)] as CFArray, locations: [0, 1])!
  ctx.drawRadialGradient(g, startCenter: CGPoint(x: cx, y: cy), startRadius: 0, endCenter: CGPoint(x: cx, y: cy), endRadius: r, options: [])
}
blob(260, 820, 520, 0x19B8C8, 0.55)
blob(1180, 120, 420, 0x6E5BFF, 0.45)
blob(1110, 300, 420, 0xFF8A4C, 0.55)
// 3. sun disc
let sun = CGGradient(colorsSpace: cs, colors: [rgb(0xFFF1C7), rgb(0xFFD27A), rgb(0xFF8A4C)] as CFArray, locations: [0, 0.55, 1])!
ctx.saveGState()
ctx.addEllipse(in: CGRect(x: 1110-210, y: 300-210, width: 420, height: 420)); ctx.clip()
ctx.drawRadialGradient(sun, startCenter: CGPoint(x: 1060, y: 240), startRadius: 0, endCenter: CGPoint(x: 1110, y: 300), endRadius: 230, options: [.drawsAfterEndLocation])
// horizon stripes cut through the sun
ctx.setFillColor(rgb(0xC9367F, 0.55))
for i in 0..<6 { let y = 330.0 + Double(i) * 26.0; ctx.fill(CGRect(x: 880, y: y, width: 460, height: 6.0 + Double(i) * 2.2)) }
ctx.restoreGState()
// 4. fine grid
ctx.setStrokeColor(rgb(0xFFFFFF, 0.13)); ctx.setLineWidth(1)
var x = 0.0; while x <= W { ctx.move(to: CGPoint(x: x + 0.5, y: 0)); ctx.addLine(to: CGPoint(x: x + 0.5, y: H)); x += 48 }
var y = 0.0; while y <= H { ctx.move(to: CGPoint(x: 0, y: y + 0.5)); ctx.addLine(to: CGPoint(x: W, y: y + 0.5)); y += 48 }
ctx.strokePath()
// 5. headline
let ns = NSGraphicsContext(cgContext: ctx, flipped: true)
NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = ns
let font = NSFont.systemFont(ofSize: 300, weight: .black)
let para = NSMutableParagraphStyle(); para.alignment = .left
let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: NSColor.white, .kern: -9.0, .paragraphStyle: para]
("Liquid" as NSString).draw(at: NSPoint(x: 78, y: 96), withAttributes: attrs)
("Glass" as NSString).draw(at: NSPoint(x: 78, y: 384), withAttributes: attrs)
// small mono row of marks to show dispersion
let small = NSFont.monospacedSystemFont(ofSize: 15, weight: .medium)
let sattrs: [NSAttributedString.Key: Any] = [.font: small, .foregroundColor: NSColor(white: 1, alpha: 0.85), .kern: 1.2]
("REFRACTION  ·  DISPERSION  ·  FROST  ·  VIBRANCY  ·  RIM LIGHT" as NSString).draw(at: NSPoint(x: 86, y: 804), withAttributes: sattrs)
NSGraphicsContext.restoreGraphicsState()

let img = ctx.makeImage()!
let out = URL(fileURLWithPath: CommandLine.arguments[1])
let dest = CGImageDestinationCreateWithURL(out as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, img, nil); CGImageDestinationFinalize(dest)
print("wrote", out.path, pw, ph)
