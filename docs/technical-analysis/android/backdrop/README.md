# Backdrop 模块技术分析

## 文件清单
- Backdrop.kt
- BackdropEffectScope.kt
- DrawBackdropModifier.kt
- InverseLayerScope.kt
- LayerRecorder.kt
- Outline.kt
- RuntimeShaderCache.kt
- ShapeProvider.kt
- Shaders.kt
- effects/Blur.kt
- effects/ColorFilter.kt
- effects/Lens.kt
- effects/RenderEffect.kt
- backdrops/Backdrop.kt
- backdrops/CanvasBackdrop.kt
- backdrops/CombinedBackdrop.kt
- backdrops/EmptyBackdrop.kt
- backdrops/LayerBackdrop.kt
- backdrops/LayerBackdropModifier.kt
- shadow/InnerShadow.kt
- shadow/InnerShadowModifier.kt
- shadow/Shadow.kt
- shadow/ShadowModifier.kt
- highlight/Highlight.kt
- highlight/HighlightModifier.kt
- highlight/HighlightStyle.kt

---

## Backdrop.kt

### Backdrop
- 类型：`interface`
- 职责：定义背景绘制协议，是整个 backdrop 系统的核心抽象。所有背景实现（LayerBackdrop、CanvasBackdrop、EmptyBackdrop 等）都实现此接口。
- 设计模式：策略模式（不同的 Backdrop 实现提供不同的背景绘制策略）

#### isCoordinatesDependent: Boolean
- 作用域：接口属性
- 使用场景：标识此 Backdrop 是否依赖布局坐标信息。若为 `true`，DrawBackdropModifier 会持续提供 LayoutCoordinates；若为 `false`，可传入 null 以避免不必要的坐标追踪开销。
- 建议：命名清晰，设计合理。

#### DrawScope.drawBackdrop(density, coordinates, layerBlock)
- 参数：
  - `density: Density` — 当前绘制环境的密度信息，用于 dp 到 px 转换
  - `coordinates: LayoutCoordinates?` — 布局坐标信息，可为 null（当 `isCoordinatesDependent = false` 时）
  - `layerBlock: (GraphicsLayerScope.() -> Unit)? = null` — 可选的图层变换块，用于反向变换
- 返回值：无（DrawScope 扩展函数，直接绘制到画布）
- 实现逻辑：由各实现类定义具体的背景绘制方式
- 调用关系：由 `DrawBackdropNode` 的 `recordBackdropBlock` 调用
- 建议：接口设计简洁，扩展函数接收 `DrawScope` 作为接收者使得实现类可以直接使用绘制 API，是 Compose 自定义绘制的惯用模式。

---

## BackdropEffectScope.kt

### BackdropEffectScope
- 类型：`sealed interface`
- 父接口：`Density`, `RuntimeShaderCache`
- 职责：定义背景效果作用域的公共接口，供 effects 包中的扩展函数（blur、colorFilter、lens 等）使用。
- 设计模式：DSL 作用域（通过扩展函数构建效果链）

#### size: Size
- 作用域：接口属性
- 使用场景：当前绘制区域尺寸，供效果计算使用

#### layoutDirection: LayoutDirection
- 作用域：接口属性
- 使用场景：布局方向（LTR/RTL），影响圆角等属性的计算

#### shape: Shape
- 作用域：接口属性
- 使用场景：当前形状，供 lens 等需要圆角信息的效果使用

#### padding: Float
- 作用域：接口可变属性
- 初始值：`0f`
- 使用场景：效果所需的额外内边距（如模糊半径），DrawBackdropModifier 会据此扩展绘制区域
- 建议：作为可变属性在接口中定义，由效果函数（如 blur）修改，这是一种副作用式的设计。建议在文档中明确说明此属性的修改语义。

#### renderEffect: RenderEffect?
- 作用域：接口可变属性
- 初始值：`null`
- 使用场景：累积的渲染效果链，由效果函数逐步构建
- 建议：同 padding，属于可变状态，效果函数通过追加方式构建效果链。

### BackdropEffectScopeImpl
- 类型：`internal abstract class`
- 父类：`BackdropEffectScope`, `RuntimeShaderCache`
- 职责：`BackdropEffectScope` 的内部实现基类，管理效果作用域的状态

#### density: Float
- 初始值：`1f`
- 使用场景：屏幕密度

#### fontScale: Float
- 初始值：`1f`
- 使用场景：字体缩放因子

#### size: Size
- 初始值：`Size.Unspecified`
- 使用场景：绘制区域尺寸

#### layoutDirection: LayoutDirection
- 初始值：`LayoutDirection.Ltr`
- 使用场景：布局方向

#### padding: Float
- 初始值：`0f`
- 使用场景：效果额外内边距

#### renderEffect: RenderEffect?
- 初始值：`null`
- 使用场景：渲染效果链

#### runtimeShaderCache: RuntimeShaderCacheImpl
- 作用域：私有
- 使用场景：AGSL 着色器缓存

#### update(scope: DrawScope): Boolean
- 参数：`scope: DrawScope` — 当前绘制作用域
- 返回值：`Boolean` — 状态是否发生变化
- 实现逻辑：比较 density、fontScale、size、layoutDirection 是否变化，若变化则更新并返回 true
- 调用关系：由 `DrawBackdropNode.draw()` 调用
- 建议：增量更新机制设计合理，避免不必要的重绘。

#### apply(effects: BackdropEffectScope.() -> Unit)
- 参数：`effects` — 效果 DSL 块
- 返回值：无
- 实现逻辑：重置 padding 和 renderEffect，然后执行效果块
- 调用关系：由 `DrawBackdropNode.updateEffects()` 调用
- 建议：每次应用效果前重置状态是正确的，确保效果链从干净状态开始构建。

#### reset()
- 返回值：无
- 实现逻辑：将所有属性恢复到默认值，清空着色器缓存
- 调用关系：由 `DrawBackdropNode.onDetach()` 调用
- 建议：资源清理完整，防止内存泄漏。

---

## DrawBackdropModifier.kt

### drawPlainBackdrop(backdrop, shape, effects, ...)
- 类型：`Modifier` 扩展函数
- 职责：创建不带高光和阴影的纯背景绘制 Modifier
- 设计模式：Modifier 工厂函数

#### 参数
- `backdrop: Backdrop` — 背景实现
- `shape: () -> Shape` — 形状提供者（lambda 延迟求值）
- `effects: BackdropEffectScope.() -> Unit` — 效果 DSL 块
- `layerBlock: (GraphicsLayerScope.() -> Unit)? = null` — 可选的图层变换
- `exportedBackdrop: LayerBackdrop? = null` — 可导出的图层背景
- `onDrawBehind: (DrawScope.() -> Unit)? = null` — 背景后绘制回调
- `onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop` — 自定义背景绘制回调
- `onDrawSurface: (DrawScope.() -> Unit)? = null` — 表面绘制回调
- `onDrawFront: (DrawScope.() -> Unit)? = null` — 前景绘制回调

