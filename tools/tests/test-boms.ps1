# 完整功能测试脚本
$base = 'http://localhost:8080/api'
$headers = @{'Content-Type'='application/json'}

function Test-Step($name, $result) {
    if ($result) {
        Write-Host "[PASS] $name" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $name" -ForegroundColor Red
    }
    return $result
}

Write-Host "`n=== BOMS 商机管理系统 功能测试 ===" -ForegroundColor Cyan

# 1. 创建商机
Write-Host "`n--- 1. 商机提报 ---"
$opp = @{
    company = "测试客户公司"
    name = "2026年服务器采购项目"
    supplierCompany = "某供应商"
    industry = "政府"
    deviceTypes = "服务器"
    estimatedPurchaseAmount = 500
    estimatedPurchaseAmountUnit = "万元"
    requiresExclusiveAuthorization = $true
    submitter = "testuser"
    submitterName = "张三"
    submitterUserId = "wechat_zhangsan"
} | ConvertTo-Json
$created = Invoke-RestMethod -Uri "$base/opportunities" -Method POST -Headers $headers -Body $opp
$oppId = $created.id
Test-Step "创建商机 (ID: $oppId)" ($oppId -gt 0)

# 2. 查询商机列表
Write-Host "`n--- 2. 查询商机 ---"
$list = Invoke-RestMethod -Uri "$base/opportunities" -Method GET
Test-Step "查询商机列表 (共 $($list.Count) 条)" ($list.Count -ge 1)

$detail = Invoke-RestMethod -Uri "$base/opportunities/$oppId" -Method GET
Test-Step "查询商机详情 (公司: $($detail.company))" ($detail.company -eq "测试客户公司")

# 3. 修改商机
Write-Host "`n--- 3. 编辑商机 ---"
$update = @{ estimatedPurchaseAmount = 800 } | ConvertTo-Json
$updated = Invoke-RestMethod -Uri "$base/opportunities/$oppId" -Method PATCH -Headers $headers -Body $update
Test-Step "修改采购金额 → $($updated.estimatedPurchaseAmount)万" ($updated.estimatedPurchaseAmount -eq 800)

# 4. OA 商机报备
Write-Host "`n--- 4. OA 商机报备流程 ---"
$startReport = @{ userId = "wechat_zhangsan"; userName = "张三" } | ConvertTo-Json
$reported = Invoke-RestMethod -Uri "$base/oa/opportunities/$oppId/report-flow/start" -Method POST -Headers $headers -Body $startReport
Test-Step "发起商机报备 (流程号: $($reported.reportFlowNo))" ($reported.reportFlowStatus -eq "IN_PROGRESS")

$archive = @{ opportunityId = $oppId; status = "ARCHIVED" } | ConvertTo-Json
$archived = Invoke-RestMethod -Uri "$base/oa/callback/report-flow/archived" -Method POST -Headers $headers -Body $archive
Test-Step "商机报备归档" ($archived.reportFlowStatus -eq "ARCHIVED")

# 5. 项目授权申请预填
Write-Host "`n--- 5. 项目授权预填接口 ---"
$reportFlowNo = $archived.reportFlowNo
$prefill = Invoke-RestMethod -Uri "$base/oa/report-flows/$reportFlowNo/bid-document-prefill" -Method GET
Test-Step "获取预填数据 (版本号: $($prefill.deviceRequirementVersion))" ($prefill.opportunityId -eq $oppId)

# 6. 发起项目授权申请
Write-Host "`n--- 6. 发起项目授权申请 ---"
$startAuth = @{
    reportFlowNo = $reportFlowNo
    bidDocumentFlowNo = "AUTH-TEST-001"
    deviceRequirementVersion = $prefill.deviceRequirementVersion
} | ConvertTo-Json
$authStarted = Invoke-RestMethod -Uri "$base/oa/callback/bid-document-flow/started" -Method POST -Headers $headers -Body $startAuth
Test-Step "发起项目授权申请 (锁定状态: $($authStarted.deviceRequirementLockStatus))" ($authStarted.deviceRequirementLockStatus -eq "SOFT_LOCKED")

# 7. 到06节点 → 硬锁定
Write-Host "`n--- 7. 06节点审批 → 硬锁定 ---"
$node06 = @{ bidDocumentFlowNo = "AUTH-TEST-001"; nodeCode = "06" } | ConvertTo-Json
$hardLocked = Invoke-RestMethod -Uri "$base/oa/callback/bid-document-flow/node-reached" -Method POST -Headers $headers -Body $node06
Test-Step "到达06节点 硬锁定 (状态: $($hardLocked.deviceRequirementLockStatus))" ($hardLocked.deviceRequirementLockStatus -eq "HARD_LOCKED")

# 8. 修订记录
Write-Host "`n--- 8. 修订记录 ---"
$history = Invoke-RestMethod -Uri "$base/opportunities/$oppId/history" -Method GET
Test-Step "查询修订记录 (共 $($history.Count) 条)" ($history.Count -ge 1)

# 9. 活动日志
Write-Host "`n--- 9. 活动日志 ---"
$final = Invoke-RestMethod -Uri "$base/opportunities/$oppId" -Method GET
$activities = $final.activities
Test-Step "活动日志包含「项目授权」 关键词" ($activities | Where-Object { $_.content -like "*项目授权*" })

Write-Host "`n=== 测试完成 ===" -ForegroundColor Cyan
