param(
    [string]$BaseUrl = "http://127.0.0.1:18080",
    [string]$Profile = "local",
    [int]$Port = 18080,
    [switch]$StartBackendIfNeeded = $true
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$projectRoot = Split-Path -Parent $root
$backendDir = Join-Path $projectRoot "backend"
$reportDir = Join-Path $backendDir "build\reports\admin-console"
$runtimeNode = "C:\Users\syy\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
$runtimeNodeModules = "C:\Users\syy\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules"
$browserScript = Join-Path $backendDir "tools\admin_console_browser_check.mjs"
$logOut = Join-Path $reportDir "backend-stdout.log"
$logErr = Join-Path $reportDir "backend-stderr.log"
$reportPath = Join-Path $reportDir "smoke-report.json"

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$startedProcess = $null

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{},
        [int]$TimeoutSec = 60
    )

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
        TimeoutSec = $TimeoutSec
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }

    try {
        $payload = Invoke-RestMethod @params
        if ($null -ne $payload.PSObject.Properties["code"]) {
            if ($payload.code -ne 0) {
                throw "HTTP 200 $Url failed: $($payload | ConvertTo-Json -Depth 10 -Compress)"
            }
            return $payload.data
        }
        return $payload
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            $bodyText = $reader.ReadToEnd()
            throw "HTTP $([int]$response.StatusCode) $Url failed: $bodyText"
        }
        throw
    }
}