#### 实现逻辑
1. 创建 `ShapeProvider` 包装 shape lambda
2. 如果有 layerBlock，先应用 `graphicsLayer` Modifier
3. 应用 `DrawBackdropElement`

#### 建议：与 `drawBackdrop` 相比缺少高光和阴影支持，适用于不需要这些效果的简单场景。

### drawBackdrop(backdrop, shape, effects, highlight, shadow, ...)
- 类型：`Modifier` 扩展函数
- 职责：创建完整的背景绘制 Modifier，包含高光、阴影和内阴影支持
- 设计模式：Modifier 工厂函数（组合模式）

#### 参数
- 与 `drawPlainBackdrop` 相同的参数，加上：
- `highlight: (() -> Highlight?)? = DefaultHighlight` — 高光配置（默认启用）
- `shadow: (() -> Shadow?)? = DefaultShadow` — 阴影配置（默认启用）
- `innerShadow: (() -> InnerShadow?)? = null` — 内阴影配置（默认禁用）

#### 实现逻辑
1. 创建 `ShapeProvider`
2. 可选应用 `graphicsLayer`
3. 可选应用 `InnerShadowElement`
4. 可选应用 `ShadowElement`
5. 可选应用 `HighlightElement`
6. 应用 `DrawBackdropElement`

#### 建议：Modifier 链的顺序很重要——内阴影 → 外阴影 → 高光 → 背景，这个顺序确保了正确的视觉层叠。

### DefaultHighlight
- 类型：私有顶层属性 `() -> Highlight`
- 初始值：`{ Highlight.Default }`
- 使用场景：drawBackdrop 的 highlight 参数默认值

### DefaultShadow
- 类型：私有顶层属性 `() -> Shadow`
- 初始值：`{ Shadow.Default }`
- 使用场景：drawBackdrop 的 shadow 参数默认值

### DefaultOnDrawBackdrop
- 类型：私有顶层属性 `DrawScope.(DrawScope.() -> Unit) -> Unit`
- 初始值：`{ it() }`
- 使用场景：默认的背景绘制策略，直接执行传入的绘制块

### DrawBackdropElement
- 类型：`private class`
- 父类：`ModifierNodeElement<DrawBackdropNode>`
- 职责：Modifier 节点元素，负责创建和更新 `DrawBackdropNode`

#### 所有属性（backdrop, shapeProvider, effects, layerBlock, exportedBackdrop, onDrawBehind, onDrawBackdrop, onDrawSurface, onDrawFront）
- 作用域：构造参数，不可变
- 使用场景：传递给 DrawBackdropNode 的配置

#### create(): DrawBackdropNode
- 返回值：新创建的 `DrawBackdropNode`
- 实现逻辑：使用当前所有属性创建节点

#### update(node: DrawBackdropNode)
- 参数：`node` — 需要更新的节点
- 实现逻辑：更新节点的所有可变属性，然后调用 `invalidateDrawCache()`

#### equals()/hashCode()
- 实现逻辑：逐一比较所有属性
- 建议：实现完整，符合 ModifierNodeElement 的契约。

### DrawBackdropNode
- 类型：`private class`
- 父类：`LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode, Modifier.Node`
- 职责：核心绘制节点，负责测量、布局、效果应用和背景绘制
- 设计模式：Compose Modifier Node API（节点式 Modifier）

#### effectScope: BackdropEffectScopeImpl
- 作用域：私有，匿名子类
- 使用场景：覆盖 `shape` 属性以从 `shapeProvider` 获取

#### graphicsLayer: GraphicsLayer?
- 作用域：私有
- 初始值：`null`
- 使用场景：用于录制背景内容并应用 RenderEffect

#### layoutLayerBlock: GraphicsLayerScope.() -> Unit
- 作用域：私有
- 使用场景：为 placeable 的图层设置 clip、shape 和 Offscreen 合成策略

#### layoutCoordinates: LayoutCoordinates?
- 作用域：私有，使用 `mutableStateOf(null, neverEqualPolicy())`
- 使用场景：存储布局坐标，供坐标依赖的 Backdrop 使用

#### padding: Float
- 作用域：私有，使用 `mutableFloatStateOf`
- 初始值：`0f`
- 使用场景：效果所需的额外内边距

#### recordBackdropBlock: DrawScope.() -> Unit
- 作用域：私有
- 实现逻辑：根据 padding 平移画布，调用 `onDrawBackdrop` 委托绘制背景，然后恢复平移

#### drawBackdropLayer: DrawScope.() -> Unit
- 作用域：私有
- 实现逻辑：将背景录制到 GraphicsLayer（考虑 padding 扩展尺寸），设置 topLeft 偏移，然后绘制图层

#### measure(measurable, constraints): MeasureResult
- 参数：
  - `measurable: Measurable` — 被测量的子组件
  - `constraints: Constraints` — 测量约束
- 返回值：`MeasureResult`
- 实现逻辑：测量子组件，使用 `placeWithLayer` 放置并应用 `layoutLayerBlock`

#### draw()
- 实现逻辑：
  1. 检查 effectScope 是否需要更新
  2. 按顺序绘制：onDrawBehind → backdropLayer → onDrawSurface → content → onDrawFront
  3. 如果有 exportedBackdrop，将背景内容录制到其 GraphicsLayer
- 建议：绘制顺序设计合理，提供了多个钩子点供自定义。

#### onGloballyPositioned(coordinates)
- 实现逻辑：如果 Backdrop 依赖坐标，更新 layoutCoordinates；否则清空。同时更新 exportedBackdrop 的坐标。

#### onObservedReadsChanged()
- 实现逻辑：调用 `invalidateDrawCache()`

#### invalidateDrawCache()
- 实现逻辑：调用 `observeEffects()`

#### observeEffects()
- 实现逻辑：在 `observeReads` 块中调用 `updateEffects()`

#### updateEffects()
- 实现逻辑：如果 API >= S，应用效果块并更新 GraphicsLayer 的 renderEffect 和 padding
- 建议：API 级别检查是必要的，RenderEffect 从 Android 12 (S) 开始支持。

#### onAttach()
- 实现逻辑：创建 GraphicsLayer，开始观察效果

#### onDetach()
- 实现逻辑：释放 GraphicsLayer，重置 effectScope 和坐标
- 建议：资源清理完整，防止内存泄漏。

---

## InverseLayerScope.kt

### InverseLayerScope
- 类型：`internal class`
- 父接口：`GraphicsLayerScope`
- 职责：实现图层变换的逆变换，用于 LayerBackdrop 中抵消源组件的图层变换效果
- 设计模式：数学逆变换计算

