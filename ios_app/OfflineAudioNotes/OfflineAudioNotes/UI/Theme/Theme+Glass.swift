//
//  Theme+Glass.swift
//  OfflineAudioNotes
//
//  Created by JOSE ZARABANDA on 12/13/25.
//

import SwiftUI

// MARK: - Liquid Glass Modifiers

struct LiquidGlassEffect: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial) // Fallback/Baseline
            .background(AppColors.background.opacity(0.3)) // Whiten slightly
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: AppColors.primary.opacity(0.15), radius: 15, x: 0, y: 8)
            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(LinearGradient(
                        colors: [.white.opacity(0.6), .white.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ), lineWidth: 1)
            )
    }
}

struct GlassButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .background(AppColors.primary.opacity(0.2)) // Tinted glass
            .background(.thinMaterial)
            .clipShape(Capsule())
            .shadow(color: AppColors.primary.opacity(0.2), radius: 8, x: 0, y: 4)
            .overlay(
                Capsule()
                    .stroke(.white.opacity(0.4), lineWidth: 1)
            )
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - Shimmer Effect
struct Shimmer: ViewModifier {
    @State private var phase: CGFloat = 0
    
    func body(content: Content) -> some View {
        content
            .mask(
                GeometryReader { geometry in
                    let width = geometry.size.width
                    Rectangle()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [.black.opacity(0.3), .black, .black.opacity(0.3)]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .rotationEffect(.degrees(30))
                        .offset(x: -width + (phase * 3 * width))
                }
            )
            .onAppear {
                withAnimation(.linear(duration: 2).repeatForever(autoreverses: false)) {
                    phase = 1
                }
            }
    }
}

extension View {
    func glassCard() -> some View {
        self.modifier(LiquidGlassEffect())
    }
    
    // Updated to use the ButtonStyle for better interaction
    func glassButton() -> some View {
        self.buttonStyle(GlassButtonStyle())
    }
    
    // Official "Liquid" style shim if API not found in this env
    func glassEffect() -> some View {
        self.glassCard()
    }
    
    func shimmer() -> some View {
        self.modifier(Shimmer())
    }
}