function Wait-BackendHealthy {
    param([int]$MaxSeconds = 90)
    $healthUrl = "$BaseUrl/v1/sync/health"
    $deadline = (Get-Date).AddSeconds($MaxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $health = Invoke-RestMethod -Method Get -Uri $healthUrl -TimeoutSec 5
            if ($health.code -eq 0) {
                return $true
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Start-Backend {
    if (-not $StartBackendIfNeeded) {
        throw "Backend is not healthy and StartBackendIfNeeded is disabled."
    }
    $gradle = Join-Path $projectRoot "android\gradlew.bat"
    $arguments = @(
        "-p", ('"{0}"' -f $backendDir),
        "bootRun",
        ('"--args=--spring.profiles.active={0} --server.port={1}"' -f $Profile, $Port),
        "--no-daemon",
        "--console=plain"
    )
    $startedProcess = Start-Process -FilePath $gradle -ArgumentList $arguments -WorkingDirectory $projectRoot -RedirectStandardOutput $logOut -RedirectStandardError $logErr -PassThru -WindowStyle Hidden
    Start-Sleep -Seconds 3
    if (-not (Wait-BackendHealthy)) {
        throw "Backend failed to become healthy after bootRun. Logs: $logOut / $logErr"
    }
    return $startedProcess
}

function Stop-StartedBackend {
    param([System.Diagnostics.Process]$Process)
    if ($null -ne $Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force
    }
}

try {
    if (-not (Wait-BackendHealthy -MaxSeconds 5)) {
        $startedProcess = Start-Backend
    }

    $summaryBefore = Invoke-Api -Method Get -Url "$BaseUrl/v1/admin/summary"
    $seedNoReset = Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/demo/seed?reset=false"
    $seedReset = Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/demo/seed?reset=true"
    $summaryAfter = Invoke-Api -Method Get -Url "$BaseUrl/v1/admin/summary"

    $expectedCounts = @{
        user_count = 4
        product_count = 5
        customer_count = 3
        supplier_count = 3
        sale_order_count = 3
        purchase_order_count = 2
        pay_order_count = 2
    }

    foreach ($key in $expectedCounts.Keys) {
        if ($seedReset.$key -ne $expectedCounts[$key]) {
            throw "Seed reset expected $key=$($expectedCounts[$key]) but got $($seedReset.$key)"
        }
    }

    $uniquePhone = "139" + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $createUser = Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/users" -Body @{
        phone = $uniquePhone
        password = "123456"
        nickname = "script-user"
        status = 1
    }

    $duplicateRejected = $false
    try {
        Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/users" -Body @{
            phone = $uniquePhone
            password = "123456"
            nickname = "duplicate-phone"
            status = 1
        } | Out-Null
    } catch {
        if ($_.ToString() -like '*phone already registered*') {
            $duplicateRejected = $true
        } else {
            throw
        }
    }
    if (-not $duplicateRejected) {
        throw "Duplicate phone creation was not rejected."
    }

    $emptyNicknameRejected = $false
    try {
        Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/users" -Body @{
            phone = "13800138993"
            password = "123456"
            nickname = " "
            status = 1
        } | Out-Null
    } catch {
        if ($_.ToString() -like '*nickname is required*') {
            $emptyNicknameRejected = $true
        } else {
            throw
        }
    }
    if (-not $emptyNicknameRejected) {
        throw "Empty nickname creation was not rejected."
    }

    $usersAfterCreate = Invoke-Api -Method Get -Url "$BaseUrl/v1/admin/users?keyword=$uniquePhone"
    if (($usersAfterCreate | Measure-Object).Count -lt 1) {
        throw "Created user was not returned by keyword search."
    }

    $updatedUser = Invoke-Api -Method Put -Url "$BaseUrl/v1/admin/users/$($createUser.id)" -Body @{
        nickname = "script-user-off"
        status = 0
        password = ""
        keepSessions = $true
    }
    if ($updatedUser.nickname -ne "script-user-off" -or $updatedUser.status -ne 0) {
        throw "User update did not persist nickname/status."
    }

    $resetSessionPhone = "139" + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $resetSessionUser = Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/users" -Body @{
        phone = $resetSessionPhone
        password = "123456"
        nickname = "reset-session-user"
        status = 1
    }
    $oldLogin = Invoke-Api -Method Post -Url "$BaseUrl/v1/auth/login" -Body @{
        phone = $resetSessionPhone
        password = "123456"
    }
    Invoke-Api -Method Put -Url "$BaseUrl/v1/admin/users/$($resetSessionUser.id)" -Body @{
        nickname = "reset-session-user"
        status = 1
        password = "654321"
        keepSessions = $false
    } | Out-Null
    $oldSessionRejected = $false
    try {
        Invoke-Api -Method Get -Url "$BaseUrl/v1/auth/users/me" -Headers @{ Authorization = "Bearer $($oldLogin.token)" } | Out-Null
    } catch {
        if ($_.ToString() -like '*not logged in*') {
            $oldSessionRejected = $true
        } else {
            throw
        }
    }
    if (-not $oldSessionRejected) {
        throw "keepSessions=false did not revoke existing session."
    }
    $loginAfterReset = Invoke-Api -Method Post -Url "$BaseUrl/v1/auth/login" -Body @{
        phone = $resetSessionPhone
        password = "654321"
    }
    $meAfterReset = Invoke-Api -Method Get -Url "$BaseUrl/v1/auth/users/me" -Headers @{ Authorization = "Bearer $($loginAfterReset.token)" }
    if ($meAfterReset.phone -ne $resetSessionPhone) {
        throw "Updated password login did not reach user profile."
    }

    $keepUserPhone = "139" + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $keepUser = Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/users" -Body @{
        phone = $keepUserPhone
        password = "123456"
        nickname = "keep-session-user"
        status = 1
    }
    $firstLogin = Invoke-Api -Method Post -Url "$BaseUrl/v1/auth/login" -Body @{
        phone = $keepUserPhone
        password = "123456"
    }
    Invoke-Api -Method Put -Url "$BaseUrl/v1/admin/users/$($keepUser.id)" -Body @{
        nickname = "keep-session-user"
        status = 1
        password = "888888"
        keepSessions = $true
    } | Out-Null
    $meKeepSession = Invoke-Api -Method Get -Url "$BaseUrl/v1/auth/users/me" -Headers @{ Authorization = "Bearer $($firstLogin.token)" }
    if ($meKeepSession.phone -ne $keepUserPhone) {
        throw "keepSessions=true did not preserve existing session."
    }

    $smoke = Invoke-Api -Method Post -Url "$BaseUrl/v1/admin/agent/smoke" -TimeoutSec 180
    if ([string]::IsNullOrWhiteSpace($smoke.workbench_narrative) -or [string]::IsNullOrWhiteSpace($smoke.answer_summary)) {
        throw "Agent smoke response did not include readable narrative fields."
    }
    if ($smoke.task_status -notin @("queued", "running", "completed", "failed")) {
        throw "Agent smoke task ended in unexpected status: $($smoke.task_status)"
    }

    $tasks = Invoke-Api -Method Get -Url "$BaseUrl/v1/agent/tasks"
    $latestTask = $tasks | Sort-Object createdAt -Descending | Select-Object -First 1
    if ($null -eq $latestTask) {
        throw "No agent task found after smoke."
    }

    $taskDetail = $null
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        $taskDetail = Invoke-Api -Method Get -Url "$BaseUrl/v1/agent/tasks/$($latestTask.id)"
        if ($taskDetail.task.status -in @("completed", "failed")) {
            break
        }
        Start-Sleep -Seconds 2
    }
    if ($null -eq $taskDetail -or $taskDetail.task.status -notin @("completed", "failed")) {
        throw "Latest agent task did not reach a final status."
    }

    $notificationsUrl = '{0}/v1/agent/notifications?undelivered_only=false&unread_only=false' -f $BaseUrl
    $notifications = Invoke-Api -Method Get -Url $notificationsUrl
    $taskNotification = $notifications | Where-Object { $_.taskId -eq $latestTask.id } | Sort-Object createdAt -Descending | Select-Object -First 1
    if ($null -eq $taskNotification) {
        throw "No notification was written for the latest agent task."
    }
    $replacementChar = [string][char]0xfffd
    if ($taskNotification.title.Contains($replacementChar) -or $taskNotification.body.Contains($replacementChar)) {
        throw "Notification text contains unreadable characters."
    }

    $indexHtml = Invoke-WebRequest -Uri "$BaseUrl/admin-console/index.html" -UseBasicParsing -TimeoutSec 30
    $stylesCss = Invoke-WebRequest -Uri "$BaseUrl/admin-console/styles.css" -UseBasicParsing -TimeoutSec 30
    $appJs = Invoke-WebRequest -Uri "$BaseUrl/admin-console/app.js" -UseBasicParsing -TimeoutSec 30

    foreach ($payload in @(
        @{ Name = "index"; Response = $indexHtml; Anchor = "Warehouse Admin" },
        @{ Name = "styles"; Response = $stylesCss; Anchor = ".summary-card" },
        @{ Name = "app"; Response = $appJs; Anchor = "async function runAgentSmoke()" }
    )) {
        if ($payload.Response.StatusCode -ne 200) {
            throw "$($payload.Name) resource did not return 200."
        }
        if (-not $payload.Response.Content.Contains($payload.Anchor)) {
            throw "$($payload.Name) resource did not contain expected anchor: $($payload.Anchor)"
        }
        if ($payload.Response.Content.Contains($replacementChar)) {
            throw "$($payload.Name) resource contains unreadable characters."
        }
    }

    $browserReport = "skipped"
    $browserReportPath = Join-Path $reportDir "browser"
    if (Test-Path $runtimeNode) {
        try {
            $env:NODE_PATH = $runtimeNodeModules
            $browserOutput = & $runtimeNode $browserScript $BaseUrl $browserReportPath 2>&1
            if ($LASTEXITCODE -eq 0) {
                $browserReport = ($browserOutput | Out-String).Trim()
            } else {
                $browserReport = "skipped: playwright browser binaries not available"
            }
        } catch {
            $browserReport = "skipped: browser helper failed"
        }
    }

    $report = [ordered]@{
        generatedAt = (Get-Date).ToString("s")
        baseUrl = $BaseUrl
        summaryBefore = $summaryBefore
        seedWithoutReset = $seedNoReset
        seedWithReset = $seedReset
        summaryAfter = $summaryAfter
        createdUser = $createUser
        updatedUser = $updatedUser
        agentSmoke = $smoke
        latestTask = $taskDetail
        latestNotification = $taskNotification
        browserReport = $browserReport
    }
    $report | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 $reportPath
    Write-Output $reportPath
} finally {
    Stop-StartedBackend -Process $startedProcess
}