#### 所有 GraphicsLayerScope 属性（size, density, fontScale, scaleX, scaleY, alpha, translationX, translationY, shadowElevation, ambientShadowColor, spotShadowColor, rotationX, rotationY, rotationZ, cameraDistance, transformOrigin, shape, clip, renderEffect, compositingStrategy）
- 作用域：类属性
- 使用场景：暂存图层变换参数，用于逆变换计算

#### matrix: Matrix?
- 作用域：私有
- 初始值：`null`
- 使用场景：缓存矩阵对象避免重复分配

#### DrawTransform.inverseTransform(density, layerBlock)
- 参数：
  - `density: Density` — 密度信息
  - `layerBlock: GraphicsLayerScope.() -> Unit` — 原始图层变换块
- 实现逻辑：
  1. 将 DrawTransform 的 size 和 density 复制到自身
  2. 执行 layerBlock，捕获变换参数
  3. 调用 `inverseTransformAtTopLeft` 计算逆变换
- 调用关系：由 `LayerBackdrop.drawBackdrop()` 调用

#### reset()
- 实现逻辑：将所有属性恢复到默认值
- 调用关系：由 `LayerBackdrop.obtainInverseLayerScope()` 调用

#### DrawTransform.inverseTransformAtTopLeft(rotationZ, scaleX, scaleY)
- 参数：
  - `rotationZ: Float = 0f` — Z 轴旋转角度
  - `scaleX: Float = 1f` — X 轴缩放
  - `scaleY: Float = 1f` — Y 轴缩放
- 实现逻辑：
  1. 如果无旋转，仅做缩放逆变换（1/scaleX, 1/scaleY）
  2. 如果有旋转，构建 2D 旋转+缩放矩阵，计算其逆矩阵并应用
- 建议：
  - 当前仅处理 rotationZ + scale 的逆变换，忽略了 translationX/Y、rotationX/Y。如果图层变换包含平移或 3D 旋转，逆变换将不正确。
  - 矩阵缓存（`matrix` 属性）避免了每次重新分配，是好的优化。
  - `det == 0f` 的检查防止了奇异矩阵导致的除零错误。

---

## LayerRecorder.kt

### recordLayer(layer, density, size, block)
- 类型：`internal fun DrawScope.recordLayer` 扩展函数
- 职责：在 DrawScope 中将内容录制到 GraphicsLayer

#### 参数
- `layer: GraphicsLayer` — 目标图层
- `density: Density = drawContext.density` — 密度信息，默认使用当前绘制上下文的密度
- `size: IntSize = this.size.toIntSize()` — 录制尺寸，默认使用当前绘制区域尺寸
- `block: DrawScope.() -> Unit` — 要录制的内容

#### 返回值：无

#### 实现逻辑
1. 调用 `layer.record(size)` 开始录制
2. 在录制块中临时替换 `drawContext.density` 为传入的 density
3. 执行 block
4. 在 finally 块中恢复原始 density

#### 调用关系
- 被 `DrawBackdropNode.drawBackdropLayer` 调用
- 被 `LayerBackdropNode.draw()` 调用
- 被 `ShadowNode.draw()` 调用

#### 建议：density 的保存/恢复机制确保了录制过程中的密度一致性，try-finally 保证了异常安全。

---

## Outline.kt

### Canvas.clipOutline(outline, path)
- 类型：`internal fun Canvas.clipOutline` 扩展函数
- 职责：根据 Outline 类型对 Canvas 进行裁剪

#### 参数
- `outline: Outline` — 轮廓（Rectangle / Rounded / Generic）
- `path: Path?` — 可复用的 Path 对象（仅用于 Rounded 类型）

#### 实现逻辑
- `Outline.Rectangle` → `clipRect`
- `Outline.Rounded` → 将 RoundRect 添加到 Path 后 `clipPath`
- `Outline.Generic` → 直接 `clipPath`

#### 调用关系
- 被 `InnerShadowNode.draw()` 调用
- 被 `HighlightNode.draw()` 调用

#### 建议：Path 复用（`path!!.rewind()` + `path.addRoundRect()`）避免了每次创建新 Path 对象，是好的性能优化。

---

## RuntimeShaderCache.kt

### RuntimeShaderCache
- 类型：`sealed interface`
- 职责：定义 AGSL RuntimeShader 缓存的公共接口
- 设计模式：缓存模式

#### obtainRuntimeShader(key, string): RuntimeShader
- 参数：
  - `key: String` — 缓存键
  - `string: String` — AGSL 着色器代码（`@Language("AGSL")`）
- 返回值：`RuntimeShader`
- API 要求：`@RequiresApi(Build.VERSION_CODES.TIRAMISU)`
- 实现逻辑：由实现类定义

### RuntimeShaderCacheImpl
- 类型：`internal class`
- 父接口：`RuntimeShaderCache`
- 职责：AGSL RuntimeShader 缓存的内部实现

#### runtimeShaders: MutableMap\<String, RuntimeShader\>
- 作用域：私有
- 使用场景：存储已编译的着色器，避免重复编译

#### obtainRuntimeShader(key, string): RuntimeShader
- 实现逻辑：使用 `getOrPut` 从缓存获取或创建新的 RuntimeShader
- 建议：`getOrPut` 是惰性创建，性能合理。但需注意 RuntimeShader 对象的内存占用，长时间运行可能积累大量缓存。

#### clear()
- 实现逻辑：清空缓存映射
- 调用关系：由 `BackdropEffectScopeImpl.reset()` 和 `HighlightNode.onDetach()` 调用

---

## ShapeProvider.kt

### ShapeProvider
- 类型：`internal class`
- 注解：`@Immutable`
- 职责：包装 Shape lambda 并提供带缓存的 Outline 创建

#### shapeBlock: () -> Shape
- 作用域：构造参数
- 使用场景：延迟求值的形状提供者

#### innerShape: Shape
- 作用域：属性（getter）
- 实现逻辑：直接调用 `shapeBlock()`
- 使用场景：供 BackdropEffectScope 获取当前形状

#### shape: Shape（匿名对象）
- 作用域：属性
- 实现逻辑：带缓存的 `createOutline`——如果 shape、size、layoutDirection、density 未变化，返回缓存的 Outline
- 使用场景：供 Shadow、Highlight、DrawBackdropNode 等获取形状轮廓

#### _shape: Shape?
- 作用域：私有
- 初始值：`null`
- 使用场景：缓存上次的 Shape 对象引用

#### _outline: Outline?
- 作用域：私有
- 初始值：`null`
- 使用场景：缓存的 Outline 对象

#### _size: Size
- 作用域：私有
- 初始值：`Size.Unspecified`
- 使用场景：缓存的上次尺寸

#### _layoutDirection: LayoutDirection?
- 作用域：私有
- 初始值：`null`
- 使用场景：缓存的上次布局方向

#### _density: Float?
- 作用域：私有
- 初始值：`null`
- 使用场景：缓存的上次密度值

