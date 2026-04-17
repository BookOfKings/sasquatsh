import SwiftUI
import SceneKit

struct D20SpinnerView: UIViewRepresentable {
    var size: CGFloat = 80
    var color: UIColor = UIColor(red: 0.388, green: 0.400, blue: 0.945, alpha: 1)
    var numberColor: UIColor = .white

    func makeUIView(context: Context) -> SCNView {
        let scnView = SCNView()
        scnView.backgroundColor = .clear
        scnView.autoenablesDefaultLighting = false
        scnView.allowsCameraControl = false
        scnView.isUserInteractionEnabled = false
        scnView.antialiasingMode = .multisampling4X

        let scene = SCNScene()
        scnView.scene = scene

        // Camera
        let cameraNode = SCNNode()
        cameraNode.camera = SCNCamera()
        cameraNode.position = SCNVector3(0, 0, 3.2)
        scene.rootNode.addChildNode(cameraNode)

        // Ambient light
        let ambientLight = SCNNode()
        ambientLight.light = SCNLight()
        ambientLight.light?.type = .ambient
        ambientLight.light?.intensity = 500
        scene.rootNode.addChildNode(ambientLight)

        // Key light
        let keyLight = SCNNode()
        keyLight.light = SCNLight()
        keyLight.light?.type = .directional
        keyLight.light?.intensity = 900
        keyLight.position = SCNVector3(2, 3, 4)
        keyLight.look(at: SCNVector3Zero)
        scene.rootNode.addChildNode(keyLight)

        // Fill light
        let fillLight = SCNNode()
        fillLight.light = SCNLight()
        fillLight.light?.type = .directional
        fillLight.light?.intensity = 400
        fillLight.position = SCNVector3(-3, -1, 2)
        fillLight.look(at: SCNVector3Zero)
        scene.rootNode.addChildNode(fillLight)

        let d20Node = createD20Node()
        scene.rootNode.addChildNode(d20Node)

        // Spin animation
        let spin = CABasicAnimation(keyPath: "rotation")
        spin.toValue = NSValue(scnVector4: SCNVector4(0.3, 1, 0.15, Float.pi * 2))
        spin.duration = 3.5
        spin.repeatCount = .infinity
        d20Node.addAnimation(spin, forKey: "spin")

        return scnView
    }

    func updateUIView(_ uiView: SCNView, context: Context) {}

    private func createD20Node() -> SCNNode {
        let phi: Float = (1 + sqrt(5)) / 2
        let s: Float = 0.75

        let verts: [SCNVector3] = [
            SCNVector3(-1 * s,  phi * s, 0),
            SCNVector3( 1 * s,  phi * s, 0),
            SCNVector3(-1 * s, -phi * s, 0),
            SCNVector3( 1 * s, -phi * s, 0),
            SCNVector3(0, -1 * s,  phi * s),
            SCNVector3(0,  1 * s,  phi * s),
            SCNVector3(0, -1 * s, -phi * s),
            SCNVector3(0,  1 * s, -phi * s),
            SCNVector3( phi * s, 0, -1 * s),
            SCNVector3( phi * s, 0,  1 * s),
            SCNVector3(-phi * s, 0, -1 * s),
            SCNVector3(-phi * s, 0,  1 * s),
        ]

        let faces: [[Int]] = [
            [0, 11, 5], [0, 5, 1], [0, 1, 7], [0, 7, 10], [0, 10, 11],
            [1, 5, 9], [5, 11, 4], [11, 10, 2], [10, 7, 6], [7, 1, 8],
            [3, 9, 4], [3, 4, 2], [3, 2, 6], [3, 6, 8], [3, 8, 9],
            [4, 9, 5], [2, 4, 11], [6, 2, 10], [8, 6, 7], [9, 8, 1],
        ]

        let numbers = [20, 8, 14, 2, 17, 1, 13, 7, 19, 4, 16, 10, 6, 18, 12, 5, 11, 15, 3, 9]

        // Build a single geometry with all faces
        var allVertices: [SCNVector3] = []
        var allNormals: [SCNVector3] = []
        var allIndices: [Int32] = []
        var faceTextures: [UIImage] = []

        let parentNode = SCNNode()

        for (i, face) in faces.enumerated() {
            let v0 = verts[face[0]]
            let v1 = verts[face[1]]
            let v2 = verts[face[2]]

            // Normal
            let e1 = SCNVector3(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z)
            let e2 = SCNVector3(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z)
            let n = SCNVector3(
                e1.y * e2.z - e1.z * e2.y,
                e1.z * e2.x - e1.x * e2.z,
                e1.x * e2.y - e1.y * e2.x
            )

            // Create face with number texture
            let texture = renderNumberTexture(number: numbers[i])

            let material = SCNMaterial()
            material.diffuse.contents = texture
            material.specular.contents = UIColor.white.withAlphaComponent(0.3)
            material.shininess = 0.4
            material.isDoubleSided = true

            let faceVerts = [v0, v1, v2]
            let faceNormals = [n, n, n]
            // UV: map triangle to texture
            let uvs: [CGPoint] = [CGPoint(x: 0.5, y: 0), CGPoint(x: 0, y: 1), CGPoint(x: 1, y: 1)]

            let vertexSource = SCNGeometrySource(vertices: faceVerts)
            let normalSource = SCNGeometrySource(normals: faceNormals)
            let uvSource = SCNGeometrySource(textureCoordinates: uvs)
            let element = SCNGeometryElement(indices: [Int32(0), Int32(1), Int32(2)], primitiveType: .triangles)

            let geometry = SCNGeometry(sources: [vertexSource, normalSource, uvSource], elements: [element])
            geometry.materials = [material]

            let faceNode = SCNNode(geometry: geometry)
            parentNode.addChildNode(faceNode)
        }

        return parentNode
    }

    private func renderNumberTexture(number: Int) -> UIImage {
        let size = CGSize(width: 128, height: 128)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            // Fill with face color
            color.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))

            // Draw number
            let text = "\(number)" as NSString
            let fontSize: CGFloat = number >= 10 ? 38 : 44
            let font = UIFont.systemFont(ofSize: fontSize, weight: .bold)
            let attrs: [NSAttributedString.Key: Any] = [
                .font: font,
                .foregroundColor: numberColor,
            ]
            let textSize = text.size(withAttributes: attrs)
            let x = (size.width - textSize.width) / 2
            let y = (size.height - textSize.height) / 2 + 10 // offset down into triangle center
            text.draw(at: CGPoint(x: x, y: y), withAttributes: attrs)
        }
    }
}
