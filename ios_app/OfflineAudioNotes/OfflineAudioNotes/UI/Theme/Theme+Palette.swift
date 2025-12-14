//
//  Theme+Palette.swift
//  OfflineAudioNotes
//
//  Created by JOSE ZARABANDA on 12/13/25.
//

import SwiftUI

struct AppColors {
    // Pure White Background
    static let background = Color.white
    
    // Palette Accents
    static let primary = Color(hex: "90f1ef") // Aqua
    static let secondary = Color(hex: "ffd6e0") // Pink
    static let highlight = Color(hex: "ffef9f") // Yellow
    static let success = Color(hex: "c1fba4") // Light Green
    static let warning = Color(hex: "7bf1a8") // Greenish Aqua (Using as warning/active)
    
    // Semantic aliases
    static let tint = primary
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