#### 建议：缓存机制避免了每次绘制时重新计算 Outline，对于复杂形状（如圆角矩形）有明显的性能提升。但需注意 `shapeBlock()` 每次调用 `innerShape` 时都会执行，如果 lambda 中的计算较重，可考虑也加缓存。

---

## Shaders.kt

### RoundedRectSDF
- 类型：`private const String`（`@Language("AGSL")`）
- 职责：圆角矩形有符号距离场（SDF）的 AGSL 着色器代码片段，被其他着色器引用

#### radiusAt(coord, radii): Float
- 参数：`coord: float2`（坐标），`radii: float4`（四角半径）
- 返回值：当前坐标对应的圆角半径

#### sdRoundedRect(coord, halfSize, radius): Float
- 参数：`coord: float2`，`halfSize: float2`（半尺寸），`radius: float`（统一半径）
- 返回值：有符号距离值（负值在内部，正值在外部）

#### gradSdRoundedRect(coord, halfSize, radius): float2
- 参数：同上
- 返回值：SDF 的梯度向量（用于折射方向计算）

### RoundedRectRefractionShaderString
- 类型：`internal const String`（`@Language("AGSL")`）
- 职责：圆角矩形折射效果的 AGSL 着色器

#### uniform 变量
- `content: shader` — 输入内容
- `size: float2` — 尺寸
- `offset: float2` — 偏移
- `cornerRadii: float4` — 四角半径
- `refractionHeight: float` — 折射高度
- `refractionAmount: float` — 折射量
- `depthEffect: float` — 深度效果开关

#### circleMap(x): Float
- 实现逻辑：`1.0 - sqrt(1.0 - x * x)`，圆形映射函数

#### main(coord): half4
- 实现逻辑：
  1. 计算当前坐标到圆角矩形边缘的距离
  2. 如果距离超过折射高度，直接返回原始内容
  3. 使用 circleMap 计算折射偏移量
  4. 计算 SDF 梯度方向
  5. 沿梯度方向偏移采样坐标

### RoundedRectRefractionWithDispersionShaderString
- 类型：`internal val String`（`@Language("AGSL")`）
- 职责：带色散的圆角矩形折射效果着色器

#### 额外 uniform 变量
- `chromaticAberration: float` — 色差强度

#### main(coord): half4
- 实现逻辑：在基础折射之上，对 R/O/Y/G/C/B/P 七个光谱分量分别偏移采样，模拟色散效果
- 建议：七次 `content.eval()` 调用对 GPU 开销较大，在低端设备上可能影响性能。可考虑减少采样次数（如只用 RGB 三次）。

### DefaultHighlightShaderString
- 类型：`internal const String`（`@Language("AGSL")`）
- 职责：默认高光效果着色器，基于 SDF 梯度与法线方向的点积计算高光强度

#### uniform 变量
- `size: float2`，`cornerRadii: float4`，`color: half4`（layout(color)），`angle: float`，`falloff: float`

#### main(coord): half4
- 实现逻辑：计算 SDF 梯度与指定角度法线的点积，取绝对值的 falloff 次幂作为强度

### AmbientHighlightShaderString
- 类型：`internal const String`（`@Language("AGSL")`）
- 职责：环境光高光效果着色器，根据梯度方向的正负分别产生明暗效果

#### main(coord): half4
- 实现逻辑：与 Default 类似，但使用 `step(0.0, d)` 区分正负梯度方向

### GammaAdjustmentShaderString
- 类型：`internal const String`（`@Language("AGSL")`）
- 职责：Gamma 校正着色器

#### uniform 变量
- `content: shader`，`power: float`

#### main(coord): half4
- 实现逻辑：对 RGB 通道分别应用 `pow(color, power)` 变换

#### 建议：着色器代码质量高，注释清晰。AGSL 与 GLSL 语法一致，但运行在 Android RuntimeShader 上。建议为每个着色器添加数学原理注释，方便后续维护。

---

## effects/Blur.kt

### BackdropEffectScope.blur(radius, edgeTreatment)
- 类型：`BackdropEffectScope` 扩展函数
- 职责：向效果链添加模糊效果

#### 参数
- `radius: Float`（`@FloatRange(from = 0.0)`）— 模糊半径
- `edgeTreatment: TileMode = TileMode.Clamp` — 边缘处理模式

#### 实现逻辑
1. API < S 直接返回
2. radius <= 0 直接返回
3. 如果边缘模式非 Clamp 或已有 RenderEffect，更新 padding
4. 如果已有 RenderEffect，创建链式模糊效果；否则创建独立模糊效果

#### 调用关系：由效果 DSL 块调用

#### 建议：
- 模糊半径使用 px 而非 dp，调用者需自行转换。建议提供 dp 重载。
- padding 的更新逻辑：仅在非 Clamp 模式或有前置效果时才增加 padding，这是因为 Clamp 模式下模糊不需要额外空间。

---

## effects/ColorFilter.kt

### BackdropEffectScope.colorFilter(colorFilter: ColorFilter)
- 类型：扩展函数
- 职责：添加 Android 原生 ColorFilter 效果

#### 参数
- `colorFilter: ColorFilter` — Android 原生颜色过滤器

#### 实现逻辑：如果已有 RenderEffect，创建链式效果；否则创建独立 ColorFilter 效果

### BackdropEffectScope.colorFilter(colorFilter: Compose ColorFilter)
- 类型：扩展函数
- 职责：添加 Compose ColorFilter 效果（委托给 Android 原生版本）

### BackdropEffectScope.opacity(alpha)
- 类型：扩展函数
- 职责：调整不透明度

#### 参数
- `alpha: Float`（`@FloatRange(from = 0.0, to = 1.0)`）— 透明度

#### 实现逻辑：通过 ColorMatrix 的 alpha 通道实现

### BackdropEffectScope.colorControls(brightness, contrast, saturation)
- 类型：扩展函数
- 职责：调整亮度、对比度和饱和度

#### 参数
- `brightness: Float = 0f` — 亮度偏移
- `contrast: Float = 1f` — 对比度倍率
- `saturation: Float = 1f` — 饱和度倍率

#### 实现逻辑：如果参数都是默认值则跳过；否则通过 ColorMatrix 实现

### BackdropEffectScope.vibrancy()
- 类型：扩展函数
- 职责：应用鲜艳度增强（饱和度 1.5 倍）

### VibrantColorFilter
- 类型：私有顶层属性
- 初始值：`colorControlsColorFilter(saturation = 1.5f)`
- 使用场景：vibrancy() 的底层实现

### BackdropEffectScope.exposureAdjustment(ev)
- 类型：扩展函数
- 职责：曝光调整

#### 参数
- `ev: Float` — 曝光值（EV）

#### 实现逻辑：`scale = 2^(ev/2.2)`，通过 ColorMatrix 缩放 RGB 通道

