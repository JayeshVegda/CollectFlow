# CollectFlow Agent Instructions

This repository is **CollectFlow**, a solo owner-operator cash collection tracker tailored for the **Nothing Phone (2a) Plus** on Android 14/15/16 (API 36).

For the authoritative mission, architecture, and safety rules, see **[AI_AGENT_GUIDE.md](AI_AGENT_GUIDE.md)**.

---

## Agent Skills Available in This Repository

The `.agents/skills/` directory equips agents with expert domain knowledge for Android and Jetpack Compose development:

### 1. Jetpack Compose UI, Motion & Design
- **[nothing-design](.agents/skills/nothing-design/SKILL.md)**: Nothing OS monochrome aesthetic (`#000000` OLED black, Space Mono typography, red accents, max 16dp radius).
- **[compose-animations](.agents/skills/compose-animations/SKILL.md)**: Motion in Compose, `AnimatedVisibility`, `animate*AsState`, `rememberTransition`, `Animatable`.
- **[compose-component-design](.agents/skills/compose-component-design/SKILL.md)**: Composable API design, slot APIs, layout modifiers.
- **[compose-focus-navigation](.agents/skills/compose-focus-navigation/SKILL.md)**: Focus management and keyboard navigation.
- **[compose-ui](.agents/skills/compose-ui/SKILL.md)**: Best practices for stateless, idiomatic Composables.
- **[compose-navigation](.agents/skills/compose-navigation/SKILL.md)**: Type-safe Navigation Compose, arguments, and backstack patterns.
- **[android-accessibility](.agents/skills/android-accessibility/SKILL.md)**: Touch targets (min 48dp), content descriptions, semantics tree.
- **[coil-compose](.agents/skills/coil-compose/SKILL.md)**: Asynchronous image loading with Coil in Compose.

### 2. State, Effects & Concurrency
- **[compose-state-and-effects](.agents/skills/compose-state-and-effects/SKILL.md)**: State hoisting, `LaunchedEffect`, `DisposableEffect`, snapshot state.
- **[kotlin-concurrency-and-flow](.agents/skills/kotlin-concurrency-and-flow/SKILL.md)**: Structured concurrency, Flow state vs event modeling.
- **[android-coroutines](.agents/skills/android-coroutines/SKILL.md)**: Android Coroutine scopes, lifecycle-aware collection.
- **[kotlin-concurrency-expert](.agents/skills/kotlin-concurrency-expert/SKILL.md)**: Concurrency debugging, race condition prevention.
- **[kotlin-control-flow](.agents/skills/kotlin-control-flow/SKILL.md)**: Exhaustive `when`, contracts, and idiomatic flow.
- **[kotlin-api-design](.agents/skills/kotlin-api-design/SKILL.md)**: Value classes, domain modeling (e.g. `Paise`), clean function signatures.

### 3. Performance & Benchmarking
- **[compose-performance](.agents/skills/compose-performance/SKILL.md)**: Recomposition optimization, compiler stability reports, skippability, deferred reads.
- **[compose-performance-audit](.agents/skills/compose-performance-audit/SKILL.md)**: Recomposition diagnosis and memory leak checks.
- **[gradle-build-performance](.agents/skills/gradle-build-performance/SKILL.md)**: Build cache and configuration optimization.
- **[android-benchmark-comparison](.agents/skills/android-benchmark-comparison/SKILL.md)**: Android Macrobenchmark & Microbenchmark guidelines.

### 4. Architecture, Data Layer & Reliability
- **[android-architecture](.agents/skills/android-architecture/SKILL.md)**: Modern Android architecture patterns.
- **[android-viewmodel](.agents/skills/android-viewmodel/SKILL.md)**: `ViewModel` patterns, `StateFlow`, UDF.
- **[android-data-layer](.agents/skills/android-data-layer/SKILL.md)**: Offline-first Room database, repository abstraction.
- **[android-gradle-logic](.agents/skills/android-gradle-logic/SKILL.md)**: Gradle Kotlin DSL, Version Catalogs (`libs.versions.toml`).
- **[android-retrofit](.agents/skills/android-retrofit/SKILL.md)**: Network client guidelines.

### 5. Testing & Automation
- **[android-testing](.agents/skills/android-testing/SKILL.md)**: Unit testing, assertions, StateFlow testing.
- **[compose-ui-testing-patterns](.agents/skills/compose-ui-testing-patterns/SKILL.md)**: Compose UI assertions, semantics interactions.
- **[android-emulator-skill](.agents/skills/android-emulator-skill/SKILL.md)**: Automated emulator scripting, ADB navigation, screen mapping.

### 6. Mobile App UI Design & Branding
- **[mobile-app-ui-design](.agents/skills/mobile-app-ui-design/SKILL.md)**: Mobile interface design patterns and interaction flows.
- **[logo-design](.agents/skills/logo-design/SKILL.md)**: App icon, logo geometry, and visual asset styling.
- **[appllama-app-design-skill](.agents/skills/appllama-app-design-skill/SKILL.md)**: UI layout composition.

### 7. Android Systems & Reverse Engineering
- **[android-reverse-engineering](.agents/skills/android-reverse-engineering/SKILL.md)**: APK/AAR decompilation, calling chain analysis, and JNI/SO debugging.
- **[integrate-adsdk](.agents/skills/integrate-adsdk/SKILL.md)**: Mobile ad network mediation and SDK integration patterns.

### 8. Play Store Optimization (ASO) & Analytics
- **[google-play-listing](.agents/skills/google-play-listing/SKILL.md)**: Play Store listing optimization and guidelines.
- **[android-aso](.agents/skills/android-aso/SKILL.md)**: App Store Optimization for Android apps.
- **[screenshot-optimization](.agents/skills/screenshot-optimization/SKILL.md)**: App screenshot layout and messaging.
- **[app-icon-optimization](.agents/skills/app-icon-optimization/SKILL.md)**: App icon visual testing and conversion.
- **[crash-analytics](.agents/skills/crash-analytics/SKILL.md)**: Crash tracking and stability metrics.

### 9. Workflow & Planning
- **[to-plan](.agents/skills/to-plan/SKILL.md)**: Step-by-step implementation planning.
- **[deliver-spec](.agents/skills/deliver-spec/SKILL.md)**: Specification verification workflows.
- **[shepherd](.agents/skills/shepherd/SKILL.md)**: Code review and verification practices.
- **[gradle-run](.agents/skills/gradle-run/SKILL.md)**: CLI Gradle execution guidelines.
