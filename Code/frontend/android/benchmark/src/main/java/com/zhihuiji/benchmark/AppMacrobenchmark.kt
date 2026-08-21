package com.zhihuiji.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupToHomeInteractive() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkFlows.PackageName,
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            iterations = 5,
        ) {
            with(BenchmarkFlows) {
                startHome()
            }
        }
    }

    @Test
    fun navigateFromHomeToAgentWorkbench() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkFlows.PackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                with(BenchmarkFlows) {
                    startHome()
                }
            },
        ) {
            with(BenchmarkFlows) {
                openAssistantTab()
            }
        }
    }

    @Test
    fun renderRuleSummaryAgentChatFlow() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkFlows.PackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
        ) {
            with(BenchmarkFlows) {
                launchAgentChatWithQuestion()
            }
        }
    }
}