#### 建议：2.2 的 gamma 系数是近似值，标准摄影中通常使用 `2^(ev)` 作为线性曝光调整。当前公式 `2^(ev/2.2)` 更接近感知亮度调整，但命名可能引起混淆。

### BackdropEffectScope.gammaAdjustment(power)
- 类型：扩展函数
- 职责：Gamma 校正

#### 参数
- `power: Float` — Gamma 幂次

#### 实现逻辑：使用 AGSL RuntimeShader 实现（需要 API >= TIRAMISU）

### colorControlsColorFilter(brightness, contrast, saturation): ColorFilter
- 类型：私有函数
- 职责：构建亮度/对比度/饱和度的 ColorMatrix

#### 实现逻辑：使用 ITU-R BT.709 亮度系数（0.213, 0.715, 0.072）构建 ColorMatrix

#### 建议：系数选择正确，符合标准。整体 ColorFilter 模块设计良好，提供了丰富的图像调整能力。

---

## effects/Lens.kt

### BackdropEffectScope.lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)
- 类型：扩展函数
- 职责：添加透镜折射效果

#### 参数
- `refractionHeight: Float`（`@FloatRange(from = 0.0)`）— 折射影响高度
- `refractionAmount: Float`（`@FloatRange(from = 0.0)`）— 折射偏移量
- `depthEffect: Boolean = false` — 是否启用深度效果
- `chromaticAberration: Boolean = false` — 是否启用色差效果

#### 实现逻辑
1. API < TIRAMISU 直接返回（需要 RuntimeShader 支持）
2. 参数 <= 0 直接返回
3. 如果有 padding，减去 refractionHeight（最低为 0）
4. 根据形状提取圆角半径
5. 根据 chromaticAberration 选择着色器
6. 设置着色器 uniform 变量
7. 创建 RuntimeShaderEffect 并调用 `effect()`

#### 调用关系：由效果 DSL 块调用

### cornerRadii: FloatArray?
- 类型：`BackdropEffectScope` 私有扩展属性
- 职责：从当前形状提取四角圆角半径

#### 实现逻辑：支持三种形状类型：
- `RoundedRectangularShape` — 使用其 corners 属性
- `AbsoluteRoundedCornerShape` — 直接获取四角半径，限制最大值为 minDimension/2
- `CornerBasedShape` — 考虑 LTR/RTL 布局方向获取四角半径

#### 建议：对不支持的形状类型返回 null 并抛出异常，这是合理的防御性设计。

### throwUnsupportedSDFException(): Nothing
- 类型：私有函数
- 职责：抛出不支持形状的异常

---

## effects/RenderEffect.kt

### BackdropEffectScope.effect(effect: RenderEffect)
- 类型：扩展函数
- 职责：添加 Android 原生 RenderEffect 到效果链

#### 参数
- `effect: RenderEffect` — Android 原生渲染效果

#### 实现逻辑：如果已有 RenderEffect，创建链式效果；否则直接设置

### BackdropEffectScope.effect(effect: Compose RenderEffect)
- 类型：扩展函数
- 职责：添加 Compose RenderEffect（委托给 Android 原生版本）

#### 建议：这两个函数是效果链的基础构建块，其他效果函数（blur、colorFilter、lens）最终都通过此函数将效果追加到链中。

---

## backdrops/Backdrop.kt

### rememberBackdrop(backdrop, onDraw): Backdrop
- 类型：`@Composable` 函数
- 职责：创建带自定义绘制回调的 Backdrop 包装

#### 参数
- `backdrop: Backdrop` — 原始 Backdrop
- `onDraw: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit` — 自定义绘制回调

#### 实现逻辑：使用 `remember` 缓存包装后的 Backdrop 实例

### Backdrop（私有类）
- 类型：`private class`
- 父接口：`Backdrop`
- 注解：`@Immutable`
- 职责：装饰器，为现有 Backdrop 添加自定义绘制回调

#### backdrop: Backdrop
- 作用域：构造参数
- 使用场景：被装饰的原始 Backdrop

#### onDraw: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit
- 作用域：构造参数
- 使用场景：自定义绘制策略

#### drawBackdrop(density, coordinates, layerBlock)
- 实现逻辑：通过 `onDraw` 回调包装原始 Backdrop 的绘制

#### 建议：装饰器模式的应用，允许在不修改原始 Backdrop 的情况下自定义绘制行为。

---

## backdrops/CanvasBackdrop.kt

### rememberCanvasBackdrop(onDraw): Backdrop
- 类型：`@Composable` 函数
- 职责：创建基于自定义绘制逻辑的 Backdrop

#### 参数
- `onDraw: DrawScope.() -> Unit` — 自定义绘制逻辑

#### 实现逻辑：使用 `remember` 缓存 CanvasBackdrop 实例

### CanvasBackdrop
- 类型：`private class`
- 父接口：`Backdrop`
- 注解：`@Immutable`
- 职责：基于自定义 DrawScope 绘制的 Backdrop 实现

#### onDraw: DrawScope.() -> Unit
- 作用域：构造参数
- 使用场景：自定义绘制逻辑

#### isCoordinatesDependent: Boolean
- 值：`false`
- 使用场景：Canvas 绘制不依赖布局坐标

#### drawBackdrop(density, coordinates, layerBlock)
- 实现逻辑：直接执行 `onDraw()`

#### 建议：最简单的 Backdrop 实现，适用于纯自定义绘制场景（如渐变背景、图案背景等）。

---

## backdrops/CombinedBackdrop.kt

### rememberCombinedBackdrop(backdrop1, backdrop2): Backdrop
- 类型：`@Composable` 函数（2参数版本）
- 职责：组合两个 Backdrop

### rememberCombinedBackdrop(backdrop1, backdrop2, backdrop3): Backdrop
- 类型：`@Composable` 函数（3参数版本）
- 职责：组合三个 Backdrop

### rememberCombinedBackdrop(vararg backdrops): Backdrop
- 类型：`@Composable` 函数（可变参数版本）
- 职责：组合任意数量的 Backdrop

### Combined2Backdrops
- 类型：`private class`
- 父接口：`Backdrop`
- 职责：组合2个 Backdrop 的实现

#### isCoordinatesDependent: Boolean
- 实现逻辑：`backdrop1.isCoordinatesDependent || backdrop2.isCoordinatesDependent`

#### drawBackdrop(density, coordinates, layerBlock)
- 实现逻辑：依次调用两个 Backdrop 的 drawBackdrop

### Combined3Backdrops
- 类型：`private class`
- 父接口：`Backdrop`
- 职责：组合3个 Backdrop 的实现

### CombinedBackdrops
- 类型：`private class`
- 父接口：`Backdrop`
- 职责：组合任意数量 Backdrop 的实现

#### isCoordinatesDependent: Boolean
- 实现逻辑：`backdrops.any { it.isCoordinatesDependent }`

