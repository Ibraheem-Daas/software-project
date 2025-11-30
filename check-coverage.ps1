# PowerShell script to run Maven tests with coverage
$mvnPath = "C:\Users\ibrah\.maven\maven-3.9.11\bin\mvn.cmd"
& $mvnPath jacoco:report

$xml = [xml](Get-Content 'target/site/jacoco/jacoco.xml')
$counters = $xml.report.counter | Where-Object { $_.type -eq 'INSTRUCTION' }
$covered = [int]$counters.covered
$missed = [int]$counters.missed
$total = $covered + $missed
$pct = [math]::Round(($covered / $total) * 100, 2)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "       CURRENT COVERAGE RESULTS" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
Write-Host "Total Instructions: $total"
Write-Host "Covered: $covered" -ForegroundColor Green
Write-Host "Missed: $missed" -ForegroundColor Yellow
Write-Host "Coverage: $pct%" -ForegroundColor $(if($pct -ge 90){'Green'}else{'Yellow'})
$needed = [math]::Ceiling($total * 0.9) - $covered
if($needed -gt 0){ 
    Write-Host "`nStill need $needed more instructions" -ForegroundColor Yellow
    $improvement = 548 - $missed
    Write-Host "Improvement from 548: $improvement instructions" -ForegroundColor Cyan
} else { 
    Write-Host "`n🎉 TARGET ACHIEVED! 🎉" -ForegroundColor Green
}
Write-Host "========================================`n" -ForegroundColor Cyan
