import Foundation

struct EntityID: Codable, Hashable, RawRepresentable, ExpressibleByStringLiteral {
    let rawValue: String

    init(rawValue: String) {
        self.rawValue = rawValue
    }

    init(stringLiteral value: StringLiteralType) {
        self.rawValue = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let string = try? container.decode(String.self) {
            rawValue = string
            return
        }
        if let intValue = try? container.decode(Int64.self) {
            rawValue = String(intValue)
            return
        }
        throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported entity id")
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}