#### 建议：
- 提供了2、3、可变参数三个重载版本，覆盖了常见使用场景。
- `isCoordinatesDependent` 使用 `any` 语义正确——只要任一子 Backdrop 依赖坐标，组合就依赖坐标。
- 可变参数版本使用 `vararg`，注意其会创建数组，在热路径上可能有微小开销。

---

## backdrops/EmptyBackdrop.kt

### emptyBackdrop(): Backdrop
- 类型：`@Stable` 函数
- 职责：返回空 Backdrop 单例

### EmptyBackdrop
- 类型：`private object`
- 父接口：`Backdrop`
- 注解：`@Immutable`
- 职责：空背景实现，不绘制任何内容

#### isCoordinatesDependent: Boolean
- 值：`false`

#### drawBackdrop(density, coordinates, layerBlock)
- 实现逻辑：空实现

#### 建议：空对象模式的应用，避免使用 null 表示"无背景"。

---

## backdrops/LayerBackdrop.kt

### rememberLayerBackdrop(graphicsLayer, onDraw): LayerBackdrop
- 类型：`@Composable` 函数
- 职责：创建图层背景

#### 参数
- `graphicsLayer: GraphicsLayer = rememberGraphicsLayer()` — 图层
- `onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw` — 绘制回调（默认绘制内容）

### DefaultOnDraw
- 类型：私有顶层属性
- 初始值：`{ drawContent() }`
- 使用场景：默认的图层内容绘制策略

### LayerBackdrop
- 类型：`@Stable` 类
- 父接口：`Backdrop`
- 职责：基于 GraphicsLayer 的背景实现，用于将一个组件的内容作为另一个组件的背景（毛玻璃效果的核心）

#### graphicsLayer: GraphicsLayer
- 作用域：公开属性
- 使用场景：存储源组件内容的图层

#### onDraw: ContentDrawScope.() -> Unit
- 作用域：internal
- 使用场景：定义如何将内容录制到 graphicsLayer

#### isCoordinatesDependent: Boolean
- 值：`true`
- 使用场景：LayerBackdrop 需要坐标信息来计算偏移

#### layerCoordinates: LayoutCoordinates?
- 作用域：internal，使用 `mutableStateOf`
- 使用场景：源组件的布局坐标

#### inverseLayerScope: InverseLayerScope?
- 作用域：私有
- 使用场景：缓存逆变换作用域对象

#### drawBackdrop(density, coordinates, layerBlock)
- 实现逻辑：
  1. 获取当前坐标和源组件坐标
  2. 如果有 layerBlock，先应用逆变换
  3. 计算源组件相对于当前组件的偏移（优先使用 `localPositionOf`，异常时回退到窗口坐标差值）
  4. 平移画布使源组件内容对齐
  5. 绘制 graphicsLayer

#### obtainInverseLayerScope(): InverseLayerScope
- 实现逻辑：复用或创建 InverseLayerScope

#### 建议：
- `localPositionOf` 的异常捕获（`try-catch`）是必要的，因为外部变换（如缩放）可能导致坐标计算失败。TODO 注释表明这是一个已知限制。
- 回退方案 `positionInWindow() - positionInWindow()` 在有外部变换时结果不准确，但对于简单场景足够。
- 这是毛玻璃效果的关键组件——源组件通过 `layerBackdrop()` Modifier 录制内容到 graphicsLayer，目标组件通过 LayerBackdrop 读取并绘制。

---

## backdrops/LayerBackdropModifier.kt

### Modifier.layerBackdrop(backdrop): Modifier
- 类型：Modifier 扩展函数
- 职责：将组件内容录制到 LayerBackdrop 的 GraphicsLayer 中

#### 参数
- `backdrop: LayerBackdrop` — 目标图层背景

### LayerBackdropElement
- 类型：`private class`
- 父类：`ModifierNodeElement<LayerBackdropNode>`
- 职责：Modifier 节点元素

### LayerBackdropNode
- 类型：`private class`
- 父类：`DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node`
- 职责：录制组件内容到 LayerBackdrop

#### draw()
- 实现逻辑：先绘制内容，然后将内容录制到 backdrop 的 graphicsLayer

#### onGloballyPositioned(coordinates)
- 实现逻辑：更新 backdrop 的 layerCoordinates

#### onDetach()
- 实现逻辑：清空 backdrop 的 layerCoordinates

#### 建议：此 Modifier 应应用于源组件（即作为背景内容的组件），与目标组件上的 `drawBackdrop` Modifier 配合使用。

---

## shadow/InnerShadow.kt

### InnerShadow
- 类型：`@Immutable data class`
- 职责：定义内阴影的视觉参数

#### radius: Dp
- 初始值：`24f.dp`
- 使用场景：模糊半径

#### offset: DpOffset
- 初始值：`DpOffset(0f.dp, radius)`
- 使用场景：阴影偏移（默认向下偏移一个半径）

#### color: Color
- 初始值：`Color.Black.copy(alpha = 0.15f)`
- 使用场景：阴影颜色

#### alpha: Float
- 初始值：`1f`
- 使用场景：整体透明度（`@FloatRange(from = 0.0, to = 1.0)`）

#### blendMode: BlendMode
- 初始值：`DrawScope.DefaultBlendMode`
- 使用场景：混合模式

#### Default: InnerShadow
- 作用域：伴生对象
- 初始值：`InnerShadow()`
- 使用场景：默认内阴影配置

### lerp(start, stop, fraction): InnerShadow
- 类型：`@Stable` 顶层函数
- 职责：在两个 InnerShadow 之间线性插值

#### 参数
- `start: InnerShadow` — 起始值
- `stop: InnerShadow` — 终止值
- `fraction: Float` — 插值比例

#### 实现逻辑：对 radius、offset、color、alpha 分别插值，blendMode 使用阈值切换

#### 建议：blendMode 使用 `fraction < 0.5f` 切换是常见做法，但可能导致动画中间帧的突变。如果需要平滑过渡，可考虑始终使用相同的 blendMode。

---

## shadow/Shadow.kt

### Shadow
- 类型：`@Immutable data class`
- 职责：定义外阴影的视觉参数

#### radius: Dp
- 初始值：`24f.dp`
- 使用场景：模糊半径

#### offset: DpOffset
- 初始值：`DpOffset(0f.dp, radius / 6f)`
- 使用场景：阴影偏移（默认向下偏移 radius/6）

#### color: Color
- 初始值：`Color.Black.copy(alpha = 0.1f)`
- 使用场景：阴影颜色

#### alpha: Float
- 初始值：`1f`
- 使用场景：整体透明度

#### blendMode: BlendMode
- 初始值：`DrawScope.DefaultBlendMode`
- 使用场景：混合模式

#### Default: Shadow
- 作用域：伴生对象
- 初始值：`Shadow()`
- 使用场景：默认外阴影配置

#### 建议：与 InnerShadow 相比，默认偏移量更小（radius/6 vs radius），颜色更淡（0.1 vs 0.15），符合外阴影通常更轻的设计直觉。

---

## shadow/ShadowModifier.kt

### ShadowElement
- 类型：`internal class`
- 父类：`ModifierNodeElement<ShadowNode>`
- 职责：外阴影 Modifier 节点元素

### ShadowNode
- 类型：`internal class`
- 父类：`DrawModifierNode, Modifier.Node`
- 职责：外阴影绘制节点

#### shouldAutoInvalidate: Boolean
- 值：`false`
- 使用场景：禁用自动失效，手动控制重绘

#### shadowLayer: GraphicsLayer?
- 作用域：私有
- 使用场景：用于录制阴影内容

#### paint: Paint
- 作用域：私有
- 使用场景：阴影绘制画笔

#### draw()
- 实现逻辑：
  1. 如果 shadow 为 null，直接绘制内容
  2. 计算阴影尺寸（原始尺寸 + 4倍半径 + 偏移）
  3. 配置画笔（颜色 + BlurMaskFilter）
  4. 录制到 GraphicsLayer：先绘制填充轮廓，再用 Clear 模式绘制偏移轮廓（形成阴影遮罩）
  5. 平移绘制阴影图层
  6. 绘制内容

#### configurePaint(shadow)
- 实现逻辑：设置画笔颜色和 BlurMaskFilter

#### ShadowMaskPaint
- 类型：私有顶层属性
- 初始值：`Paint().apply { blendMode = BlendMode.Clear }`
- 使用场景：清除阴影内部区域

#### 建议：
- 阴影绘制使用了"填充 + 清除偏移轮廓"的技术，产生自然的投影效果。
- `BlurMaskFilter` 是 Android 传统模糊方案，在 API < S 的设备上也能工作。
- 阴影尺寸 `4 * radius` 是经验值，确保模糊边缘不被裁剪。

---

## shadow/InnerShadowModifier.kt

### InnerShadowElement
- 类型：`internal class`
- 父类：`ModifierNodeElement<InnerShadowNode>`
- 职责：内阴影 Modifier 节点元素

### InnerShadowNode
- 类型：`internal class`
- 父类：`DrawModifierNode, Modifier.Node`
- 职责：内阴影绘制节点

#### shouldAutoInvalidate: Boolean
- 值：`false`

#### shadowLayer: GraphicsLayer?
- 作用域：私有

#### paint: Paint
- 作用域：私有

#### clipPath: Path?
- 作用域：私有
- 使用场景：复用 Path 对象

#### prevRadius: Float
- 作用域：私有
- 初始值：`Float.NaN`
- 使用场景：缓存上次的模糊半径，避免重复创建 BlurEffect

#### draw()
- 实现逻辑：
  1. 先绘制内容
  2. API < S 直接返回
  3. 计算阴影参数
  4. 配置画笔
  5. 如果半径变化，更新 BlurEffect
  6. 录制到 GraphicsLayer：裁剪到轮廓 → 绘制填充 → 平移绘制清除遮罩
  7. 裁剪到轮廓后绘制阴影图层

#### configurePaint(shadow)
- 实现逻辑：仅设置颜色（模糊由 RenderEffect 处理）

#### drawMaskedShadow(outline, layer)
- 实现逻辑：裁剪到轮廓后绘制图层（当前未被使用，因为逻辑已内联到 draw()）

#### 建议：
- `drawMaskedShadow` 函数未被调用，是死代码，建议移除。
- 内阴影使用 `BlurEffect`（API >= S）而非 `BlurMaskFilter`，这是因为内阴影需要 `TileMode.Decal` 避免边缘重复。
- `prevRadius` 缓存避免了每帧创建新的 BlurEffect 对象，是好的优化。

---

## highlight/Highlight.kt

### Highlight
- 类型：`@Immutable data class`
- 职责：定义高光效果的视觉参数

#### width: Dp
- 初始值：`0.5f.dp`
- 使用场景：高光描边宽度

#### blurRadius: Dp
- 初始值：`width / 2f`
- 使用场景：高光模糊半径（默认为宽度的一半）

#### alpha: Float
- 初始值：`1f`
- 使用场景：整体透明度

#### style: HighlightStyle
- 初始值：`HighlightStyle.Default`
- 使用场景：高光样式

#### Default: Highlight
- 作用域：伴生对象
- 初始值：`Highlight()`

#### Ambient: Highlight
- 作用域：伴生对象
- 初始值：`Highlight(style = HighlightStyle.Ambient)`

#### Plain: Highlight
- 作用域：伴生对象
- 初始值：`Highlight(style = HighlightStyle.Plain)`

#### 建议：三种预设样式覆盖了常见的高光需求。`blurRadius` 默认为 `width / 2f` 提供了自然的模糊效果。

---

## highlight/HighlightModifier.kt

### HighlightElement
- 类型：`internal class`
- 父类：`ModifierNodeElement<HighlightNode>`
- 职责：高光 Modifier 节点元素

### HighlightNode
- 类型：`internal class`
- 父类：`DrawModifierNode, Modifier.Node`
- 职责：高光绘制节点

#### shouldAutoInvalidate: Boolean
- 值：`false`

#### highlightLayer: GraphicsLayer?
- 作用域：私有

#### paint: Paint
- 作用域：私有
- 初始值：`Paint().apply { style = PaintingStyle.Stroke }`
- 使用场景：高光描边画笔

#### clipPath: Path?
- 作用域：私有
- 使用场景：复用 Path 对象

#### runtimeShaderCache: RuntimeShaderCacheImpl
- 作用域：私有
- 使用场景：缓存 AGSL 着色器

#### prevStyle: HighlightStyle?
- 作用域：私有
- 初始值：`null`
- 使用场景：缓存上次的样式（当前未使用）

#### draw()
- 实现逻辑：
  1. 如果高光为 null 或宽度 <= 0，直接绘制内容
  2. 先绘制内容
  3. 计算安全尺寸（+2px 余量）
  4. 配置画笔（颜色、描边宽度、模糊、着色器）
  5. 录制到 GraphicsLayer：裁剪到轮廓 → 绘制描边轮廓
  6. 偏移 (-1, -1) 绘制高光图层

#### configurePaint(highlight)
- 实现逻辑：
  1. 设置颜色
  2. 计算描边宽度（ceil 后限制最大为 minDimension/2，再乘2）
  3. 设置 BlurMaskFilter
  4. API >= S 时创建着色器

#### 建议：
- `prevStyle` 属性未被使用，是死代码，建议移除或用于着色器缓存优化。
- 偏移 (-1, -1) 是为了补偿 safeSize 的 +2 余量，确保高光与内容对齐。
- 描边宽度 `ceil(width) * 2f` 的计算方式确保了描边在轮廓内外各占一半。

---

## highlight/HighlightStyle.kt

### HighlightStyle
- 类型：`@Immutable interface`
- 职责：定义高光样式的协议

#### color: Color
- 作用域：接口属性
- 使用场景：高光颜色

#### blendMode: BlendMode
- 作用域：接口属性
- 使用场景：混合模式

#### DrawScope.createShader(shape, runtimeShaderCache): Shader?
- 参数：
  - `shape: Shape` — 当前形状
  - `runtimeShaderCache: RuntimeShaderCache` — 着色器缓存
- 返回值：`Shader?` — AGSL 着色器（可为 null）
- API 要求：`@RequiresApi(Build.VERSION_CODES.S)`

### HighlightStyle.Plain
- 类型：`@Immutable data class`
- 父接口：`HighlightStyle`
- 职责：纯色高光样式（无着色器）

#### color: Color
- 初始值：`Color.White.copy(alpha = 0.38f)`

#### blendMode: BlendMode
- 初始值：`BlendMode.Plus`

#### createShader()
- 返回值：`null`
- 使用场景：使用纯色描边，无渐变效果

### HighlightStyle.Default
- 类型：`@Immutable data class`
- 父接口：`HighlightStyle`
- 职责：默认高光样式，使用 AGSL 着色器实现方向性高光

#### color: Color
- 初始值：`Color.White.copy(alpha = 0.5f)`

#### blendMode: BlendMode
- 初始值：`BlendMode.Plus`

#### angle: Float
- 初始值：`45f`
- 使用场景：高光方向角度（度数）

#### falloff: Float
- 初始值：`1f`
- 使用场景：高光衰减指数

#### createShader()
- 实现逻辑：API >= TIRAMISU 时创建 DefaultHighlightShader，设置 size、cornerRadii、color、angle、falloff uniform

#### Deprecated 构造函数(intensity, angle, falloff)
- 职责：旧版构造函数，使用 intensity 参数代替 color
- 替代方案：`HighlightStyle.Default(color = Color.White.copy(alpha = intensity), ...)`

### HighlightStyle.Ambient
- 类型：`@Immutable data class`
- 父接口：`HighlightStyle`
- 职责：环境光高光样式，产生明暗对比效果

#### intensity: Float
- 初始值：`0.38f`
- 使用场景：环境光强度

#### color: Color
- 值：`Color.White.copy(alpha = intensity)`

#### blendMode: BlendMode
- 值：`DrawScope.DefaultBlendMode`（非 Plus，因为需要区分明暗）

#### createShader()
- 实现逻辑：API >= TIRAMISU 时创建 AmbientHighlightShader

### 伴生对象预设
- `Default: Default` — 默认高光
- `Ambient: Ambient` — 环境光高光
- `Plain: Plain` — 纯色高光

### getCornerRadii(shape): FloatArray
- 类型：私有扩展函数
- 职责：从 Shape 提取四角圆角半径
- 实现逻辑：仅支持 `CornerBasedShape`，其他返回 `FloatArray(4) { maxRadius }`
- 建议：与 Lens.kt 中的 `cornerRadii` 属性功能类似但实现不同。Lens 支持更多形状类型，此处仅支持 CornerBasedShape。建议统一提取逻辑。

---

## 模块整体架构分析

### 分层结构
```
backdrop/
├── Backdrop.kt              # 核心接口
├── BackdropEffectScope.kt   # 效果作用域
├── DrawBackdropModifier.kt  # 主入口 Modifier
├── InverseLayerScope.kt     # 逆变换计算
├── LayerRecorder.kt         # 图层录制工具
├── Outline.kt               # 轮廓裁剪工具
├── RuntimeShaderCache.kt    # AGSL 着色器缓存
├── ShapeProvider.kt         # 形状缓存提供者
├── Shaders.kt               # AGSL 着色器代码
├── effects/                 # 效果扩展函数
│   ├── Blur.kt
│   ├── ColorFilter.kt
│   ├── Lens.kt
│   └── RenderEffect.kt
├── backdrops/               # Backdrop 实现
│   ├── Backdrop.kt          # 装饰器
│   ├── CanvasBackdrop.kt    # 自定义绘制
│   ├── CombinedBackdrop.kt  # 组合
│   ├── EmptyBackdrop.kt     # 空实现
│   ├── LayerBackdrop.kt     # 图层背景（毛玻璃核心）
│   └── LayerBackdropModifier.kt # 图层录制 Modifier
├── shadow/                  # 阴影系统
│   ├── Shadow.kt
│   ├── ShadowModifier.kt
│   ├── InnerShadow.kt
│   └── InnerShadowModifier.kt
└── highlight/               # 高光系统
    ├── Highlight.kt
    ├── HighlightStyle.kt
    └── HighlightModifier.kt
```

### 核心设计模式
1. **策略模式**：`Backdrop` 接口的不同实现提供不同的背景绘制策略
2. **装饰器模式**：`backdrops/Backdrop.kt` 中的包装类为 Backdrop 添加自定义绘制回调
3. **组合模式**：`CombinedBackdrop` 允许组合多个 Backdrop
4. **DSL 模式**：`BackdropEffectScope` 通过扩展函数构建效果链
5. **Modifier Node API**：使用 Compose 的新式 Node-based Modifier API 实现高性能绘制

### 毛玻璃效果工作流程
1. 源组件应用 `Modifier.layerBackdrop(layerBackdrop)` → 将内容录制到 GraphicsLayer
2. 目标组件应用 `Modifier.drawBackdrop(layerBackdrop, ...)` → 读取源组件的 GraphicsLayer 并绘制为背景
3. 效果链（blur、colorFilter 等）通过 `BackdropEffectScope` 构建，最终应用到 GraphicsLayer 的 RenderEffect
4. 高光和阴影通过独立的 Modifier 节点叠加

### API 级别兼容性
- **Android 12 (S, API 31)**：RenderEffect、BlurEffect 基础支持
- **Android 13 (TIRAMISU, API 33)**：RuntimeShader (AGSL) 支持，lens 和高级高光效果
- **Android 12 以下**：仅支持 BlurMaskFilter 方式的阴影，无模糊/高光效果

### 建议汇总
1. **死代码清理**：`InnerShadowNode.drawMaskedShadow()` 和 `HighlightNode.prevStyle` 未被使用
2. **圆角提取统一**：Lens.kt 和 HighlightStyle.kt 中的圆角提取逻辑应统一
3. **性能优化**：色散着色器的7次采样可考虑减少到3次（RGB）
4. **文档补充**：AGSL 着色器应添加数学原理注释
5. **dp 支持**：blur 的 radius 参数应提供 dp 重载
6. **逆变换完整性**：InverseLayerScope 仅处理 rotationZ + scale，未处理 translation 和 3D 旋转
